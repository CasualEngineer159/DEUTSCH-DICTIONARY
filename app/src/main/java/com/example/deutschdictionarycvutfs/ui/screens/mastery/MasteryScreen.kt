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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.deutschdictionarycvutfs.DictionaryManager
import com.example.deutschdictionarycvutfs.Lesson
import com.example.deutschdictionarycvutfs.MasteryManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun MasteryScreen(
    dictionaryManager: DictionaryManager,
    masteryManager: MasteryManager,
    onLessonSelected: (String) -> Unit
) {
    var refreshTrigger by remember { mutableStateOf(0) }
    val lessons = remember(refreshTrigger) { dictionaryManager.getAllLessons() }
    
    var lessonToReset by remember { mutableStateOf<Lesson?>(null) }
    var resetTimer by remember { mutableStateOf(3) }

    LaunchedEffect(lessonToReset) {
        if (lessonToReset != null) {
            resetTimer = 3
            while (resetTimer > 0) {
                delay(1000)
                resetTimer--
            }
        }
    }

    if (lessonToReset != null) {
        AlertDialog(
            onDismissRequest = { lessonToReset = null },
            title = { Text("Zapomenout lekci") },
            text = { Text("Opravdu chcete vymazat veškerý postup u lekce '${lessonToReset?.lessonName}'? Tuto akci nelze vrátit zpět.") },
            confirmButton = {
                Button(
                    onClick = {
                        lessonToReset?.let { masteryManager.resetLessonMastery(it) }
                        lessonToReset = null
                        refreshTrigger++
                    },
                    enabled = resetTimer == 0,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (resetTimer > 0) "Smazat za $resetTimer" else "Smazat")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { lessonToReset = null }) {
                    Text("Zrušit")
                }
            }
        )
    }
    
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$masteryPercentInt %",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (masteryPercentInt == 100) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (masteryPercentInt > 0) {
                                        IconButton(
                                            onClick = { lessonToReset = lesson },
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text("🗑️")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}