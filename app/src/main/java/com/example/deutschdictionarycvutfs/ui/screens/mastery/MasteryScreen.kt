package com.example.deutschdictionarycvutfs.ui.screens.mastery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.deutschdictionarycvutfs.DictionaryManager
import com.example.deutschdictionarycvutfs.MasteryManager
import kotlin.math.roundToInt

@Composable
fun MasteryScreen(
    dictionaryManager: DictionaryManager,
    masteryManager: MasteryManager,
    onLessonSelected: (String) -> Unit
) {
    val lessons = remember { dictionaryManager.getAllLessons() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mastery Přehled",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
        )

        if (lessons.isEmpty()) {
            Text("Žádné lekce nebyly nalezeny.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(lessons) { (fileName, lesson) ->
                    val masteryFloat = masteryManager.getLessonMastery(lesson)
                    val masteryPercentInt = masteryFloat.roundToInt()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLessonSelected(fileName) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        ) {
                            if (masteryFloat > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(masteryFloat / 100f)
                                        .background(Color(0xFFC8E6C9))
                                )
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lesson.lessonName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$masteryPercentInt %",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (masteryPercentInt == 100) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}