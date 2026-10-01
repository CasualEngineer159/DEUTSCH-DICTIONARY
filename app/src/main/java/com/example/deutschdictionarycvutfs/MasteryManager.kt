package com.example.deutschdictionarycvutfs

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class WordProgress(
    val mastery: Int = 0,
    val lastTestedAtEpochMilli: Long? = null
)

@Serializable
data class MasteryData(
    val wordsMastery: Map<String, WordProgress> = emptyMap()
)

class MasteryManager(context: Context) {
    private val file = File(context.filesDir, "mastery_data.json")
    private val json = Json { ignoreUnknownKeys = true }

    var masteryData: MasteryData = MasteryData()
        private set

    init {
        loadMastery()
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
        // Pokud existuje progres pod novým ID, vrátí jej
        if (masteryData.wordsMastery.containsKey(wordId)) {
            return masteryData.wordsMastery[wordId]!!
        }
        // Pokud je zadán starý klíč a progres pod ním existuje, provedeme "tichou migraci" tím, že ho vrátíme.
        // Při dalším uložení (po odpovědi) se už uloží pod novým wordId.
        if (legacyKey != null && masteryData.wordsMastery.containsKey(legacyKey)) {
            return masteryData.wordsMastery[legacyKey]!!
        }
        return WordProgress()
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