package com.example.deutschdictionarycvutfs.ui.screens.practice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.deutschdictionarycvutfs.DictionaryManager
import com.example.deutschdictionarycvutfs.ui.components.SetupCheckboxRow
import com.example.deutschdictionarycvutfs.ui.models.PracticeConfig
import com.example.deutschdictionarycvutfs.ui.models.PracticeLength

@Composable
fun PracticeSetupScreen(
    dictionaryManager: DictionaryManager,
    onStartPractice: (PracticeConfig) -> Unit
) {
    val lessons = remember { dictionaryManager.getAllLessons() }
    val selectedLessons = remember { 
        mutableStateListOf<String>().apply {
            addAll(lessons.map { it.first })
        }
    }

    var isMultipleChoice by remember { mutableStateOf(true) }
    var isWrittenTranslation by remember { mutableStateOf(true) }
    var isCzToDe by remember { mutableStateOf(true) }
    var isDeToCz by remember { mutableStateOf(true) }
    var selectedLength by remember { mutableStateOf(PracticeLength.STANDARD) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Nastavení procvičování",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(text = "1. Výběr lekcí", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        if (lessons.isEmpty()) {
            Text("Žádné lekce nebyly nalezeny.")
        } else {
            lessons.forEach { (fileName, lesson) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (selectedLessons.contains(fileName)) selectedLessons.remove(fileName)
                            else selectedLessons.add(fileName)
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = selectedLessons.contains(fileName), onCheckedChange = null)
                    Text(text = lesson.lessonName, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "2. Způsob procvičování", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        SetupCheckboxRow("Výběr z několika možností", isMultipleChoice) { isMultipleChoice = it }
        SetupCheckboxRow("Napsat překlad sám", isWrittenTranslation) { isWrittenTranslation = it }
        Spacer(modifier = Modifier.height(8.dp))
        SetupCheckboxRow("Čeština -> Němčina", isCzToDe) { isCzToDe = it }
        SetupCheckboxRow("Němčina -> Čeština", isDeToCz) { isDeToCz = it }
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "3. Délka procvičování", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        PracticeLength.entries.forEach { lengthOption ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedLength = lengthOption }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(selected = selectedLength == lengthOption, onClick = null)
                Text(text = lengthOption.title, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        val canStart = selectedLessons.isNotEmpty() &&
                (isMultipleChoice || isWrittenTranslation) &&
                (isCzToDe || isDeToCz)

        Button(
            onClick = {
                onStartPractice(
                    PracticeConfig(
                        selectedLessons = selectedLessons.toList(),
                        isMultipleChoice = isMultipleChoice,
                        isWrittenTranslation = isWrittenTranslation,
                        isCzToDe = isCzToDe,
                        isDeToCz = isDeToCz,
                        wordCount = selectedLength.wordCount
                    )
                )
            },
            enabled = canStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "Začít trénink", style = MaterialTheme.typography.titleMedium)
        }
        if (!canStart) {
            Text(
                text = "Musíte vybrat alespoň jednu lekci, jeden typ odpovědi a jeden směr překladu.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}