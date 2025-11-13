package com.example.lshookcamera

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast

class PickerActivity : Activity() {
    companion object {
        private const val TAG = "LSPicker"
        private const val PICK_CODE = 12345
    }

    private var origRequest: Int = -1
    private var extraOutput: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            origRequest = intent.getIntExtra("__ls_orig_request", -1)
            extraOutput = intent.getParcelableExtra("__ls_extra_output")

            val pick = Intent(Intent.ACTION_OPEN_DOCUMENT)
            pick.addCategory(Intent.CATEGORY_OPENABLE)
            pick.type = "image/*"
            pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            startActivityForResult(pick, PICK_CODE)
        } catch (t: Throwable) {
            Log.e(TAG, "start picker failed: $t")
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val uri = data.data
                if (uri != null) {
                    try {
                        if (extraOutput != null) {
                            val copied = copyUriToUri(contentResolver, uri, extraOutput!!)
                            runOnUiThread { finish() }
                        } else {
                            val bmp = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            val res = Intent()
                            res.putExtra("data", bmp)
                            setResult(Activity.RESULT_OK, res)
                            finish()
                        }
                    } catch (t: Throwable) {
                        Toast.makeText(this, "Ошибка обработки изображения: $t", Toast.LENGTH_LONG).show()
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                } else {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            } else {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun copyUriToUri(resolver: ContentResolver, src: Uri, dst: Uri): Boolean {
        return try {
            resolver.openInputStream(src)?.use { input ->
                resolver.openOutputStream(dst)?.use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "copy error: $t")
            false
        }
    }
}
