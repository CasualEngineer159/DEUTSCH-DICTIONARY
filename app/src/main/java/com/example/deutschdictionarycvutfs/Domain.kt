package com.example.deutschdictionarycvutfs

import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

/** 
 * DOMAIN MODELS 
 */
data class VocabItem(
    val id: String,
    val lessonId: String,
    val wordDe: String,
    val wordCs: String,
    val synonymsDe: List<String> = emptyList(),
    val synonymsCs: List<String> = emptyList(),
    val mastery: Int = 0, // Valid range: 0..100
    val lastTestedAtEpochMilli: Long? = null
)

enum class DomainQuestionType { MULTIPLE_CHOICE, WRITTEN }
enum class TranslationDirection { DE_TO_CZ, CZ_TO_DE }

data class QuestionFormat(
    val type: DomainQuestionType,
    val direction: TranslationDirection
)

data class PracticeSettings(
    val allowedQuestionTypes: Set<DomainQuestionType> = DomainQuestionType.entries.toSet(),
    val allowedDirections: Set<TranslationDirection> = TranslationDirection.entries.toSet()
) {
    fun isFormatAllowed(format: QuestionFormat): Boolean {
        return format.type in allowedQuestionTypes && format.direction in allowedDirections
    }
}

object AnswerValidator {
    private val germanArticles = listOf("der ", "die ", "das ", "ein ", "eine ")

    fun isCorrect(userAnswer: String, item: VocabItem, direction: TranslationDirection): Boolean {
        val input = userAnswer.trim()
        
        val validAnswers = if (direction == TranslationDirection.CZ_TO_DE) {
            listOf(item.wordDe) + item.synonymsDe
        } else {
            listOf(item.wordCs) + item.synonymsCs
        }

        // 1. Přesná shoda (ignoruje velikost písmen)
        if (validAnswers.any { it.equals(input, ignoreCase = true) }) {
            return true
        }

        // 2. Kontrola bez členů (pro němčinu)
        if (direction == TranslationDirection.CZ_TO_DE) {
            val inputWithoutArticle = removeArticle(input)
            
            return validAnswers.any { validAnswer ->
                val validWithoutArticle = removeArticle(validAnswer)
                inputWithoutArticle.equals(validWithoutArticle, ignoreCase = true)
            }
        }

        return false
    }

    private fun removeArticle(text: String): String {
        var result = text.trim()
        for (article in germanArticles) {
            if (result.startsWith(article, ignoreCase = true)) {
                result = result.substring(article.length).trim()
                break // Odstraníme pouze první nalezený člen
            }
        }
        return result
    }
}

/** 
 * DOMAIN RULE 1: MASTERY UPDATE LOGIC
 */
object MasteryUpdater {
    
    fun applyAnswer(
        item: VocabItem,
        format: QuestionFormat,
        isCorrect: Boolean,
        answeredAtMilli: Long
    ): VocabItem {
        var newMastery = item.mastery
        
        if (isCorrect) {
            val gained = when (format.type) {
                DomainQuestionType.MULTIPLE_CHOICE -> if (format.direction == TranslationDirection.DE_TO_CZ) 10 else 12
                DomainQuestionType.WRITTEN -> if (format.direction == TranslationDirection.DE_TO_CZ) 20 else 25
            }
            
            newMastery = if (format.type == DomainQuestionType.MULTIPLE_CHOICE) {
                if (item.mastery >= 40) {
                    item.mastery
                } else {
                    (item.mastery + gained).coerceAtMost(40)
                }
            } else {
                item.mastery + gained
            }
        } else {
            val penalty = when (format.type) {
                DomainQuestionType.MULTIPLE_CHOICE -> 20
                DomainQuestionType.WRITTEN -> 5
            }
            newMastery -= penalty
        }
        
        return item.copy(
            mastery = newMastery.coerceIn(0, 100),
            lastTestedAtEpochMilli = answeredAtMilli
        )
    }
}

/** 
 * DOMAIN RULE 2: NEXT QUESTION SELECTION LOGIC
 */
