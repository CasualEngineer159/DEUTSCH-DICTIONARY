package com.example.deutschdictionarycvutfs.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainMenuScreen(
    onNavigateToDictionaries: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToMastery: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
    }
}