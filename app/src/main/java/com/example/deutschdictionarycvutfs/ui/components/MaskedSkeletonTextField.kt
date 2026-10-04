package com.example.deutschdictionarycvutfs.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.deutschdictionarycvutfs.ui.models.AnswerState

@Composable
fun MaskedSkeletonTextField(
    targetText: String,
    hiddenIndices: List<Int>,
    writtenAnswer: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    answerState: AnswerState,
    onDone: () -> Unit
) {
    val visualTransformation = remember(targetText, hiddenIndices) {
        VisualTransformation { text ->
            val typedChars = text.text
            val out = buildAnnotatedString {
                var typedIdx = 0
                for (i in targetText.indices) {
                    if (i in hiddenIndices) {
                        if (typedIdx < typedChars.length) {
                            append(typedChars[typedIdx].toString())
                            typedIdx++
                        } else {
                            append("_")
                        }
                    } else {
                        append(targetText[i].toString())
                    }
                }
            }
            
            val offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (hiddenIndices.isEmpty()) return targetText.length
                    if (offset >= hiddenIndices.size) return hiddenIndices.last() + 1
                    return hiddenIndices[offset]
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (hiddenIndices.isEmpty()) return 0
                    var originalOffset = 0
                    for (i in hiddenIndices) {
                        if (offset > i) {
                            originalOffset++
                        } else {
                            break
                        }
                    }
                    return originalOffset.coerceIn(0, text.text.length)
                }
            }
            TransformedText(out, offsetMapping)
        }
    }

    val isCorrectState = answerState == AnswerState.CORRECT
    val isIncorrectState = answerState == AnswerState.INCORRECT

    OutlinedTextField(
        value = writtenAnswer,
        onValueChange = { newValue ->
            // Prevent typing more characters than hidden slots
            if (newValue.text.length <= hiddenIndices.size) {
                onValueChange(newValue)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.headlineMedium.copy(
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center
        ),
        visualTransformation = visualTransformation,
        enabled = answerState == AnswerState.IDLE,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
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
            }
        )
    )
}
