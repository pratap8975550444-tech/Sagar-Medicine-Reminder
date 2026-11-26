
package com.sagar.medicinereminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences(
            "MedicineDataFreshV2",
            Context.MODE_PRIVATE
        )

        val json = prefs.getString("list", "[]") ?: "[]"
        val medicines = JSONArray(json)

        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        for (i in 0 until medicines.length()) {
            val medicine = medicines.getJSONObject(i)
            val medicineId = medicine.optInt("id")
            val name = medicine.optString("name", "Medicine")
            val times = medicine.optJSONArray("times") ?: continue

            for (j in 0 until times.length()) {
                val time = times.getJSONObject(j)

                if (!time.optBoolean("enabled", true)) continue

                val hour = time.optInt("hour")
                val minute = time.optInt("minute")

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                val requestCode =
                    ((medicineId.toLong() * 31 + j) and 0x7fffffff).toInt()

                val reminderIntent = Intent(
                    context,
                    ReminderReceiver::class.java
                ).apply {
                    putExtra("name", name)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    reminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        }
    }
}
