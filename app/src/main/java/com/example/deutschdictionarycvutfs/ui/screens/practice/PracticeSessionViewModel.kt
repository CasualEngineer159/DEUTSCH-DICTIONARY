package com.example.deutschdictionarycvutfs.ui.screens.practice

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.deutschdictionarycvutfs.AnswerResult
import com.example.deutschdictionarycvutfs.AnswerValidator
import com.example.deutschdictionarycvutfs.DictionaryManager
import com.example.deutschdictionarycvutfs.DomainQuestionType
import com.example.deutschdictionarycvutfs.MasteryManager
import com.example.deutschdictionarycvutfs.MasteryUpdater
import com.example.deutschdictionarycvutfs.PracticeSessionEngine
import com.example.deutschdictionarycvutfs.PracticeSettings
import com.example.deutschdictionarycvutfs.TimeUtils
import com.example.deutschdictionarycvutfs.TranslationDirection
import com.example.deutschdictionarycvutfs.VocabItem
import com.example.deutschdictionarycvutfs.WordProgress
import com.example.deutschdictionarycvutfs.WordStatus
import com.example.deutschdictionarycvutfs.ui.models.AnswerState
import com.example.deutschdictionarycvutfs.ui.models.PracticeConfig
import com.example.deutschdictionarycvutfs.ui.models.SessionResult
import com.example.deutschdictionarycvutfs.ui.models.WordContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PracticeSessionViewModel(
    private val config: PracticeConfig,
    private val dictionaryManager: DictionaryManager,
    private val masteryManager: MasteryManager
) : ViewModel() {

    private val _currentWordCount = MutableStateFlow(0)
    val currentWordCount: StateFlow<Int> = _currentWordCount.asStateFlow()

    private val _answeredQuestionsCount = MutableStateFlow(0)
    val answeredQuestionsCount: StateFlow<Int> = _answeredQuestionsCount.asStateFlow()

    private val _correctAnswersCount = MutableStateFlow(0)
    val correctAnswersCount: StateFlow<Int> = _correctAnswersCount.asStateFlow()

    private val _currentQuestion = MutableStateFlow<PracticeSessionEngine.NextQuestion?>(null)
    val currentQuestion: StateFlow<PracticeSessionEngine.NextQuestion?> = _currentQuestion.asStateFlow()

    private val _options = MutableStateFlow<List<VocabItem>>(emptyList())
    val options: StateFlow<List<VocabItem>> = _options.asStateFlow()

    private val _selectedOption = MutableStateFlow<String?>(null)
    val selectedOption: StateFlow<String?> = _selectedOption.asStateFlow()

    private val _answerState = MutableStateFlow(AnswerState.IDLE)
    val answerState: StateFlow<AnswerState> = _answerState.asStateFlow()

    private val _skeletonHiddenIndices = MutableStateFlow<List<Int>>(emptyList())
    val skeletonHiddenIndices: StateFlow<List<Int>> = _skeletonHiddenIndices.asStateFlow()

    private val _writtenAnswer = MutableStateFlow(TextFieldValue(""))
    val writtenAnswer: StateFlow<TextFieldValue> = _writtenAnswer.asStateFlow()

    private val _sessionResult = MutableStateFlow<SessionResult?>(null)
    val sessionResult: StateFlow<SessionResult?> = _sessionResult.asStateFlow()

    private val allWordsContext: List<WordContext>
    private val initialMasteries: Map<String, Float>
    private val engine: PracticeSessionEngine

    init {
        allWordsContext = config.selectedLessons.flatMap { fileName ->
            val lesson = dictionaryManager.loadLesson(fileName)
            lesson?.categories?.flatMap { cat ->
                cat.words.map { word -> WordContext(word, lesson.lessonName, lesson.lessonId) }
            } ?: emptyList()
        }

        initialMasteries = config.selectedLessons.associateWith { fileName ->
            val lesson = dictionaryManager.loadLesson(fileName)
            if (lesson != null) masteryManager.getLessonMastery(lesson) else 0f
        }

        engine = PracticeSessionEngine(PracticeSettings(allowedQuestionTypes = config.allowedFormats))
    }
    
    fun start() {
        generateNextQuestion()
    }

    fun generateNextQuestion() {
        if (config.wordCount != -1 && _currentWordCount.value >= config.wordCount) {
            finishSession()
        } else {
            _answerState.value = AnswerState.IDLE
            _writtenAnswer.value = TextFieldValue("")
            _selectedOption.value = null
            _selectedArticle.value = null

            val currentTimeMilli = System.currentTimeMillis()
            val currentEpochDay = TimeUtils.getCurrentLocalEpochDay()

            val vocabItems = allWordsContext.map { ctx ->
                val legacyKey = masteryManager.getLegacyWordKey(ctx.lessonId, ctx.word.de)
                val wordId = ctx.word.id ?: legacyKey
                val progress = masteryManager.getWordProgress(wordId, legacyKey)
                VocabItem(
                    id = wordId,
                    lessonId = ctx.lessonId,
                    wordDe = ctx.word.de,
                    wordCs = ctx.word.cs,
                    synonymsDe = ctx.word.synonymsDe,
                    synonymsCs = ctx.word.synonymsCs,
                    mastery = progress.mastery,
                    lastTestedAtEpochMilli = progress.lastTestedAtEpochMilli,
                    status = progress.status ?: WordStatus.LOCKED
                )
            }.toMutableList()

            // WIP Limit Enforcement
            var activeCount = vocabItems.count { it.status == WordStatus.NEW || it.status == WordStatus.IN_PROGRESS }

            val iceboxWords = vocabItems.filter { it.status == WordStatus.LOCKED && (it.mastery > 0 || it.lastTestedAtEpochMilli != null) }
                .sortedByDescending { it.mastery }
                
            val freshWords = vocabItems.filter { it.status == WordStatus.LOCKED && it.mastery == 0 && it.lastTestedAtEpochMilli == null }
            
            for (locked in iceboxWords) {
                if (activeCount >= 30) break
                val index = vocabItems.indexOf(locked)
                vocabItems[index] = locked.copy(status = WordStatus.IN_PROGRESS)
                masteryManager.setWordProgress(locked.id, WordProgress(locked.mastery, locked.lastTestedAtEpochMilli, WordStatus.IN_PROGRESS))
                activeCount++
            }

            for (locked in freshWords) {
                if (activeCount >= 30) break
                val index = vocabItems.indexOf(locked)
                vocabItems[index] = locked.copy(status = WordStatus.NEW)
                masteryManager.setWordProgress(locked.id, WordProgress(locked.mastery, locked.lastTestedAtEpochMilli, WordStatus.NEW))
                activeCount++
            }

            val nextQ = engine.getNextQuestion(vocabItems, currentTimeMilli)
            if (nextQ == null) {
                finishSession()
            } else {
                _currentQuestion.value = nextQ

                if (nextQ.format.type == DomainQuestionType.MULTIPLE_CHOICE || nextQ.format.type == DomainQuestionType.TIME_ATTACK) {
                    val distractors = vocabItems.filter { it.id != nextQ.item.id }.shuffled().take(4)
                    _options.value = (distractors + nextQ.item).shuffled()
                }
                
                if (nextQ.format.type == DomainQuestionType.SKELETON) {
                    val targetText = if (nextQ.format.direction == TranslationDirection.CZ_TO_DE) nextQ.item.wordDe else nextQ.item.wordCs
                    val validIndices = targetText.indices.filter { targetText[it] != ' ' && targetText[it] != '-' }
                    if (validIndices.size <= 2) {
                        _skeletonHiddenIndices.value = emptyList()
                    } else {
                        val numToHide = (validIndices.size * 0.4).toInt()
                        _skeletonHiddenIndices.value = validIndices.shuffled().take(numToHide).sorted()
                    }
                } else {
                    _skeletonHiddenIndices.value = emptyList()
                }

                _currentWordCount.value++
            }
        }
    }

    fun finishSession() {
        val finalMasteries = config.selectedLessons.associateWith { fileName ->
            val lesson = dictionaryManager.loadLesson(fileName)
            if (lesson != null) masteryManager.getLessonMastery(lesson) else 0f
        }
        
        val masteryChanges = finalMasteries.mapNotNull { (fileName, finalMastery) ->
            val initial = initialMasteries[fileName] ?: 0f
            val diff = finalMastery - initial
            if (diff != 0f) {
                val lessonName = dictionaryManager.loadLesson(fileName)?.lessonName ?: fileName
                lessonName to diff
            } else null
        }.toMap()

        _sessionResult.value = SessionResult(
            correctAnswers = _correctAnswersCount.value,
            totalAnswered = _answeredQuestionsCount.value,
            masteryChanges = masteryChanges
        )
    }

    fun updateWrittenAnswer(newValue: TextFieldValue) {
        _writtenAnswer.value = newValue
    }

    // Tier 5: Three-State Input answers
    private val _selectedArticle = MutableStateFlow<String?>(null)
    val selectedArticle: StateFlow<String?> = _selectedArticle.asStateFlow()

    fun selectArticle(article: String) {
        if (_selectedArticle.value.equals(article, ignoreCase = true)) {
            _selectedArticle.value = null
        } else {
            _selectedArticle.value = article
        }
    }

    fun checkAnswer(userAnswer: String, timeTakenMilli: Long? = null) {
        if (_answerState.value == AnswerState.IDLE && _currentQuestion.value != null) {
            val q = _currentQuestion.value!!
            
            val actualUserAnswer = if (q.format.type == DomainQuestionType.SKELETON) {
                val targetText = if (q.format.direction == TranslationDirection.CZ_TO_DE) q.item.wordDe else q.item.wordCs
                val hiddenIndices = _skeletonHiddenIndices.value
                val typedChars = userAnswer
                buildString {
                    var typedIdx = 0
                    for (i in targetText.indices) {
                        if (i in hiddenIndices) {
                            if (typedIdx < typedChars.length) {
                                append(typedChars[typedIdx].toString())
                                typedIdx++
                            } else {
                                append("_")
                            }
                        } else {
                            append(targetText[i].toString())
                        }
                    }
                }
            } else {
                userAnswer
            }

            _selectedOption.value = actualUserAnswer
            
            val result = if (q.format.type == DomainQuestionType.THREE_STATE) {
                if (AnswerValidator.articleRegex.matches(q.item.wordDe)) {
                    val match = AnswerValidator.articleRegex.find(q.item.wordDe)!!
                    val correctArticle = match.groupValues[1]
                    val correctRoot = match.groupValues[2]
                    
                    val isArticleCorrect = _selectedArticle.value.equals(correctArticle, ignoreCase = true)
                    val isRootCorrect = actualUserAnswer.trim().equals(correctRoot, ignoreCase = true)
                    
                    AnswerResult.ThreeState(
                        hasArticle = true,
                        isArticleCorrect = isArticleCorrect,
                        isRootCorrect = isRootCorrect
                    )
                } else {
                    // No article expected. If user selected one, it's incorrect.
                    val isArticleCorrect = _selectedArticle.value == null
                    val isRootCorrect = AnswerValidator.validateSimple(actualUserAnswer, q.item, q.format.direction)
                    
                    AnswerResult.ThreeState(
                        hasArticle = false,
                        isArticleCorrect = isArticleCorrect,
                        isRootCorrect = isRootCorrect
                    )
                }
            } else {
                AnswerResult.Simple(
                    isCorrect = AnswerValidator.validateSimple(actualUserAnswer, q.item, q.format.direction),
                    timeTakenMilli = timeTakenMilli
                )
            }
            
            val oldMastery = q.item.mastery
            val updatedVocabItem = MasteryUpdater.applyAnswer(
                item = q.item,
                format = q.format,
                result = result,
                answeredAtMilli = System.currentTimeMillis()
            )
            
            val actualGain = maxOf(0, updatedVocabItem.mastery - oldMastery)
            if (actualGain > 0) {
                val epochDay = TimeUtils.getCurrentLocalEpochDay()
                masteryManager.addDailyPoints(epochDay, actualGain)
            }
            
            val isOverallCorrect = when(result) {
                is AnswerResult.Simple -> result.isCorrect
                is AnswerResult.ThreeState -> result.isRootCorrect && result.isArticleCorrect
            }
            
            masteryManager.setWordProgress(
                wordId = q.item.id,
                progress = WordProgress(
                    mastery = updatedVocabItem.mastery,
                    lastTestedAtEpochMilli = updatedVocabItem.lastTestedAtEpochMilli,
                    status = updatedVocabItem.status
                )
            )

            if (isOverallCorrect) {
                _answerState.value = AnswerState.CORRECT
                _correctAnswersCount.value++
            } else {
                _answerState.value = AnswerState.INCORRECT
            }
            _answeredQuestionsCount.value++
        }
    }
    
    fun getHasWords(): Boolean = allWordsContext.isNotEmpty()
}

class PracticeSessionViewModelFactory(
    private val config: PracticeConfig,
    private val dictionaryManager: DictionaryManager,
    private val masteryManager: MasteryManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeSessionViewModel::class.java)) {
            return PracticeSessionViewModel(config, dictionaryManager, masteryManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}