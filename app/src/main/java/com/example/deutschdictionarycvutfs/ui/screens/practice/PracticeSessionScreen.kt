package com.example.deutschdictionarycvutfs.ui.screens.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deutschdictionarycvutfs.AnswerValidator
import com.example.deutschdictionarycvutfs.DictionaryManager
import com.example.deutschdictionarycvutfs.DomainQuestionType
import com.example.deutschdictionarycvutfs.MasteryManager
import com.example.deutschdictionarycvutfs.TimeUtils
import com.example.deutschdictionarycvutfs.TranslationDirection
import com.example.deutschdictionarycvutfs.ui.models.AnswerState
import com.example.deutschdictionarycvutfs.ui.models.PracticeConfig
import com.example.deutschdictionarycvutfs.ui.models.SessionResult
import com.example.deutschdictionarycvutfs.ui.components.MaskedSkeletonTextField
import kotlinx.coroutines.delay
import kotlin.random.Random

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
    val selectedOption by viewModel.selectedOption.collectAsState()
    val sessionResult by viewModel.sessionResult.collectAsState()
    val skeletonHiddenIndices by viewModel.skeletonHiddenIndices.collectAsState()

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

    val edgeColor by animateColorAsState(
        targetValue = when (answerState) {
            AnswerState.CORRECT -> Color(0x664CAF50) // Green with opacity
            AnswerState.INCORRECT -> Color(0x66F44336) // Red with opacity
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 300)
    )

    if (currentQuestion == null) return

    val q = currentQuestion!!
    val sourceText = if (q.format.direction == TranslationDirection.CZ_TO_DE) q.item.wordCs else q.item.wordDe
    val targetText = if (q.format.direction == TranslationDirection.CZ_TO_DE) q.item.wordDe else q.item.wordCs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawWithContent {
                drawContent()
                if (edgeColor != Color.Transparent) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to edgeColor,
                            0.15f to Color.Transparent,
                            0.85f to Color.Transparent,
                            1.0f to edgeColor,
                            startY = 0f,
                            endY = size.height
                        )
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0.0f to edgeColor,
                            0.15f to Color.Transparent,
                            0.85f to Color.Transparent,
                            1.0f to edgeColor,
                            startX = 0f,
                            endX = size.width
                        )
                    )
                }
            }
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

            // Daily Quota Progress Bar
            val masteryData by masteryManager.masteryDataFlow.collectAsState()
            val currentEpochDay = remember { TimeUtils.getCurrentLocalEpochDay() }
            val pointsToday = masteryData.dailyPointsGained[currentEpochDay] ?: 0
            val quota = 1500
            val dailyProgress = (pointsToday.toFloat() / quota).coerceIn(0f, 1f)
            
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { dailyProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (dailyProgress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val animatedScale by animateFloatAsState(
                    targetValue = if (answerState != AnswerState.IDLE) 1f else 0.5f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
                val animatedAlpha by animateFloatAsState(
                    targetValue = if (answerState != AnswerState.IDLE) 1f else 0f,
                    animationSpec = tween(durationMillis = 200)
                )
                
                var lastActiveState by remember { mutableStateOf(AnswerState.IDLE) }
                if (answerState != AnswerState.IDLE) {
                    lastActiveState = answerState
                }

                if (animatedAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(animatedScale)
                            .alpha(animatedAlpha)
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (lastActiveState == AnswerState.CORRECT) "✓" else "✕",
                            color = if (lastActiveState == AnswerState.CORRECT) Color(0xFF4CAF50) else Color(0xFFF44336),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

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

            Spacer(modifier = Modifier.height(32.dp))

            val timeStart = remember(q) { System.currentTimeMillis() }
            
            if (q.format.type == DomainQuestionType.TIME_ATTACK) {
                var progress by remember { mutableStateOf(1f) }
                LaunchedEffect(q, answerState) {
                    if (answerState == AnswerState.IDLE) {
                        val duration = 5000L
                        while(true) {
                            val elapsed = System.currentTimeMillis() - timeStart
                            progress = 1f - (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                            if (elapsed > duration) break
                            delay(16)
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (progress > 0.3f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (q.format.type == DomainQuestionType.MULTIPLE_CHOICE || q.format.type == DomainQuestionType.TIME_ATTACK) {
                options.forEach { optionWord ->
                    val optionText = if (q.format.direction == TranslationDirection.CZ_TO_DE) optionWord.wordDe else optionWord.wordCs
                    
                    val isSelected = optionText == selectedOption
                    val isCorrectTarget = optionText == targetText
                    
                    val containerColor = when {
                        answerState == AnswerState.IDLE -> MaterialTheme.colorScheme.surfaceVariant
                        isCorrectTarget && answerState != AnswerState.IDLE -> Color(0xFFE8F5E9)
                        isSelected && answerState == AnswerState.INCORRECT -> Color(0xFFFFEBEE)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val contentColor = when {
                        answerState == AnswerState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                        isCorrectTarget && answerState != AnswerState.IDLE -> Color(0xFF2E7D32)
                        isSelected && answerState == AnswerState.INCORRECT -> Color(0xFFC62828)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                    val borderColor = when {
                        isCorrectTarget && answerState != AnswerState.IDLE -> Color(0xFF4CAF50)
                        isSelected && answerState == AnswerState.INCORRECT -> Color(0xFFF44336)
                        else -> Color.Transparent
                    }

                    Button(
                        onClick = { viewModel.checkAnswer(optionText, System.currentTimeMillis() - timeStart) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(56.dp),
                        enabled = answerState == AnswerState.IDLE,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = containerColor,
                            contentColor = contentColor,
                            disabledContainerColor = containerColor,
                            disabledContentColor = contentColor
                        ),
                        border = BorderStroke(2.dp, borderColor).takeIf { borderColor != Color.Transparent }
                    ) {
                        Text(text = optionText, style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                val isCorrectState = answerState == AnswerState.CORRECT
                val isIncorrectState = answerState == AnswerState.INCORRECT

                if (q.format.type == DomainQuestionType.SCRAMBLED) {
                    val scrambled = remember(q) {
                        targetText.toList().shuffled().joinToString(" ")
                    }
                    Text(
                        text = scrambled,
                        style = MaterialTheme.typography.headlineMedium,
                        letterSpacing = 4.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                } else if (q.format.type == DomainQuestionType.SKELETON) {
                    MaskedSkeletonTextField(
                        targetText = targetText,
                        hiddenIndices = skeletonHiddenIndices,
                        writtenAnswer = writtenAnswer,
                        onValueChange = { viewModel.updateWrittenAnswer(it) },
                        answerState = answerState,
                        onDone = { viewModel.checkAnswer(writtenAnswer.text) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                val isThreeState = q.format.type == DomainQuestionType.THREE_STATE
                val selectedArticle by viewModel.selectedArticle.collectAsState()

                if (isThreeState) {
                    val articles = listOf("der", "die", "das")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        articles.forEachIndexed { index, article ->
                            SegmentedButton(
                                selected = selectedArticle.equals(article, ignoreCase = true),
                                onClick = { viewModel.selectArticle(article) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = articles.size),
                                enabled = answerState == AnswerState.IDLE
                            ) {
                                Text(article)
                            }
                        }
                    }
                }

                if (q.format.type != DomainQuestionType.SKELETON) {
                    OutlinedTextField(
                        value = writtenAnswer,
                        onValueChange = { viewModel.updateWrittenAnswer(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (isThreeState) "Kmen slova" else "Tvoje odpověď") },
                        enabled = answerState == AnswerState.IDLE,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.checkAnswer(writtenAnswer.text) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = when {
                                isCorrectState -> Color(0xFF2E7D32)
                                isIncorrectState -> Color(0xFFC62828)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            disabledBorderColor = when {
                                isCorrectState -> Color(0xFF4CAF50)
                                isIncorrectState -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.outline
                            },
                            disabledLabelColor = when {
                                isCorrectState -> Color(0xFF4CAF50)
                                isIncorrectState -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                }
                
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
                        onClick = { 
                            if (q.format.type == DomainQuestionType.SKELETON) {
                                // For SKELETON, we need the reconstructed string. 
                                // It's easier to trigger the IME action. But we don't have direct access here.
                                // Actually, let's just keep a derived state in ViewModel, or reconstruct it here.
                                // To make it simpler, for SKELETON the checkAnswer button should be handled inside or we rebuild it.
                                // Let's reconstruct it here:
                                val skeletonValidIndices = targetText.indices.filter { targetText[it] != ' ' && targetText[it] != '-' }
                                val skeletonHiddenIndices = if (skeletonValidIndices.size <= 2) emptyList() else skeletonValidIndices.shuffled(
                                    Random(q.item.id.hashCode())
                                ).take((skeletonValidIndices.size * 0.4).toInt()).sorted() // Wait, random seed based on ID!
                                // Wait, in MaskedSkeletonTextField it uses `remember` without seed! That means it changes on recomposition if targetText changes.
                            }
                            viewModel.checkAnswer(writtenAnswer.text) 
                        },
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