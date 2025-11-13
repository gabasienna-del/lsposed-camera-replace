// HookEntry.kt - основной Xposed hook (Kotlin)
package com.example.lshookcamera

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedBridge

class HookEntry : IXposedHookLoadPackage {
    companion object {
        private const val TAG = "LSHook"
        private const val PREFS = "ls_hook_prefs"
        private const val KEY_MODE = "mode"
        private const val KEY_PATH = "path"
        private const val DEFAULT_PATH = "/sdcard/LSMod/replace.jpg"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "android" || lpparam.packageName == BuildConfig.APPLICATION_ID) return

        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                lpparam.classLoader,
                "startActivityForResult",
                Intent::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val intent = param.args[0] as? Intent ?: return
                            val action = intent.action
                            if (action == MediaStore.ACTION_IMAGE_CAPTURE || isCameraIntent(intent)) {
                                val activity = param.thisObject as Activity
                                val requestCode = param.args[1] as Int
                                XposedBridge.log("[$TAG] Camera intent intercepted in ${lpparam.packageName}")

                                val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                                val mode = prefs.getString(KEY_MODE, "file") ?: "file"

                                if (mode == "file") {
                                    val path = prefs.getString(KEY_PATH, DEFAULT_PATH) ?: DEFAULT_PATH
                                    val outUri = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)
                                    if (outUri != null) {
                                        val bmp = loadBitmapFromPath(path) ?: run {
                                            activity.runOnUiThread { activity.onActivityResult(requestCode, Activity.RESULT_CANCELED, null) }
                                            param.result = null
                                            return
                                        }
                                        val ok = writeBitmapToUri(activity.contentResolver, outUri, bmp)
                                        activity.runOnUiThread { activity.onActivityResult(requestCode, if (ok) Activity.RESULT_OK else Activity.RESULT_CANCELED, null) }
                                    } else {
                                        val bmp = loadBitmapFromPath(path)
                                        if (bmp != null) {
                                            val res = Intent()
                                            res.putExtra("data", bmp)
                                            activity.runOnUiThread { activity.onActivityResult(requestCode, Activity.RESULT_OK, res) }
                                        } else {
                                            activity.runOnUiThread { activity.onActivityResult(requestCode, Activity.RESULT_CANCELED, null) }
                                        }
                                    }

                                } else {
                                    val act = param.thisObject as Activity
                                    val pickIntent = Intent(act, PickerActivity::class.java)
                                    pickIntent.putExtra("__ls_orig_request", requestCode)
                                    pickIntent.putExtra("__ls_extra_output", intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT))
                                    pickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    act.startActivity(pickIntent)
                                }

                                param.result = null
                            }
                        } catch (t: Throwable) {
                            XposedBridge.log("[$TAG] hook error: $t")
                        }
                    }
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                XposedHelpers.findAndHookMethod(
                    "android.app.Activity",
                    lpparam.classLoader,
                    "startActivityForResult",
                    Intent::class.java,
                    Int::class.javaPrimitiveType,
                    android.os.Bundle::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                val intent = param.args[0] as? Intent ?: return
                                val action = intent.action
                                if (action == MediaStore.ACTION_IMAGE_CAPTURE || isCameraIntent(intent)) {
                                    val activity = param.thisObject as Activity
                                    val requestCode = param.args[1] as Int
                                    XposedBridge.log("[$TAG] Camera intent intercepted (with bundle) in ${lpparam.packageName}")

                                    val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                                    val mode = prefs.getString(KEY_MODE, "file") ?: "file"

                                    if (mode == "file") {
                                        val path = prefs.getString(KEY_PATH, DEFAULT_PATH) ?: DEFAULT_PATH
                                        val outUri = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)
                                        if (outUri != null) {
                                            val bmp = loadBitmapFromPath(path)
                                            if (bmp != null) {
                                                val ok = writeBitmapToUri(activity.contentResolver, outUri, bmp)
                                                activity.runOnUiThread { activity.onActivityResult(requestCode, if (ok) Activity.RESULT_OK else Activity.RESULT_CANCELED, null) }
                                            } else {
                                                activity.runOnUiThread { activity.onActivityResult(requestCode, Activity.RESULT_CANCELED, null) }
                                            }
                                        } else {
                                            val bmp = loadBitmapFromPath(path)
                                            if (bmp != null) {
                                                val res = Intent()
                                                res.putExtra("data", bmp)
                                                activity.runOnUiThread { activity.onActivityResult(requestCode, Activity.RESULT_OK, res) }
                                            } else {
                                                activity.runOnUiThread { activity.onActivityResult(requestCode, Activity.RESULT_CANCELED, null) }
                                            }
                                        }
                                    } else {
                                        val act = param.thisObject as Activity
                                        val pickIntent = Intent(act, PickerActivity::class.java)
                                        pickIntent.putExtra("__ls_orig_request", requestCode)
                                        pickIntent.putExtra("__ls_extra_output", intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT))
                                        pickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        act.startActivity(pickIntent)
                                    }

                                    param.result = null
                                }
                            } catch (t: Throwable) {
                                XposedBridge.log("[$TAG] hook error: $t")
                            }
                        }
                    }
                )
            }

        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] top hook error: $t")
        }
    }

    private fun isCameraIntent(intent: Intent): Boolean {
        intent.component?.let {
            val pkg = it.packageName?.lowercase()
            if (pkg != null && (pkg.contains("camera") || pkg.contains("com.sec.android.app.camera"))) return true
        }
        intent.extras?.let { extras ->
            if (extras.containsKey(MediaStore.EXTRA_OUTPUT)) return true
        }
        return false
    }

    private fun loadBitmapFromPath(path: String): android.graphics.Bitmap? {
        return try {
            android.graphics.BitmapFactory.decodeFile(path)
        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] loadBitmap error: $t")
            null
        }
    }

    private fun writeBitmapToUri(resolver: ContentResolver, uri: Uri?, bmp: android.graphics.Bitmap?): Boolean {
        if (uri == null || bmp == null) return false
        return try {
            resolver.openOutputStream(uri)?.use { os ->
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, os)
                os.flush()
            }
            true
        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] writeBitmapToUri error: $t")
            false
        }
    }
}
