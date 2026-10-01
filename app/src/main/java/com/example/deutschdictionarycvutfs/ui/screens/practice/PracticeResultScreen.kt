package com.example.deutschdictionarycvutfs.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.deutschdictionarycvutfs.ui.models.SessionResult

@Composable
fun PracticeResultScreen(
    result: SessionResult,
    onNavigateHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Trénink dokončen!",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Tvoje úspěšnost", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                val percentage = if (result.totalAnswered > 0) {
                    (result.correctAnswers.toFloat() / result.totalAnswered * 100).toInt()
                } else {
                    0
                }
                
                Text(
                    text = "${result.correctAnswers} / ${result.totalAnswered} ($percentage %)",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (percentage >= 80) Color(0xFF2E7D32) else if (percentage >= 50) Color(0xFFF57F17) else MaterialTheme.colorScheme.error
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (result.masteryChanges.isNotEmpty()) {
            Text(
                text = "Vývoj Mastery:", 
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            result.masteryChanges.forEach { (lessonName, change) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = lessonName, style = MaterialTheme.typography.bodyLarge)
                    
                    val formattedChange = String.format("%.1f", change).replace(',', '.')
                    val sign = if (change > 0) "+" else ""
                    val color = if (change > 0) Color(0xFF2E7D32) else if (change < 0) MaterialTheme.colorScheme.error else Color.Gray
                    
                    Text(
                        text = "$sign$formattedChange %",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                HorizontalDivider()
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onNavigateHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Zpět do menu", style = MaterialTheme.typography.titleMedium)
        }
    }
}