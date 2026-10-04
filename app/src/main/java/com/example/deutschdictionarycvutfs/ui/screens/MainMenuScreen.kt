package com.example.deutschdictionarycvutfs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.deutschdictionarycvutfs.MasteryManager
import com.example.deutschdictionarycvutfs.TimeUtils

@Composable
fun MainMenuScreen(
    masteryManager: MasteryManager,
    onNavigateToDictionaries: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToMastery: () -> Unit,
    onNavigateToHeatmap: () -> Unit,
    onQuickStart: () -> Unit
) {
    val masteryData by masteryManager.masteryDataFlow.collectAsState()
    val currentEpochDay = remember { TimeUtils.getCurrentLocalEpochDay() }
    val pointsToday = masteryData.dailyPointsGained[currentEpochDay] ?: 0
    val quota = 1500
    val progress = (pointsToday.toFloat() / quota).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.8f).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Denní cíl: $pointsToday / $quota bodů", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (progress >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = onQuickStart,
            modifier = Modifier.fillMaxWidth(0.8f).height(64.dp)
        ) {
            Text(text = "Rychlý start", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNavigateToDictionaries,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(text = "Slovníky")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToPractice,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(text = "Procvičit")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToMastery,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(text = "Mastery")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToHeatmap,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(text = "Kalendář (Heatmap)")
        }
    }
}