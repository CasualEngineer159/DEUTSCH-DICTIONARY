package com.example.deutschdictionarycvutfs.services

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.deutschdictionarycvutfs.MainActivity
import com.example.deutschdictionarycvutfs.MasteryManager
import com.example.deutschdictionarycvutfs.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DailyProgressService : Service() {

    private val CHANNEL_ID = "daily_progress_channel"
    private val NOTIFICATION_ID = 1
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private lateinit var masteryManager: MasteryManager

    override fun onCreate() {
        super.onCreate()
        masteryManager = MasteryManager.getInstance(this)
        createNotificationChannel()

        masteryManager.masteryDataFlow.onEach { data ->
            val currentEpochDay = TimeUtils.getCurrentLocalEpochDay()
            val pointsToday = data.dailyPointsGained[currentEpochDay] ?: 0
            val quota = 1500
            
            if (pointsToday >= quota) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                updateNotification(pointsToday, quota)
            }
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val currentEpochDay = TimeUtils.getCurrentLocalEpochDay()
        val pointsToday = masteryManager.getDailyPoints(currentEpochDay)
        val quota = 1500
        
        if (pointsToday < quota) {
            startForeground(NOTIFICATION_ID, createNotification(pointsToday, quota))
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Progress"
            val descriptionText = "Sleduje denní cíl Mastery bodů"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(points: Int, quota: Int): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("quick_start", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Denní cíl Deutsch Dictionary")
            .setContentText("Mastery body: $points / $quota")
            .setSmallIcon(R.drawable.ic_menu_agenda) // fallback icon
            .setProgress(quota, points, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Prevent swipe-away by instantly reviving the service notification if dismissed
        val deleteIntent = Intent(this, DailyProgressService::class.java).apply {
            action = "ACTION_DISMISSED"
        }
        val pendingDeleteIntent = PendingIntent.getService(
            this, 1, deleteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        notification.setDeleteIntent(pendingDeleteIntent)

        val builtNotification = notification.build()
        builtNotification.flags = builtNotification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
        
        return builtNotification
    }

    private fun updateNotification(points: Int, quota: Int) {
        startForeground(NOTIFICATION_ID, createNotification(points, quota))
    }
}
