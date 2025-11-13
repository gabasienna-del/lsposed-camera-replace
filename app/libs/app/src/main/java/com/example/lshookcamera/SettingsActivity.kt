package com.example.lshookcamera

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    companion object {
        private const val KEY_MODE = "mode"
        private const val KEY_PATH = "path"
        private const val PREFS = "ls_hook_prefs"
        private const val PICK_LOCAL = 2001
    }

    private lateinit var rbFile: RadioButton
    private lateinit var rbPicker: RadioButton
    private lateinit var tvPath: TextView
    private lateinit var btnChoose: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        rbFile = findViewById(R.id.rb_file)
        rbPicker = findViewById(R.id.rb_picker)
        tvPath = findViewById(R.id.tv_path)
        btnChoose = findViewById(R.id.btn_choose)

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val mode = prefs.getString(KEY_MODE, "file")
        val path = prefs.getString(KEY_PATH, "/sdcard/LSMod/replace.jpg")

        rbFile.isChecked = (mode == "file")
        rbPicker.isChecked = (mode == "picker")
        tvPath.text = path

        rbFile.setOnClickListener {
            prefs.edit().putString(KEY_MODE, "file").apply()
            Toast.makeText(this, "Режим: локальный файл", Toast.LENGTH_SHORT).show()
        }
        rbPicker.setOnClickListener {
            prefs.edit().putString(KEY_MODE, "picker").apply()
            Toast.makeText(this, "Режим: открывать галерею", Toast.LENGTH_SHORT).show()
        }

        btnChoose.setOnClickListener {
            val pick = Intent(Intent.ACTION_OPEN_DOCUMENT)
            pick.addCategory(Intent.CATEGORY_OPENABLE)
            pick.type = "image/*"
            startActivityForResult(pick, PICK_LOCAL)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_LOCAL && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data ?: return
            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            prefs.edit().putString(KEY_PATH, uri.toString()).apply()
            tvPath.text = uri.toString()
            Toast.makeText(this, "Выбран файл: ${uri}", Toast.LENGTH_SHORT).show()
        }
    }
}
