package com.example.deutschdictionarycvutfs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.deutschdictionarycvutfs.services.DailyProgressService
import com.example.deutschdictionarycvutfs.ui.navigation.AppNavigation
import com.example.deutschdictionarycvutfs.ui.theme.DeutschDictionaryCVUTFSTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startDailyProgressService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                startDailyProgressService()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startDailyProgressService()
        }

        // Handle the quick_start intent if opened from Notification
        var initialRoute = "main_menu"
        if (intent?.getBooleanExtra("quick_start", false) == true) {
            initialRoute = "quick_start" // We will intercept this in AppNavigation or pass it down
        }

        setContent {
            DeutschDictionaryCVUTFSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        modifier = Modifier.padding(innerPadding),
                        initialRoute = initialRoute
                    )
                }
            }
        }
    }

    private fun startDailyProgressService() {
        val intent = Intent(this, DailyProgressService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
