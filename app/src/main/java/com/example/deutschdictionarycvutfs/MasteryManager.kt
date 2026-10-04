package com.example.deutschdictionarycvutfs

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

enum class WordStatus {
    LOCKED, NEW, IN_PROGRESS, MASTERED
}

@Serializable
data class WordProgress(
    val mastery: Int = 0,
    val lastTestedAtEpochMilli: Long? = null,
    val status: WordStatus? = null // Nullable pro zpětnou kompatibilitu
)

@Serializable
data class MasteryData(
    val wordsMastery: Map<String, WordProgress> = emptyMap(),
    val lastUnlockEpochDay: Long = 0,
    val unlockedTodayCount: Int = 0
)

class MasteryManager(context: Context) {
    private val file = File(context.filesDir, "mastery_data.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    var masteryData: MasteryData = MasteryData()
        private set

    init {
        loadMastery()
        performMigrationIfNeeded()
    }

    private fun loadMastery() {
        if (file.exists()) {
            try {
                val content = file.readText()
                masteryData = json.decodeFromString(content)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun performMigrationIfNeeded() {
        var needsSave = false
        val updatedMap = masteryData.wordsMastery.toMutableMap()
        
        val toMigrate = updatedMap.filterValues { it.status == null }
        if (toMigrate.isNotEmpty()) {
            needsSave = true
            
            val inProgressCandidates = mutableListOf<Pair<String, WordProgress>>()
            
            for ((id, progress) in toMigrate) {
                if (progress.mastery >= 100) {
                    updatedMap[id] = progress.copy(status = WordStatus.MASTERED)
                } else if (progress.mastery == 0 && progress.lastTestedAtEpochMilli == null) {
                    updatedMap[id] = progress.copy(status = WordStatus.LOCKED)
                } else {
                    inProgressCandidates.add(id to progress)
                }
            }
            
            // Seřadíme rozpracovaná slova sestupně podle Mastery
            inProgressCandidates.sortByDescending { it.second.mastery }
            
            // Top 30 dostane IN_PROGRESS, zbytek LOCKED (čímž se "zmrazí")
            inProgressCandidates.forEachIndexed { index, pair ->
                val newStatus = if (index < 30) WordStatus.IN_PROGRESS else WordStatus.LOCKED
                updatedMap[pair.first] = pair.second.copy(status = newStatus)
            }
        }
        
        if (needsSave) {
            masteryData = masteryData.copy(wordsMastery = updatedMap)
            saveMastery()
        }
    }

    private fun saveMastery() {
        try {
            val content = json.encodeToString(masteryData)
            file.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Zachováno pro zpětnou kompatibilitu, pokud by slovíčko nemělo v JSONu ID
    fun getLegacyWordKey(lessonId: String, wordDe: String): String {
        return "${lessonId}_${wordDe}"
    }

    fun getWordProgress(wordId: String, legacyKey: String? = null): WordProgress {
        var progress = masteryData.wordsMastery[wordId]
        if (progress == null && legacyKey != null) {
            progress = masteryData.wordsMastery[legacyKey]
        }
        
        if (progress == null) {
            return WordProgress(0, null, WordStatus.LOCKED)
        }
        
        // Záchytná síť, pokud by se nějaké slovo propadlo (hlavní migrace je v performMigrationIfNeeded)
        if (progress.status == null) {
            return progress.copy(status = WordStatus.LOCKED)
        }
        
        return progress
    }

    fun getWordMastery(wordId: String, legacyKey: String? = null): Int {
        return getWordProgress(wordId, legacyKey).mastery
    }

    fun setWordProgress(wordId: String, progress: WordProgress) {
        val currentMap = masteryData.wordsMastery.toMutableMap()
        currentMap[wordId] = progress.copy(mastery = progress.mastery.coerceIn(0, 100))
        masteryData = masteryData.copy(wordsMastery = currentMap)
        saveMastery()
    }

    fun updateUnlockData(epochDay: Long, count: Int) {
        masteryData = masteryData.copy(lastUnlockEpochDay = epochDay, unlockedTodayCount = count)
        saveMastery()
    }

    fun resetLessonMastery(lesson: Lesson) {
        val currentMap = masteryData.wordsMastery.toMutableMap()
        lesson.categories.flatMap { it.words }.forEach { word ->
            val legacyKey = getLegacyWordKey(lesson.lessonId, word.de)
            val id = word.id ?: legacyKey
            
            if (currentMap.containsKey(legacyKey) && legacyKey != id) {
                currentMap.remove(legacyKey)
            }
            currentMap[id] = WordProgress(0, null, WordStatus.LOCKED)
        }
        masteryData = masteryData.copy(wordsMastery = currentMap)
        saveMastery()
    }

    fun getLessonMastery(lesson: Lesson): Float {
        if (lesson.categories.isEmpty()) return 0f
        val allWords = lesson.categories.flatMap { it.words }
        if (allWords.isEmpty()) return 0f

        val totalMastery = allWords.sumOf { word -> 
            val legacyKey = getLegacyWordKey(lesson.lessonId, word.de)
            val id = word.id ?: legacyKey
            getWordMastery(id, legacyKey)
        }
        return totalMastery.toFloat() / allWords.size
    }
}