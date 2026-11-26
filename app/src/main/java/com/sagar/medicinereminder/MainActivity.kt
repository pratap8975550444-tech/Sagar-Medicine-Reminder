
package com.sagar.medicinereminder

import android.app.*
import android.os.Bundle
import android.content.*
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class MainActivity : Activity() {

    data class DoseTime(
        val hour: Int,
        val minute: Int,
        val period: String,
        val enabled: Boolean = true
    )

    data class Medicine(
        val id: Int,
        val name: String,
        var dose: String = "",
        val times: MutableList<DoseTime> = mutableListOf()
    )

    private val medicines = mutableListOf<Medicine>()

    private val prefs by lazy {
        getSharedPreferences("MedicineDataFreshV2", MODE_PRIVATE)
    }

    private val medicineNames = listOf(
        "PANGRAF",
        "MYLEPT",
        "OMNACORTIL",
        "VALGAN",
        "BACTRIM DS",
        "PAN",
        "SUPRACAL",
        "ECOSPRIN",
        "BECOSULE",
        "PACIMOL",
        "CETOLAC",
        "CLOUEN LOZENGES",
        "CANDID POWDER"
    )

    private val periods = listOf(
        "सकाळ",
        "दुपार",
        "संध्याकाळ",
        "रात्र"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()
        loadMedicines()

        medicines.forEach { scheduleAllAlarms(it) }

        showHome()
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    private fun loadMedicines() {
        medicines.clear()

        val array = JSONArray(prefs.getString("list", "[]"))

        if (array.length() == 0) {
            medicineNames.forEachIndexed { index, name ->
                medicines.add(Medicine(index + 1, name))
            }
            saveMedicines()
            return
        }

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val times = mutableListOf<DoseTime>()
            val timeArray = item.optJSONArray("times") ?: JSONArray()

            for (j in 0 until timeArray.length()) {
                val t = timeArray.getJSONObject(j)

                times.add(
                    DoseTime(
                        t.getInt("hour"),
                        t.getInt("minute"),
                        t.optString("period", "सकाळ"),
                        t.optBoolean("enabled", true)
                    )
                )
            }

            medicines.add(
                Medicine(
                    item.getInt("id"),
                    item.getString("name"),
                    item.optString("dose", ""),
                    times
                )
            )
        }
    }

    private fun saveMedicines() {
        val array = JSONArray()

        medicines.forEach { medicine ->
            val item = JSONObject()

            item.put("id", medicine.id)
            item.put("name", medicine.name)
            item.put("dose", medicine.dose)

            val times = JSONArray()

            medicine.times.forEach { t ->
                val obj = JSONObject()

                obj.put("hour", t.hour)
                obj.put("minute", t.minute)
                obj.put("period", t.period)
                obj.put("enabled", t.enabled)

                times.put(obj)
            }

            item.put("times", times)
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
            setPadding(0, 10, 0, 10)
        })

        layout.addView(TextView(this).apply {
            text = "YOUR DAILY MEDICINE SCHEDULE"
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        })

        medicines.forEach { medicine ->

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundColor(0xFFF1F5F9.toInt())
            }

            card.addView(TextView(this).apply {
                text = medicine.name
                textSize = 20f
                setTextColor(0xFF166534.toInt())
            })

            card.addView(TextView(this).apply {
                text = "DOSE: ${medicine.dose.ifBlank { "Not Set" }}"
                textSize = 15f
            })

            if (medicine.times.isEmpty()) {
                card.addView(TextView(this).apply {
                    text = "TIME: Not Set"
                    textSize = 14f
                })
            }

            medicine.times.forEach { t ->
                card.addView(TextView(this).apply {
                    text = "${t.period} - ${
                        String.format("%02d:%02d", t.hour, t.minute)
                    }   Alarm: ${if (t.enabled) "ON" else "OFF"}"
                    textSize = 14f
                })
            }

            val buttons = LinearLayout(this)

            buttons.addView(Button(this).apply {
                text = "EDIT"
                setOnClickListener {
                    editMedicine(medicine)
                }
            })

            buttons.addView(Button(this).apply {
                text = "DELETE"
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Delete Medicine")
                        .setMessage("Delete ${medicine.name}?")
                        .setPositiveButton("YES") { _, _ ->
                            cancelAllAlarms(medicine.id)
                            medicines.remove(medicine)
                            saveMedicines()
                            showHome()
                        }
                        .setNegativeButton("NO", null)
                        .show()
                }
            })

            card.addView(buttons)

            val space = TextView(this).apply {
                text = ""
                height = 15
            }

            layout.addView(card)
            layout.addView(space)
        }

        layout.addView(Button(this).apply {
            text = "+ ADD MEDICINE"
            setOnClickListener {
                addNewMedicine()
            }
        })

        val scroll = ScrollView(this)
        scroll.addView(layout)

        setContentView(scroll)
    }

    private fun editMedicine(medicine: Medicine) {

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 10, 20, 10)
        }

        val doseInput = EditText(this).apply {
            hint = "Enter Dose (Example: 4 mg)"
            setText(medicine.dose)
        }

        val timesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun addTimeRow(existing: DoseTime? = null) {

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(5, 12, 5, 12)
            }

            val period = Spinner(this)

            period.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                periods
            )

            val picker = TimePicker(this).apply {
                setIs24HourView(false)

                existing?.let {
                    hour = it.hour
                    minute = it.minute
                }
            }

            val alarm = CheckBox(this).apply {
                text = "ALARM ON"
                isChecked = existing?.enabled ?: true
            }

            val remove = Button(this).apply {
                text = "REMOVE TIME"
                setOnClickListener {
                    timesLayout.removeView(row)
                }
            }

            existing?.let {
                period.setSelection(
                    periods.indexOf(it.period).coerceAtLeast(0)
                )
            }

            row.addView(TextView(this).apply {
                text = "Select Time"
                textSize = 16f
            })

            row.addView(period)
            row.addView(picker)
            row.addView(alarm)
            row.addView(remove)

            row.tag = Triple(period, picker, alarm)

            timesLayout.addView(row)
        }

        medicine.times.forEach {
            addTimeRow(it)
        }

        box.addView(TextView(this).apply {
            text = "MEDICINE: ${medicine.name}"
            textSize = 18f
        })

        box.addView(TextView(this).apply {
            text = "DOSE"
        })

        box.addView(doseInput)

        box.addView(timesLayout)

        box.addView(Button(this).apply {
            text = "+ ADD TIME"
            setOnClickListener {
                addTimeRow()
            }
        })

        val scroll = ScrollView(this)
        scroll.addView(box)

        AlertDialog.Builder(this)
            .setTitle("EDIT MEDICINE")
            .setView(scroll)
            .setPositiveButton("SAVE") { _, _ ->

                medicine.dose = doseInput.text.toString().trim()

                medicine.times.clear()

                for (i in 0 until timesLayout.childCount) {

                    val row = timesLayout.getChildAt(i)

                    val controls =
                        row.tag as? Triple<*, *, *> ?: continue

                    val period = controls.first as Spinner
                    val picker = controls.second as TimePicker
                    val alarm = controls.third as CheckBox

                    medicine.times.add(
                        DoseTime(
                            picker.hour,
                            picker.minute,
                            period.selectedItem.toString(),
                            alarm.isChecked
                        )
                    )
                }

                saveMedicines()
                scheduleAllAlarms(medicine)
                showHome()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun addNewMedicine() {

        val input = EditText(this).apply {
            hint = "Medicine Name"
        }

        AlertDialog.Builder(this)
            .setTitle("ADD MEDICINE")
            .setView(input)
            .setPositiveButton("ADD") { _, _ ->

                val name = input.text.toString().trim()

                if (name.isNotEmpty()) {

                    val id = (System.currentTimeMillis() %
                        Int.MAX_VALUE).toInt()

                    medicines.add(
                        Medicine(id, name)
                    )

                    saveMedicines()
                    showHome()
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun scheduleAllAlarms(medicine: Medicine) {

        cancelAllAlarms(medicine.id)

        medicine.times.forEachIndexed { index, time ->

            if (time.enabled) {
                scheduleMedicineAlarm(medicine, time, index)
            }
        }
    }

    private fun scheduleMedicineAlarm(
        medicine: Medicine,
        time: DoseTime,
        index: Int
    ) {

        val alarmManager =
            getSystemService(ALARM_SERVICE) as AlarmManager

        val requestCode = medicine.id * 31 + index

        val intent = Intent(
            this,
            ReminderReceiver::class.java
        ).apply {
            putExtra("name", medicine.name)
            putExtra("dose", medicine.dose)
        }

        val pending = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {

            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
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
            pending
        )
    }

    private fun cancelAllAlarms(id: Int) {

        val alarmManager =
            getSystemService(ALARM_SERVICE) as AlarmManager

        for (index in 0 until 50) {

            val requestCode = id * 31 + index

            val intent = Intent(
                this,
                ReminderReceiver::class.java
            )

            val pending = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or
                    PendingIntent.FLAG_IMMUTABLE
            )

            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
    }
}
