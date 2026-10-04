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
    val lastTestedAtEpochMilli: Long? = null,
    val status: WordStatus = WordStatus.LOCKED
)

enum class DomainQuestionType { MULTIPLE_CHOICE, TIME_ATTACK, SCRAMBLED, SKELETON, THREE_STATE }
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
        // Zjednodušení: Tiers teď ignorují směry, protože ty jsou fixní
        return true 
    }
}

sealed class AnswerResult {
    data class Simple(val isCorrect: Boolean, val timeTakenMilli: Long? = null) : AnswerResult()
    data class ThreeState(val hasArticle: Boolean, val isArticleCorrect: Boolean, val isRootCorrect: Boolean) : AnswerResult()
}

object AnswerValidator {
    private val germanArticles = listOf("der ", "die ", "das ", "ein ", "eine ")
    val articleRegex = Regex("^([dD]er|[dD]ie|[dD]as)\\s+(.*)")

    fun validateSimple(userAnswer: String, item: VocabItem, direction: TranslationDirection): Boolean {
        val input = userAnswer.trim()
        val validAnswers = if (direction == TranslationDirection.CZ_TO_DE) {
            listOf(item.wordDe) + item.synonymsDe
        } else {
            listOf(item.wordCs) + item.synonymsCs
        }

        if (validAnswers.any { it.equals(input, ignoreCase = true) }) {
            return true
        }

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
                break
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
        result: AnswerResult,
        answeredAtMilli: Long
    ): VocabItem {
        var newMastery = item.mastery
        var newStatus = if (item.status == WordStatus.NEW) WordStatus.IN_PROGRESS else item.status
        
        var gained = 0
        var penalty = 0
        var isOverallCorrect = false

        when (format.type) {
            DomainQuestionType.MULTIPLE_CHOICE -> {
                if (result is AnswerResult.Simple) {
                    isOverallCorrect = result.isCorrect
                    if (result.isCorrect) gained = 10 else penalty = 15
                }
            }
            DomainQuestionType.TIME_ATTACK -> {
                if (result is AnswerResult.Simple) {
                    isOverallCorrect = result.isCorrect
                    if (result.isCorrect) {
                        gained = if (result.timeTakenMilli != null && result.timeTakenMilli <= 5000) 15 else 5
                    } else penalty = 15
                }
            }
            DomainQuestionType.SCRAMBLED -> {
                if (result is AnswerResult.Simple) {
                    isOverallCorrect = result.isCorrect
                    if (result.isCorrect) gained = 18 else penalty = 10
                }
            }
            DomainQuestionType.SKELETON -> {
                if (result is AnswerResult.Simple) {
                    isOverallCorrect = result.isCorrect
                    if (result.isCorrect) gained = 20 else penalty = 8
                }
            }
            DomainQuestionType.THREE_STATE -> {
                if (result is AnswerResult.ThreeState) {
                    isOverallCorrect = result.isRootCorrect && (!result.hasArticle || result.isArticleCorrect)
                    if (result.hasArticle) {
                        if (result.isRootCorrect && result.isArticleCorrect) {
                            gained = 25
                        } else if (result.isRootCorrect && !result.isArticleCorrect) {
                            gained = 15
                        } else if (!result.isRootCorrect && result.isArticleCorrect) {
                            gained = 10
                            penalty = 15
                        } else {
                            penalty = 15
                        }
                    } else {
                        if (result.isRootCorrect) gained = 25 else penalty = 15
                    }
                } else if (result is AnswerResult.Simple) {
                     isOverallCorrect = result.isCorrect
                     if (result.isCorrect) gained = 25 else penalty = 15
                }
            }
        }
        
        newMastery = newMastery + gained - penalty
        if (newMastery >= 100) {
            newStatus = WordStatus.MASTERED
        } else if (newMastery < 100 && newStatus == WordStatus.MASTERED) {
            newStatus = WordStatus.IN_PROGRESS
        }
        
        return item.copy(
            mastery = newMastery.coerceIn(0, 100),
            lastTestedAtEpochMilli = answeredAtMilli,
            status = newStatus
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
        QuestionFormat(DomainQuestionType.TIME_ATTACK, TranslationDirection.CZ_TO_DE),
        QuestionFormat(DomainQuestionType.SCRAMBLED, TranslationDirection.CZ_TO_DE),
        QuestionFormat(DomainQuestionType.SKELETON, TranslationDirection.CZ_TO_DE),
        QuestionFormat(DomainQuestionType.THREE_STATE, TranslationDirection.CZ_TO_DE)
    )

    data class NextQuestion(
        val item: VocabItem,
        val format: QuestionFormat
    )

    fun getNextQuestion(words: List<VocabItem>, currentTimeMilli: Long): NextQuestion? {
        val selectedWord = selectNextWord(words, currentTimeMilli) ?: return null
        val format = selectFormat(selectedWord, currentTimeMilli)
        return NextQuestion(selectedWord, format)
    }
    
    // Zjistí efektivní mastery (zohledňuje časovou penalizaci)
    private fun getEffectiveMastery(word: VocabItem, currentTimeMilli: Long): Int {
        if (word.lastTestedAtEpochMilli == null) return word.mastery
        val millisPassed = currentTimeMilli - word.lastTestedAtEpochMilli
        val daysPassed = millisPassed / (1000.0 * 60 * 60 * 24)
        val penalty = (daysPassed * 2).toInt() // penalizace -2 mastery body za den
        return (word.mastery - penalty).coerceIn(0, 100)
    }

    private fun selectNextWord(words: List<VocabItem>, currentTimeMilli: Long): VocabItem? {
        // Vyřadíme LOCKED a MASTERED, kromě OVERDUE MASTERED
        // WIP LIMIT: max 30 sloviček (NEW + IN_PROGRESS)
        // Drip feeding se řeší před voláním této funkce v ViewModelu, tato funkce jen vybírá z aktivních.
        
        val activeWords = words.filter { it.status == WordStatus.NEW || it.status == WordStatus.IN_PROGRESS || (it.status == WordStatus.MASTERED && getEffectiveMastery(it, currentTimeMilli) < 95) }
        
        if (activeWords.isEmpty()) return null
        
        val weights = activeWords.map { word -> word to calculateWeight(word, currentTimeMilli) }
        val totalWeight = weights.sumOf { it.second }
        
        var randomValue = random.nextDouble(totalWeight)
        for ((word, weight) in weights) {
            randomValue -= weight
            if (randomValue <= 0.0) {
                return word
            }
        }
        
        return activeWords.last() 
    }

    private fun calculateWeight(word: VocabItem, currentTimeMilli: Long): Double {
        // Zvýšen minimální základ (offset) z 10.0 na 150.0. 
        val baseWeight = (100.0 - word.mastery).pow(2) + 150.0
        
        // Prioritizace New
        if (word.status == WordStatus.NEW) {
            return baseWeight * 10.0 // massive bonus
        }
        
        // Prioritizace Overdue
        val effectiveMastery = getEffectiveMastery(word, currentTimeMilli)
        if (word.status == WordStatus.MASTERED && effectiveMastery < 95) {
            val drop = 100 - effectiveMastery
            return baseWeight * (2.0 + drop * 0.5)
        }
        
        val multiplier = if (word.lastTestedAtEpochMilli == null) {
            3.0
        } else {
            val millisPassed = currentTimeMilli - word.lastTestedAtEpochMilli
            val daysPassed = millisPassed / (1000.0 * 60 * 60 * 24)
            1.0 + (daysPassed * 0.2)
        }
        
        return baseWeight * multiplier
    }

    private fun selectFormat(word: VocabItem, currentTimeMilli: Long): QuestionFormat {
        val allowedFormats = formatDifficultyOrder.filter { settings.isFormatAllowed(it) }
        if (allowedFormats.isEmpty()) {
            throw IllegalStateException("User settings have disabled all question formats.")
        }
        
        val effectiveMastery = getEffectiveMastery(word, currentTimeMilli)

        // PEVNÁ PODMÍNKA: Tier 5 je vynucen pro > 80% (pokud je povolen)
        val tier5 = formatDifficultyOrder[4] // Three state
        if (effectiveMastery > 80 && allowedFormats.contains(tier5)) {
            return tier5
        }
        
        // Peaks a Caps (zjednodušená pravidla z promptu)
        // Tier 1: 0, cap 20
        // Tier 2: 20, cap 40
        // Tier 3: 45, cap 60
        // Tier 4: 65, cap 80
        // Tier 5: 90, cap 100
        
        val peaks = mapOf(
            formatDifficultyOrder[0] to 0,
            formatDifficultyOrder[1] to 20,
            formatDifficultyOrder[2] to 45,
            formatDifficultyOrder[3] to 65,
            formatDifficultyOrder[4] to 90
        )

        val caps = mapOf(
            formatDifficultyOrder[0] to 20,
            formatDifficultyOrder[1] to 40,
            formatDifficultyOrder[2] to 60,
            formatDifficultyOrder[3] to 80,
            formatDifficultyOrder[4] to 100
        )

        // Odfiltrujeme formáty, jejichž cap je nižší než effective mastery (Hard Cap)
        val applicableFormats = allowedFormats.filter { format ->
            val cap = caps[format] ?: 100
            effectiveMastery <= cap
        }

        // Pokud všechny odfiltrujeme (např. vlivem settings a capů), vezmeme nejtěžší povolený formát
        val finalFormats = if (applicableFormats.isEmpty()) allowedFormats else applicableFormats

        val baseWeight = 10.0
        val dynamicPool = 100.0 - (baseWeight * finalFormats.size)

        val vScores = finalFormats.associateWith { format ->
            val peak = peaks[format] ?: 50
            val distance = abs(effectiveMastery - peak)
            maxOf(0, 100 - 2 * distance).toDouble()
        }

        val sumV = vScores.values.sum()
        
        val weights = finalFormats.associateWith { format ->
            if (sumV > 0) {
                baseWeight + (vScores[format]!! / sumV) * dynamicPool
            } else {
                100.0 / finalFormats.size
            }
        }

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
