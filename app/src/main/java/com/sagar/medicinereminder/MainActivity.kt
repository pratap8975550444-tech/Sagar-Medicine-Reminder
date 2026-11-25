package com.sagar.medicinereminder

import android.app.*
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private val medicines = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val layout = LinearLayout(this).apply {
            orientation = 1
            setPadding(24, 30, 24, 24)
        }

        val title = TextView(this).apply {
            text = "SAGAR MEDICINE REMINDER"
            textSize = 22f
            gravity = Gravity.CENTER
        }
        layout.addView(title)

        medicines.forEach {
            layout.addView(TextView(this).apply {
                text = it
                textSize = 18f
                setPadding(10, 15, 10, 15)
            })
        }

        layout.addView(Button(this).apply {
            text = "+ Add Medicine"
            setOnClickListener { addMedicine() }
        })

        setContentView(layout)
    }

    private fun addMedicine() {
        val input = EditText(this)
        input.hint = "Medicine Name"

        AlertDialog.Builder(this)
            .setTitle("Add Medicine")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    medicines.add(name)
                    showHome()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
