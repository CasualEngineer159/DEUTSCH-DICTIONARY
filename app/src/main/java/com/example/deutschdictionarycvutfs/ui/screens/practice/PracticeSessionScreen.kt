package com.example.deutschdictionarycvutfs.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deutschdictionarycvutfs.DictionaryManager
import com.example.deutschdictionarycvutfs.DomainQuestionType
import com.example.deutschdictionarycvutfs.MasteryManager
import com.example.deutschdictionarycvutfs.TranslationDirection
import com.example.deutschdictionarycvutfs.ui.models.AnswerState
import com.example.deutschdictionarycvutfs.ui.models.PracticeConfig
import com.example.deutschdictionarycvutfs.ui.models.SessionResult
import kotlinx.coroutines.delay

@Composable
fun PracticeSessionScreen(
    config: PracticeConfig,
    dictionaryManager: DictionaryManager,
    masteryManager: MasteryManager,
    onFinish: (SessionResult) -> Unit,
    viewModel: PracticeSessionViewModel = viewModel(
        factory = PracticeSessionViewModelFactory(config, dictionaryManager, masteryManager)
    )
) {
    if (!viewModel.getHasWords()) {
        Text("Zvolené lekce neobsahují žádná slova.")
        return
    }

    val currentWordCount by viewModel.currentWordCount.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val options by viewModel.options.collectAsState()
    val answerState by viewModel.answerState.collectAsState()
    val writtenAnswer by viewModel.writtenAnswer.collectAsState()
    val sessionResult by viewModel.sessionResult.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.start()
    }

    LaunchedEffect(answerState) {
        if (answerState == AnswerState.CORRECT) {
            delay(500)
            viewModel.generateNextQuestion()
        }
    }
    
    LaunchedEffect(sessionResult) {
        if (sessionResult != null) {
            onFinish(sessionResult!!)
        }
    }

    val backgroundColor = when (answerState) {
        AnswerState.IDLE -> MaterialTheme.colorScheme.background
        AnswerState.CORRECT -> Color(0xFFE8F5E9)
        AnswerState.INCORRECT -> Color(0xFFFFEBEE)
    }

    if (currentQuestion == null) return

    val q = currentQuestion!!
    val sourceText = if (q.format.direction == TranslationDirection.CZ_TO_DE) q.item.wordCs else q.item.wordDe
    val targetText = if (q.format.direction == TranslationDirection.CZ_TO_DE) q.item.wordDe else q.item.wordCs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (config.wordCount == -1) "Slovo: $currentWordCount" else "Slovo: $currentWordCount / ${config.wordCount}",
                    style = MaterialTheme.typography.labelLarge
                )
                OutlinedButton(onClick = { viewModel.finishSession() }) {
                    Text("Ukončit")
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = sourceText,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (q.format.direction == TranslationDirection.CZ_TO_DE) "Přelož do němčiny" else "Přelož do češtiny",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (q.format.type == DomainQuestionType.MULTIPLE_CHOICE) {
                options.forEach { optionWord ->
                    val optionText = if (q.format.direction == TranslationDirection.CZ_TO_DE) optionWord.wordDe else optionWord.wordCs
                    Button(
                        onClick = { viewModel.checkAnswer(optionText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(56.dp),
                        enabled = answerState == AnswerState.IDLE,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(text = optionText, style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                OutlinedTextField(
                    value = writtenAnswer,
                    onValueChange = { viewModel.updateWrittenAnswer(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tvoje odpověď") },
                    enabled = answerState == AnswerState.IDLE,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.checkAnswer(writtenAnswer.text) })
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (q.format.direction == TranslationDirection.CZ_TO_DE && answerState == AnswerState.IDLE) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val chars = listOf("ä", "ö", "ü", "ß")
                        chars.forEach { char ->
                            OutlinedButton(
                                onClick = {
                                    val text = writtenAnswer.text
                                    val selection = writtenAnswer.selection
                                    val newText = text.replaceRange(selection.min, selection.max, char)
                                    val newSelection = TextRange(selection.min + char.length)
                                    viewModel.updateWrittenAnswer(TextFieldValue(newText, newSelection))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(char, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                if (answerState == AnswerState.IDLE) {
                    Button(
                        onClick = { viewModel.checkAnswer(writtenAnswer.text) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Zkontrolovat", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (answerState == AnswerState.INCORRECT) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Správná odpověď:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = targetText,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.generateNextQuestion() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Další slovíčko")
                        }
                    }
                }
            }
        }
    }
}