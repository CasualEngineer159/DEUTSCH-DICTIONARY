package com.example.deutschdictionarycvutfs

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.IOException

class DictionaryManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun getAllLessons(): List<Pair<String, Lesson>> {
        val lessons = mutableListOf<Pair<String, Lesson>>()
        try {
            val files = context.assets.list("") ?: arrayOf()
            for (file in files) {
                if (file.endsWith(".json")) {
                    val lesson = loadLesson(file)
                    if (lesson != null) {
                        lessons.add(Pair(file, lesson))
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        
        // Seřadit lekce podle čísla v názvu souboru (např. lesson1.json -> 1, lesson10.json -> 10)
        return lessons.sortedBy { (fileName, _) ->
            // Extrakce čísla z názvu souboru (vyhledá první posloupnost číslic)
            val numberInName = Regex("\\d+").find(fileName)?.value?.toIntOrNull() ?: Int.MAX_VALUE
            numberInName
        }
    }

    fun loadLesson(fileName: String): Lesson? {
        val jsonString = loadJSONFromAsset(fileName)
        return if (jsonString != null) {
            try {
                json.decodeFromString<Lesson>(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    private fun loadJSONFromAsset(fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
    }
}