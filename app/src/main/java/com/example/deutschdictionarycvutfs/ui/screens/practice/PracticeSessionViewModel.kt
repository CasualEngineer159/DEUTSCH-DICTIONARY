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

        // Settings zjednodušeno, všechny formáty jsou defaultně povolené
        engine = PracticeSessionEngine(PracticeSettings())
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

            val currentTimeMilli = System.currentTimeMillis()
            val currentEpochDay = currentTimeMilli / (1000 * 60 * 60 * 24)

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

            // Drip-Feeding Logic
            var activeCount = vocabItems.count { it.status == WordStatus.NEW || it.status == WordStatus.IN_PROGRESS }
            
            var unlockedToday = if (masteryManager.masteryData.lastUnlockEpochDay == currentEpochDay) masteryManager.masteryData.unlockedTodayCount else 0

            // Priorita 1: Zamrzlá historie (rozmrazování starého progresu)
            val iceboxWords = vocabItems.filter { it.status == WordStatus.LOCKED && (it.mastery > 0 || it.lastTestedAtEpochMilli != null) }
                .sortedByDescending { it.mastery }
                
            // Priorita 2: Zcela nová slova
            val freshWords = vocabItems.filter { it.status == WordStatus.LOCKED && it.mastery == 0 && it.lastTestedAtEpochMilli == null }
            
            var changedStatuses = false

            // Rozmrazujeme dříve načatá slova jako IN_PROGRESS (bez NEW bonusu)
            for (locked in iceboxWords) {
                if (activeCount >= 30 || unlockedToday >= 15) break
                val index = vocabItems.indexOf(locked)
                vocabItems[index] = locked.copy(status = WordStatus.IN_PROGRESS)
                masteryManager.setWordProgress(locked.id, WordProgress(locked.mastery, locked.lastTestedAtEpochMilli, WordStatus.IN_PROGRESS))
                activeCount++
                unlockedToday++
                changedStatuses = true
            }

            // Až pokud je po rozmrazování pořád kapacita, uvolňujeme zcela nová slova (jako NEW)
            for (locked in freshWords) {
                if (activeCount >= 30 || unlockedToday >= 15) break
                val index = vocabItems.indexOf(locked)
                vocabItems[index] = locked.copy(status = WordStatus.NEW)
                masteryManager.setWordProgress(locked.id, WordProgress(locked.mastery, locked.lastTestedAtEpochMilli, WordStatus.NEW))
                activeCount++
                unlockedToday++
                changedStatuses = true
            }

            if (changedStatuses) {
                masteryManager.updateUnlockData(currentEpochDay, unlockedToday)
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
        _selectedArticle.value = article
    }

    fun checkAnswer(userAnswer: String, timeTakenMilli: Long? = null) {
        if (_answerState.value == AnswerState.IDLE && _currentQuestion.value != null) {
            _selectedOption.value = userAnswer
            
            val q = _currentQuestion.value!!
            
            val result = if (q.format.type == DomainQuestionType.THREE_STATE && AnswerValidator.articleRegex.matches(q.item.wordDe)) {
                val match = AnswerValidator.articleRegex.find(q.item.wordDe)!!
                val correctArticle = match.groupValues[1]
                val correctRoot = match.groupValues[2]
                
                val isArticleCorrect = _selectedArticle.value.equals(correctArticle, ignoreCase = true)
                val isRootCorrect = userAnswer.trim().equals(correctRoot, ignoreCase = true)
                
                AnswerResult.ThreeState(
                    hasArticle = true,
                    isArticleCorrect = isArticleCorrect,
                    isRootCorrect = isRootCorrect
                )
            } else {
                AnswerResult.Simple(
                    isCorrect = AnswerValidator.validateSimple(userAnswer, q.item, q.format.direction),
                    timeTakenMilli = timeTakenMilli
                )
            }
            
            val updatedVocabItem = MasteryUpdater.applyAnswer(
                item = q.item,
                format = q.format,
                result = result,
                answeredAtMilli = System.currentTimeMillis()
            )
            
            val isOverallCorrect = when(result) {
                is AnswerResult.Simple -> result.isCorrect
                is AnswerResult.ThreeState -> result.isRootCorrect && (!result.hasArticle || result.isArticleCorrect)
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