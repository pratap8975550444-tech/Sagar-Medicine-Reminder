
package com.sagar.medicinereminder

import android.app.*
import android.content.*
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class MainActivity : Activity() {

    data class Medicine(
        val id: Int,
        val name: String,
        val hour: Int,
        val minute: Int
    )

    private val medicines = mutableListOf<Medicine>()

    private val prefs by lazy {
        getSharedPreferences("MedicineData", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadMedicines()
        requestNotificationPermission()
        showHome()
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100
            )
        }
    }

    private fun loadMedicines() {
        medicines.clear()
        val array = JSONArray(prefs.getString("list", "[]"))

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            medicines.add(
                Medicine(
                    item.getInt("id"),
                    item.getString("name"),
                    item.getInt("hour"),
                    item.getInt("minute")
                )
            )
        }
    }

    private fun saveMedicines() {
        val array = JSONArray()

        medicines.forEach {
            val item = JSONObject()
            item.put("id", it.id)
            item.put("name", it.name)
            item.put("hour", it.hour)
            item.put("minute", it.minute)
            array.put(item)
        }

        prefs.edit().putString("list", array.toString()).apply()
    }

    private fun showHome() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 30, 24, 24)
        }

        layout.addView(TextView(this).apply {
            text = "SAGAR MEDICINE REMINDER"
            textSize = 23f
            gravity = Gravity.CENTER
        })

        layout.addView(TextView(this).apply {
            text = "Your Daily Medicine Schedule"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 20)
        })

        medicines.forEach { medicine ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
            }

            row.addView(TextView(this).apply {
                text = "${medicine.name}\n${String.format("%02d:%02d", medicine.hour, medicine.minute)}"
                textSize = 18f
            })

            val buttons = LinearLayout(this)

            buttons.addView(Button(this).apply {
                text = "Edit"
                setOnClickListener { addMedicine(medicine) }
            })

            buttons.addView(Button(this).apply {
                text = "Delete"
                setOnClickListener {
                    cancelAlarm(medicine.id)
                    medicines.removeAll { it.id == medicine.id }
                    saveMedicines()
                    showHome()
                }
            })

            row.addView(buttons)
            layout.addView(row)
        }

        layout.addView(Button(this).apply {
            text = "+ Add Medicine"
            setOnClickListener { addMedicine() }
        })

        val scroll = ScrollView(this)
        scroll.addView(layout)
        setContentView(scroll)
    }

    private fun addMedicine(old: Medicine? = null) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 10, 20, 10)
        }

        val input = EditText(this).apply {
            hint = "Medicine Name"
            setText(old?.name ?: "")
        }

        val picker = TimePicker(this).apply {
            setIs24HourView(true)
            if (old != null) {
                hour = old.hour
                minute = old.minute
            }
        }

        box.addView(input)
        box.addView(picker)

        AlertDialog.Builder(this)
            .setTitle(if (old == null) "Add Medicine" else "Edit Medicine")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()

                if (name.isNotEmpty()) {
                    val id = old?.id
                        ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

                    val medicine = Medicine(
                        id, name, picker.hour, picker.minute
                    )

                    if (old != null) {
                        cancelAlarm(old.id)
                        val index = medicines.indexOfFirst {
                            it.id == old.id
                        }
                        if (index >= 0) medicines[index] = medicine
                    } else {
                        medicines.add(medicine)
                    }

                    saveMedicines()
                    scheduleAlarm(medicine)
                    showHome()
                } else {
                    Toast.makeText(
                        this, "Enter medicine name", Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun scheduleAlarm(medicine: Medicine) {
        val alarmManager =
            getSystemService(ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("name", medicine.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            medicine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, medicine.hour)
            set(Calendar.MINUTE, medicine.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelAlarm(id: Int) {
        val intent = Intent(this, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            getSystemService(ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)
    }
}
