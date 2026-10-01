package com.example.deutschdictionarycvutfs

import org.junit.Test
import java.io.File
import java.util.UUID

class UpdateJsons {
    // @Test
    fun updateJsonFilesWithIds() {
        val assetsDir = File("src/main/assets")
        val jsonFiles = assetsDir.listFiles { file -> file.name.endsWith(".json") }
        
        jsonFiles?.forEach { file ->
            var content = file.readText()
            
            // Regex to match a word object like { "de": "word", ... }
            // but ONLY if it doesn't already have an id
            val regex = """\{\s*"de"\s*:""".toRegex()
            
            // Use replace with a generator function to inject unique UUIDs
            val updatedContent = regex.replace(content) { matchResult ->
                val newId = UUID.randomUUID().toString()
                """{ "id": "$newId", "de":"""
            }
            
            if (content != updatedContent) {
                file.writeText(updatedContent)
                println("Updated ${file.name}")
            }
        }
    }
}