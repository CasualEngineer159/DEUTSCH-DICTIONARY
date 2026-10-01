package com.example.deutschdictionarycvutfs.ui.screens.dictionary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.deutschdictionarycvutfs.DictionaryManager

@Composable
fun DictionaryListScreen(
    dictionaryManager: DictionaryManager,
    onLessonSelected: (String) -> Unit
) {
    val lessons = remember { dictionaryManager.getAllLessons() }

    if (lessons.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Žádné slovníky nebyly nalezeny.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(lessons) { (fileName, lesson) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onLessonSelected(fileName) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = lesson.lessonName,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "ID: ${lesson.lessonId} | ${lesson.categories.size} kategorií",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}