class PracticeSessionEngine(
    private val settings: PracticeSettings,
    private val random: Random = Random.Default
) {
    
    private val formatDifficultyOrder = listOf(
        QuestionFormat(DomainQuestionType.MULTIPLE_CHOICE, TranslationDirection.DE_TO_CZ),
        QuestionFormat(DomainQuestionType.MULTIPLE_CHOICE, TranslationDirection.CZ_TO_DE),
        QuestionFormat(DomainQuestionType.WRITTEN, TranslationDirection.DE_TO_CZ),
        QuestionFormat(DomainQuestionType.WRITTEN, TranslationDirection.CZ_TO_DE)
    )

    data class NextQuestion(
        val item: VocabItem,
        val format: QuestionFormat
    )

    fun getNextQuestion(words: List<VocabItem>, currentTimeMilli: Long): NextQuestion? {
        val selectedWord = selectNextWord(words, currentTimeMilli) ?: return null
        val format = selectFormat(selectedWord)
        return NextQuestion(selectedWord, format)
    }

    private fun selectNextWord(words: List<VocabItem>, currentTimeMilli: Long): VocabItem? {
        if (words.isEmpty()) return null
        
        val weights = words.map { word -> word to calculateWeight(word, currentTimeMilli) }
        val totalWeight = weights.sumOf { it.second }
        
        var randomValue = random.nextDouble(totalWeight)
        for ((word, weight) in weights) {
            randomValue -= weight
            if (randomValue <= 0.0) {
                return word
            }
        }
        
        return words.last() 
    }

    private fun calculateWeight(word: VocabItem, currentTimeMilli: Long): Double {
        // Zvýšen minimální základ (offset) z 10.0 na 150.0. 
        // Zabraňuje tomu, aby slova se 100% mastery měla váhu blízkou nule.
        val baseWeight = (100.0 - word.mastery).pow(2) + 150.0
        
        val multiplier = if (word.lastTestedAtEpochMilli == null) {
            3.0
        } else {
            val millisPassed = currentTimeMilli - word.lastTestedAtEpochMilli
            val daysPassed = millisPassed / (1000.0 * 60 * 60 * 24)
            // Zvýšen vliv uplynulého času z 0.1 na 0.2 (každý den bez procvičení přidává 20 % k váze navíc)
            1.0 + (daysPassed * 0.2)
        }
        
        return baseWeight * multiplier
    }

    private fun selectFormat(word: VocabItem): QuestionFormat {
        val allowedFormats = formatDifficultyOrder.filter { settings.isFormatAllowed(it) }
        if (allowedFormats.isEmpty()) {
            throw IllegalStateException("User settings have disabled all question formats.")
        }

        // PEVNÁ PODMÍNKA: Pokud je mastery > 70, vynutíme nejtěžší formát (pokud ho uživatel nevypnul v nastavení)
        val ultimateFormat = formatDifficultyOrder[3] // Written (CZ -> DE)
        if (word.mastery > 70 && allowedFormats.contains(ultimateFormat)) {
            return ultimateFormat
        }
        
        // Definice "ideálního" Mastery (peak) pro každý formát
        val peaks = mapOf(
            formatDifficultyOrder[0] to 0,   // Multiple Choice (DE -> CZ)
            formatDifficultyOrder[1] to 30,  // Multiple Choice (CZ -> DE)
            formatDifficultyOrder[2] to 60,  // Written (DE -> CZ)
            formatDifficultyOrder[3] to 100  // Written (CZ -> DE)
        )

        // Každý povolený formát má vždy garantovanou minimální šanci 10 %, aby trénink nebyl nudný
        val baseWeight = 10.0
        val dynamicPool = 100.0 - (baseWeight * allowedFormats.size)

        // Vypočteme skóre pro dynamickou část - čím blíž je aktuální mastery k peaku, tím vyšší skóre
        val vScores = allowedFormats.associateWith { format ->
            val peak = peaks[format] ?: 50
            val distance = abs(word.mastery - peak)
            // Hodnota klesá od 100 do 0 (nula je při vzdálenosti 50 % od peaku)
            maxOf(0, 100 - 2 * distance).toDouble()
        }

        val sumV = vScores.values.sum()
        
        // Finální váhy v % (součet je vždy 100)
        val weights = allowedFormats.associateWith { format ->
            if (sumV > 0) {
                baseWeight + (vScores[format]!! / sumV) * dynamicPool
            } else {
                100.0 / allowedFormats.size // Záloha, pokud by všechna vScores byla 0
            }
        }

        // "Ruleta" (Roulette wheel selection) na základě vypočtených pravděpodobností
        var randomValue = random.nextDouble(100.0)
        for ((format, weight) in weights) {
            randomValue -= weight
            if (randomValue <= 0.0) {
                return format
            }
        }
        
        return allowedFormats.last()
    }
}
