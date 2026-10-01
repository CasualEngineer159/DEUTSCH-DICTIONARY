package com.example.deutschdictionarycvutfs

import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val lessonId: String,
    val lessonName: String,
    val categories: List<Category>
)

@Serializable
data class Category(
    val nameDe: String,
    val nameCs: String,
    val words: List<Word>
)

@Serializable
data class Word(
    val id: String? = null, // Pokud chybí v JSONu, použije se null
    val de: String,
    val cs: String,
    val synonymsDe: List<String> = emptyList(),
    val synonymsCs: List<String> = emptyList()
)