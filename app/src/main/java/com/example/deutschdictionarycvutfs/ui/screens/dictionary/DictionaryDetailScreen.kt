package com.example.deutschdictionarycvutfs.ui.screens.dictionary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.deutschdictionarycvutfs.DictionaryManager

@Composable
fun DictionaryDetailScreen(
    fileName: String,
    dictionaryManager: DictionaryManager
) {
    val lesson = remember(fileName) { dictionaryManager.loadLesson(fileName) }

    if (lesson == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Slovník se nepodařilo načíst.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Text(
                text = lesson.lessonName,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
        }

        lesson.categories.forEach { category ->
            item {
                Text(
                    text = "${category.nameDe} / ${category.nameCs}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            items(category.words) { word ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = word.de,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = word.cs,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider()
            }
        }
    }
}