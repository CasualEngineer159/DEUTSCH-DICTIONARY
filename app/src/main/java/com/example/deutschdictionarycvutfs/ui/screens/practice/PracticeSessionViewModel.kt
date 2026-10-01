package com.example.deutschdictionarycvutfs.ui.screens.practice

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

        val types = mutableSetOf<DomainQuestionType>()
        if (config.isMultipleChoice) types.add(DomainQuestionType.MULTIPLE_CHOICE)
        if (config.isWrittenTranslation) types.add(DomainQuestionType.WRITTEN)
        
        val directions = mutableSetOf<TranslationDirection>()
        if (config.isCzToDe) directions.add(TranslationDirection.CZ_TO_DE)
        if (config.isDeToCz) directions.add(TranslationDirection.DE_TO_CZ)
        
        engine = PracticeSessionEngine(PracticeSettings(types, directions))
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
                    lastTestedAtEpochMilli = progress.lastTestedAtEpochMilli
                )
            }

            val nextQ = engine.getNextQuestion(vocabItems, System.currentTimeMillis())
            if (nextQ == null) {
                finishSession()
            } else {
                _currentQuestion.value = nextQ

                if (nextQ.format.type == DomainQuestionType.MULTIPLE_CHOICE) {
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

    fun checkAnswer(userAnswer: String) {
        if (_answerState.value == AnswerState.IDLE && _currentQuestion.value != null) {
            val q = _currentQuestion.value!!
            val isCorrect = AnswerValidator.isCorrect(userAnswer, q.item, q.format.direction)
            
            val updatedVocabItem = MasteryUpdater.applyAnswer(
                item = q.item,
                format = q.format,
                isCorrect = isCorrect,
                answeredAtMilli = System.currentTimeMillis()
            )
            
            masteryManager.setWordProgress(
                wordId = q.item.id,
                progress = WordProgress(
                    mastery = updatedVocabItem.mastery,
                    lastTestedAtEpochMilli = updatedVocabItem.lastTestedAtEpochMilli
                )
            )

            if (isCorrect) {
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