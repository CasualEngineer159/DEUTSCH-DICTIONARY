package com.example.deutschdictionarycvutfs.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.deutschdictionarycvutfs.DictionaryManager
import com.example.deutschdictionarycvutfs.MasteryManager
import com.example.deutschdictionarycvutfs.DomainQuestionType
import com.example.deutschdictionarycvutfs.ui.models.PracticeConfig
import com.example.deutschdictionarycvutfs.ui.models.SessionResult
import com.example.deutschdictionarycvutfs.ui.screens.HeatmapCalendarScreen
import com.example.deutschdictionarycvutfs.ui.screens.MainMenuScreen
import com.example.deutschdictionarycvutfs.ui.screens.dictionary.DictionaryDetailScreen
import com.example.deutschdictionarycvutfs.ui.screens.dictionary.DictionaryListScreen
import com.example.deutschdictionarycvutfs.ui.screens.mastery.MasteryDetailScreen
import com.example.deutschdictionarycvutfs.ui.screens.mastery.MasteryScreen
import com.example.deutschdictionarycvutfs.ui.screens.practice.PracticeResultScreen
import com.example.deutschdictionarycvutfs.ui.screens.practice.PracticeSessionScreen
import com.example.deutschdictionarycvutfs.ui.screens.practice.PracticeSetupScreen

@Composable
fun AppNavigation(modifier: Modifier = Modifier, initialRoute: String = "main_menu") {
    val navController = rememberNavController()
    val context = LocalContext.current
    val dictionaryManager = remember { DictionaryManager(context) }
    val masteryManager = remember { MasteryManager.getInstance(context) }

    var practiceConfig by remember { 
        mutableStateOf<PracticeConfig?>(null) 
    }
    
    LaunchedEffect(initialRoute) {
        if (initialRoute == "quick_start" && practiceConfig == null) {
            val allLessons = dictionaryManager.getAllLessons()
                .filter { !it.second.lessonName.contains("test", ignoreCase = true) }
                .map { it.first }
            practiceConfig = PracticeConfig(
                selectedLessons = allLessons,
                allowedFormats = DomainQuestionType.entries.toSet(),
                wordCount = -1
            )
        }
    }
    var sessionResult by remember { mutableStateOf<SessionResult?>(null) }

    NavHost(navController = navController, startDestination = if (initialRoute == "quick_start") "practice_session" else "main_menu", modifier = modifier) {
        composable("main_menu") {
            MainMenuScreen(
                masteryManager = masteryManager,
                onNavigateToDictionaries = { navController.navigate("dictionary_list") },
                onNavigateToPractice = { navController.navigate("practice_setup") },
                onNavigateToMastery = { navController.navigate("mastery_screen") },
                onNavigateToHeatmap = { navController.navigate("heatmap_screen") },
                onQuickStart = {
                    val allLessons = dictionaryManager.getAllLessons()
                        .filter { !it.second.lessonName.contains("test", ignoreCase = true) }
                        .map { it.first }
                    practiceConfig = PracticeConfig(
                        selectedLessons = allLessons,
                        allowedFormats = DomainQuestionType.entries.toSet(),
                        wordCount = -1
                    )
                    navController.navigate("practice_session")
                }
            )
        }
        composable("dictionary_list") {
            DictionaryListScreen(
                dictionaryManager = dictionaryManager,
                onLessonSelected = { fileName ->
                    navController.navigate("dictionary_detail/$fileName")
                }
            )
        }
        composable("dictionary_detail/{fileName}") { backStackEntry ->
            val fileName = backStackEntry.arguments?.getString("fileName")
            if (fileName != null) {
                DictionaryDetailScreen(fileName = fileName, dictionaryManager = dictionaryManager)
            }
        }
        composable("practice_setup") {
            PracticeSetupScreen(
                dictionaryManager = dictionaryManager,
                onStartPractice = { config ->
                    practiceConfig = config
                    navController.navigate("practice_session")
                }
            )
        }
        composable("practice_session") {
            if (practiceConfig != null) {
                PracticeSessionScreen(
                    config = practiceConfig!!,
                    dictionaryManager = dictionaryManager,
                    masteryManager = masteryManager,
                    onFinish = { result ->
                        sessionResult = result
                        navController.navigate("practice_result") {
                            popUpTo("practice_setup") { inclusive = false }
                        }
                    }
                )
            }
        }
        composable("practice_result") {
            if (sessionResult != null) {
                PracticeResultScreen(
                    result = sessionResult!!,
                    onNavigateHome = {
                        navController.navigate("main_menu") {
                            popUpTo(0)
                        }
                    }
                )
            }
        }
        composable("mastery_screen") {
            MasteryScreen(
                dictionaryManager = dictionaryManager, 
                masteryManager = masteryManager,
                onLessonSelected = { fileName ->
                    navController.navigate("mastery_detail/$fileName")
                }
            )
        }
        composable("mastery_detail/{fileName}") { backStackEntry ->
            val fileName = backStackEntry.arguments?.getString("fileName")
            if (fileName != null) {
                MasteryDetailScreen(
                    fileName = fileName,
                    dictionaryManager = dictionaryManager,
                    masteryManager = masteryManager
                )
            }
        }
        composable("heatmap_screen") {
            HeatmapCalendarScreen(masteryManager = masteryManager)
        }
    }
}