package org.levimc.launcher.core.minecraft

import android.content.res.AssetManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.mojang.minecraftpe.MainActivity
import org.levimc.launcher.core.mods.ModManager
import org.levimc.launcher.core.mods.ModNativeLoader

class MinecraftActivity : MainActivity() {

    private lateinit var gameManager: GamePackageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            gameManager = GamePackageManager.getInstance(applicationContext)
            try {
                System.loadLibrary("preloader")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load preloader: ${e.message}")
            }
        } catch (e: Exception) {
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

    companion object {
        private const val TAG = "MinecraftActivity"
    }
}