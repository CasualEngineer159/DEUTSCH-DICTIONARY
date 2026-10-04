package com.example.deutschdictionarycvutfs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.deutschdictionarycvutfs.MasteryManager
import com.example.deutschdictionarycvutfs.TimeUtils
import java.util.Calendar

data class MonthData(
    val year: Int,
    val month: Int, // 0..11
    val name: String,
    val daysInMonth: Int,
    val firstDayOfWeekOffset: Int // 0 for Monday, 6 for Sunday
)

@Composable
fun HeatmapCalendarScreen(masteryManager: MasteryManager) {
    val masteryData by masteryManager.masteryDataFlow.collectAsState()
    val quota = 1500

    val currentEpochDay = TimeUtils.getCurrentLocalEpochDay()

    val months = remember {
        val list = mutableListOf<MonthData>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val monthNames = arrayOf(
            "Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
            "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec"
        )

        // Generate last 12 months (including current)
        cal.add(Calendar.MONTH, -11)
        for (i in 0..11) {
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2.. Saturday=7
            // We want Monday=0, Sunday=6
            var firstDay = cal.get(Calendar.DAY_OF_WEEK) - 2
            if (firstDay < 0) firstDay += 7

            list.add(
                MonthData(
                    year = year,
                    month = month,
                    name = "${monthNames[month]} $year",
                    daysInMonth = daysInMonth,
                    firstDayOfWeekOffset = firstDay
                )
            )
            cal.add(Calendar.MONTH, 1)
        }
        list // Keep oldest first, so current month is at the bottom
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (months.isNotEmpty()) months.size - 1 else 0
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Mastery Kalendář",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(months.size) { index ->
                val monthData = months[index]
                MonthView(
                    monthData = monthData, 
                    currentEpochDay = currentEpochDay, 
                    quota = quota, 
                    pointsMap = masteryData.dailyPointsGained
                )
            }
        }
    }
}

@Composable
fun MonthView(
    monthData: MonthData,
    currentEpochDay: Long,
    quota: Int,
    pointsMap: Map<Long, Int>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = monthData.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val totalCells = monthData.firstDayOfWeekOffset + monthData.daysInMonth
        val rows = (totalCells + 6) / 7

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Header for days (Po, Út...)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val days = listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")
                for (day in days) {
                    Text(
                        text = day, 
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Grid
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - monthData.firstDayOfWeekOffset + 1
                        
                        if (cellIndex >= monthData.firstDayOfWeekOffset && dayNumber <= monthData.daysInMonth) {
                            // Calculate epoch day for this specific date
                            val cal = Calendar.getInstance()
                            cal.set(monthData.year, monthData.month, dayNumber, 0, 0, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            val targetEpochDay = com.example.deutschdictionarycvutfs.TimeUtils.getLocalEpochDay(cal.timeInMillis)

                            val points = pointsMap[targetEpochDay] ?: 0

                            val bgColor = when {
                                points == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                points < quota / 2 -> Color(0xFFC8E6C9)
                                points < quota -> Color(0xFF81C784)
                                points < quota * 1.5 -> Color(0xFF4CAF50)
                                else -> Color(0xFF2E7D32)
                            }
                            
                            val textColor = when {
                                points >= quota / 2 -> Color.White
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            
                            val isToday = targetEpochDay == currentEpochDay

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isToday) MaterialTheme.colorScheme.primary else bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else textColor,
                                    fontWeight = if (isToday || points > 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        } else {
                            // Empty box to keep layout aligned
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp))
                        }
                    }
                }
            }
        }
    }
}
