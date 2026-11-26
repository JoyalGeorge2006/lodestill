package dev.joyal.lodestill

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class LodestillApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(
            "session", "Focus session", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Running focus timer" })
        nm.createNotificationChannel(NotificationChannel(
            "alerts", "Session alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Timer done and interruption alerts" })
    }
}
