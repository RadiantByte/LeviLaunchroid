package org.levimc.launcher.core.minecraft

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

/**
 * Unified manager for Minecraft package resolution, library loading, and asset management
 */
class GamePackageManager private constructor(private val context: Context) {

    private val packageContext: Context
    private val assetManager: AssetManager
    private val nativeLibDir: String

    private val knownPackages = arrayOf(
        "com.mojang.minecraftpe",
        "com.mojang.minecraftpe.beta",
        "com.mojang.minecraftpe.preview"
    )

    private val requiredLibs = arrayOf(
        "libc++_shared.so",
        "libfmod.so",
        "libMediaDecoders_Android.so",
        "libpairipcore.so",
        "libmaesdk.so",
        "libminecraftpe.so"
    )

    init {
        val packageName = detectGamePackage()
            ?: throw IllegalStateException("Minecraft not found")

        packageContext = context.createPackageContext(
            packageName,
            Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
        )

        nativeLibDir = resolveNativeLibDir()
        extractLibraries()

        assetManager = createAssetManager()
        setupSecurityProvider()
    }

    private fun detectGamePackage(): String? {
        return knownPackages.firstOrNull { isPackageInstalled(it) }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun resolveNativeLibDir(): String {
        val appInfo = packageContext.applicationInfo
        return if (appInfo.splitPublicSourceDirs?.isNotEmpty() == true) {
            val cacheLibDir = File(context.cacheDir, "lib/${getDeviceAbi()}")
            cacheLibDir.mkdirs()
            cacheLibDir.absolutePath
        } else {
            appInfo.nativeLibraryDir
        }
    }

    private fun getDeviceAbi(): String {
        return Build.SUPPORTED_64_BIT_ABIS.firstOrNull {
            it.contains("arm64-v8a") || it.contains("x86_64")
        } ?: Build.SUPPORTED_32_BIT_ABIS.firstOrNull {
            it.contains("armeabi-v7a") || it.contains("x86")
        } ?: Build.CPU_ABI
    }

    private fun extractLibraries() {
        val appInfo = packageContext.applicationInfo
        val outputDir = File(nativeLibDir)

        if (File(appInfo.nativeLibraryDir).exists()) {
            copyFromNativeDir(appInfo.nativeLibraryDir, outputDir)
        }

        val apkPaths = mutableListOf<String>()
        appInfo.sourceDir?.let { apkPaths.add(it) }
        appInfo.splitPublicSourceDirs?.let { apkPaths.addAll(it) }

        apkPaths.forEach { extractFromApk(it, outputDir) }

        verifyLibraries(outputDir)
    }

    private fun copyFromNativeDir(sourceDir: String, destDir: File) {
        val source = File(sourceDir)
        if (!source.exists()) return

        requiredLibs.forEach { lib ->
            val srcFile = File(source, lib)
            val dstFile = File(destDir, lib)

            if (srcFile.exists() && srcFile.length() > 0) {
                try {
                    srcFile.copyTo(dstFile, overwrite = true)
                    dstFile.setReadable(true)
                    dstFile.setExecutable(true)
                    logFileOperation("Copied", lib)
                } catch (e: Exception) {
                    logFileOperation("Failed to copy", lib)
                }
            }
        }
    }

    private fun extractFromApk(apkPath: String, outputDir: File) {
        if (!apkPath.contains("arm") && !apkPath.contains("x86") &&
            !apkPath.contains("base.apk")) return

        try {
            ZipFile(apkPath).use { zip ->
                val abi = "lib/${getDeviceAbi()}"

                requiredLibs.forEach { lib ->
                    val entry = zip.getEntry("$abi/$lib") ?: return@forEach
                    val output = File(outputDir, lib)

                    if (output.exists() && output.length() > 0) return@forEach

                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(output).use { out ->
                            input.copyTo(out)
                        }
                    }

                    output.setReadable(true)
                    output.setExecutable(true)
                    logFileOperation("Extracted", lib, "from ${File(apkPath).name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract from $apkPath: ${e.message}")
        }
    }

    private fun verifyLibraries(dir: File) {
        val missing = requiredLibs.filterNot {
            File(dir, it).let { f -> f.exists() && f.length() > 0 }
        }

        if (missing.isNotEmpty()) {
            Log.w(TAG, "Missing libraries: ${missing.joinToString()}")
        } else {
            Log.i(TAG, "All libraries verified")
        }
    }

    private fun logFileOperation(action: String, lib: String, extra: String? = null, e: Exception? = null) {
        val message = buildString {
            append("$action $lib")
            if (extra != null) append(" $extra")
            if (e != null) append(": ${e.message}")
        }
        if (e != null) Log.w(TAG, message) else Log.d(TAG, message)
    }

    private fun createAssetManager(): AssetManager {
        val assets = AssetManager::class.java.newInstance()
        val addAssetPathMethod = AssetManager::class.java.getMethod(
            "addAssetPath",
            String::class.java
        )

        val paths = mutableListOf<String>().apply {
            add(packageContext.packageResourcePath)
            val splitPath = packageContext.packageResourcePath.replace("base.apk", "split_install_pack.apk")
            if (File(splitPath).exists()) add(splitPath)
            add(context.packageResourcePath)
        }

        paths.forEach { path ->
            addAssetPathMethod.invoke(assets, path)
        }

        return assets
    }

    private fun setupSecurityProvider() {
        Log.d(TAG, "Setting up security provider...")
        try {
            java.security.Security.insertProviderAt(org.conscrypt.Conscrypt.newProvider(), 1)
        } catch (e: Exception) {
            Log.w(TAG, "Conscrypt init failed: ${e.message}")
        }
    }

    /**
     * Load a specific library by name
     */
    fun loadLibrary(name: String): Boolean {
        val libFile = File(nativeLibDir, if (name.startsWith("lib")) name else "lib$name.so")

        return try {
            if (libFile.exists()) {
                System.load(libFile.absolutePath)
                Log.d(TAG, "Loaded $name")
                true
            } else {
                System.loadLibrary(name.removePrefix("lib").removeSuffix(".so"))
                Log.d(TAG, "Loaded $name as system library")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $name: ${e.message}")
            false
        }
    }

    /**
     * Load all required libraries in order
     */
    fun loadAllLibraries() {
        requiredLibs.forEach { lib ->
            val libName = lib.removePrefix("lib").removeSuffix(".so")
            loadLibrary(libName)
        }
    }

    /**
     * Get the asset manager with game and launcher assets
     */
    fun getAssets(): AssetManager = assetManager

    /**
     * Get the game package context
     */
    fun getPackageContext(): Context = packageContext

    /**
     * Get version information
     */
    fun getVersionName(): String? {
        return try {
            context.packageManager.getPackageInfo(
                packageContext.packageName,
                0
            ).versionName
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "GamePackageManager"

        @Volatile
        private var instance: GamePackageManager? = null

        fun getInstance(context: Context): GamePackageManager {
            return instance ?: synchronized(this) {
                instance ?: GamePackageManager(context.applicationContext).also {
                    instance = it
                }
            }
        }

        fun isInitialized() = instance != null
    }
}
