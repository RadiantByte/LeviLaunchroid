package org.levimc.launcher.core.minecraft

import android.content.res.AssetManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.mojang.minecraftpe.MainActivity
import org.conscrypt.Conscrypt
import java.security.Security


class MinecraftActivity : MainActivity() {

    private lateinit var gameManager: GamePackageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d(TAG, "Initializing game manager...")
            gameManager = GamePackageManager.getInstance(applicationContext)

            Log.d(TAG, "Setting up security provider...")
            try {
                Security.insertProviderAt(Conscrypt.newProvider(), 1)
            } catch (e: Exception) {
                Log.w(TAG, "Conscrypt init failed: ${e.message}")
            }

            Log.d(TAG, "Loading native libraries...")
            gameManager.loadAllLibraries()

            val modsEnabled = intent.getBooleanExtra("MODS_ENABLED", false)
            if (!modsEnabled) {
                Log.d(TAG, "Loading game core...")
                System.loadLibrary("preloader")

                val libPath = if (gameManager.getPackageContext().applicationInfo.splitPublicSourceDirs?.isNotEmpty() == true) {
                    "${applicationContext.cacheDir.path}/lib/${android.os.Build.CPU_ABI}/libminecraftpe.so"
                } else {
                    "${gameManager.getPackageContext().applicationInfo.nativeLibraryDir}/libminecraftpe.so"
                }
                nativeOnLauncherLoaded(libPath)
            }

            Log.i(TAG, "Game initialized successfully, calling super.onCreate()")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize game", e)
            Toast.makeText(
                this,
                "Failed to load game: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        super.onCreate(savedInstanceState)
    }

    override fun getAssets(): AssetManager {
        return if (::gameManager.isInitialized) {
            gameManager.getAssets()
        } else {
            super.getAssets()
        }
    }

    private external fun nativeOnLauncherLoaded(libPath: String)

    companion object {
        private const val TAG = "MinecraftActivity"
    }
}