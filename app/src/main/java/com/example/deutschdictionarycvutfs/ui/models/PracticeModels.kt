package com.example.deutschdictionarycvutfs.ui.models

import com.example.deutschdictionarycvutfs.Word

// Konfigurace pro trénink
data class PracticeConfig(
    val selectedLessons: List<String>,
    val isMultipleChoice: Boolean,
    val isWrittenTranslation: Boolean,
    val isCzToDe: Boolean,
    val isDeToCz: Boolean,
    val wordCount: Int
)

// Výsledek tréninku
data class SessionResult(
    val correctAnswers: Int,
    val totalAnswered: Int,
    val masteryChanges: Map<String, Float> // Lesson Name -> Změna mastery (např. +3.5f, -1.2f)
)

// Obal pro slovíčko
data class WordContext(
    val word: Word,
    val lessonName: String,
    val lessonId: String
)

enum class PracticeLength(val title: String, val wordCount: Int) {
    SHORT("Krátký (10 slov)", 10),
    STANDARD("Standard (20 slov)", 20),
    LONG("Dlouhý (30 slov)", 30),
    ENDLESS("Nekonečný", -1)
}

enum class AnswerState { IDLE, CORRECT, INCORRECT }