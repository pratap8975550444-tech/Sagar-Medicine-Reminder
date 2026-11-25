
package com.sagar.medicinereminder

import android.app.*
import android.content.*
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val name = intent.getStringExtra("name") ?: "Medicine"

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val channelId = "medicine_reminder"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medicine Reminder",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("औषध घेण्याची वेळ झाली आहे!")
                .setContentText("आता औषध घ्या: $name")
                .setAutoCancel(true)
                .build()
        } else {
            Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Medicine Reminder")
                .setContentText("Time to take: $name")
                .setAutoCancel(true)
                .build()
        }

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
