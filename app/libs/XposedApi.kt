package de.robv.android.xposed

interface IXposedHookLoadPackage {
    fun handleLoadPackage(param: LoadPackageParam)
}

class LoadPackageParam(
    val packageName: String,
    val processName: String,
    val classLoader: ClassLoader
)
