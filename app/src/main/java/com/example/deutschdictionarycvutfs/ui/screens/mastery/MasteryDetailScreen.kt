package com.example.deutschdictionarycvutfs.ui.screens.mastery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.deutschdictionarycvutfs.DictionaryManager
import com.example.deutschdictionarycvutfs.MasteryManager

@Composable
fun MasteryDetailScreen(
    fileName: String,
    dictionaryManager: DictionaryManager,
    masteryManager: MasteryManager
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
                text = lesson.lessonName + " - Mastery",
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
                val legacyKey = masteryManager.getLegacyWordKey(lesson.lessonId, word.de)
                val wordId = word.id ?: legacyKey
                val progress = masteryManager.getWordProgress(wordId, legacyKey)
                val masteryInt = progress.mastery

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = word.de,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = word.cs
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (masteryInt > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(masteryInt / 100f)
                                    .background(Color(0xFFC8E6C9))
                                    .align(Alignment.CenterStart)
                            )
                        }
                        Text(
                            text = "$masteryInt %",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (masteryInt >= 80) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}