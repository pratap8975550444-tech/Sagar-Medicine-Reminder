
package com.sagar.medicinereminder

import android.app.*
import android.content.*
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val channel = "medicine_reminder"

        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channel,
                    "Medicine Reminder",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val name = intent.getStringExtra("name") ?: "Medicine"

        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, channel)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Medicine Reminder")
                .setContentText("Time to take: $name")
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

        manager.notify(1, notification)
    }
}
