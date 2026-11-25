
package com.sagar.medicinereminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val medicineName =
            intent.getStringExtra("name") ?: "Medicine"

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        val channelId = "sagar_medicine_reminder"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sagar Medicine Reminder",
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationManager.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("औषध घेण्याची वेळ झाली आहे!")
                .setContentText("तुमचे औषध: $medicineName")
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .build()

        } else {

            Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Medicine Reminder")
                .setContentText("Time to take: $medicineName")
                .setAutoCancel(true)
                .build()
        }

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}
