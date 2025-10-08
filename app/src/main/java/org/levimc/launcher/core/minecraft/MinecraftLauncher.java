package org.levimc.launcher.core.minecraft;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import org.conscrypt.Conscrypt;
import org.levimc.launcher.core.mods.ModManager;
import org.levimc.launcher.core.mods.ModNativeLoader;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.settings.FeatureSettings;
import org.levimc.launcher.ui.dialogs.LoadingDialog;
import org.levimc.launcher.util.Logger;

import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Minecraft launcher using GamePackageManager approach
 */
public class MinecraftLauncher {
    private static final String TAG = "MinecraftLauncher";
    private final Context context;
    private GamePackageManager gameManager;

    public MinecraftLauncher(Context context, ClassLoader classLoader) {
        this.context = context;
    }

    public void launch(Intent sourceIntent, GameVersion version) {
        Activity activity = (Activity) context;

        try {
            if (version == null) {
                Logger.get().error("No version selected");
                return;
            }

            if (version.needsRepair) {
                activity.runOnUiThread(() ->
                        org.levimc.launcher.core.versions.VersionManager.attemptRepairLibs(activity, version)
                );
                return;
            }

            activity.runOnUiThread(this::showLoading);
            Log.d(TAG, "Initializing game manager...");
            gameManager = GamePackageManager.Companion.getInstance(context.getApplicationContext());
            setupSecurityProvider();
            Log.d(TAG, "Loading base native libraries...");
            loadBaseLibraries(version);
            boolean modsEnabled = sourceIntent.getBooleanExtra("MODS_ENABLED", false);
            if (!modsEnabled) {
                try {
                    Log.d(TAG, "Loading preloader library...");
                    System.loadLibrary("preloader");
                    Log.d(TAG, "Preloader library loaded");
                } catch (Exception e) {
                    Log.w(TAG, "Failed to load preloader: " + e.getMessage());
                }
            }
            Log.d(TAG, "Loading minecraftpe...");
            gameManager.loadLibrary("minecraftpe");
            if (!modsEnabled) {
                initializePreloader();
            }
            fillIntentWithMcPath(sourceIntent, version);
            launchMinecraftActivity(sourceIntent, version, modsEnabled);

        } catch (Exception e) {
            Logger.get().error("Launch failed: " + e.getMessage(), e);
            showLaunchErrorOnUi(e.getMessage());
        }
    }

    private void setupSecurityProvider() {
        Log.d(TAG, "Setting up security provider...");
        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1);
        } catch (Exception e) {
            Log.w(TAG, "Conscrypt init failed: " + e.getMessage());
        }
    }

    private void loadBaseLibraries(GameVersion version) {
        try {
            gameManager.loadLibrary("c++_shared");
            gameManager.loadLibrary("fmod");
            gameManager.loadLibrary("MediaDecoders_Android");
            gameManager.loadLibrary("pairipcore");
            if (shouldLoadMaesdk(version)) {
                gameManager.loadLibrary("maesdk");
            }

            Log.i(TAG, "Base native libraries loaded successfully");
        } catch (Exception e) {
            Logger.get().error("Error loading base libraries: " + e.getMessage());
            throw new RuntimeException("Failed to load base libraries", e);
        }
    }

    private void loadNativeLibraries(GameVersion version) {
        try {
            gameManager.loadLibrary("c++_shared");
            gameManager.loadLibrary("fmod");
            gameManager.loadLibrary("MediaDecoders_Android");

            if (shouldLoadMaesdk(version)) {
                gameManager.loadLibrary("maesdk");
            }

            gameManager.loadLibrary("minecraftpe");

            Log.i(TAG, "All native libraries loaded successfully");
        } catch (Exception e) {
            Logger.get().error("Error loading native libraries: " + e.getMessage());
            throw new RuntimeException("Failed to load native libraries", e);
        }
    }

    private void loadMods() {
        try {
            Log.d(TAG, "Loading .so mods (after minecraftpe)...");
            ModNativeLoader.loadEnabledSoMods(ModManager.getInstance(), context.getCacheDir());
            Log.d(TAG, ".so mods loaded successfully");
        } catch (Exception e) {
            Logger.get().error("Error loading so mods: " + e.getMessage());
        }
    }

    private boolean loadMods(Intent sourceIntent) {
        boolean modsEnabled = sourceIntent.getBooleanExtra("MODS_ENABLED", false);
        try {
            Log.d(TAG, "Loading .so mods (before minecraftpe)...");
            ModNativeLoader.loadEnabledSoMods(ModManager.getInstance(), context.getCacheDir());
            Log.d(TAG, ".so mods loaded successfully");
        } catch (Exception e) {
            Logger.get().error("Error loading so mods: " + e.getMessage());
        }

        if (!modsEnabled) {
            try {
                Log.d(TAG, "Loading preloader...");
                System.loadLibrary("preloader");
                Log.d(TAG, "Preloader library loaded");
            } catch (Exception e) {
                Log.w(TAG, "Failed to load preloader: " + e.getMessage());
            }
        }

        return modsEnabled;
    }

    private void initializePreloader() {
        try {
            Log.d(TAG, "Initializing preloader hooks...");
            String libPath = getMinecraftLibraryPath();
            nativeOnLauncherLoaded(libPath);
            Log.d(TAG, "Preloader initialized successfully");
        } catch (Exception e) {
            Log.w(TAG, "Failed to initialize preloader: " + e.getMessage());
        }
    }

    private String getMinecraftLibraryPath() {
        ApplicationInfo appInfo = gameManager.getPackageContext().getApplicationInfo();

        if (appInfo.splitPublicSourceDirs != null && appInfo.splitPublicSourceDirs.length > 0) {
            return context.getCacheDir().getPath() + "/lib/" + Build.CPU_ABI + "/libminecraftpe.so";
        } else {
            return appInfo.nativeLibraryDir + "/libminecraftpe.so";
        }
    }

    private void fillIntentWithMcPath(Intent sourceIntent, GameVersion version) {
        if (FeatureSettings.getInstance().isVersionIsolationEnabled()) {
            sourceIntent.putExtra("MC_PATH", version.versionDir.getAbsolutePath());
        } else {
            sourceIntent.putExtra("MC_PATH", "");
        }
    }

    private void launchMinecraftActivity(Intent sourceIntent, GameVersion version, boolean modsEnabled) {
        Activity activity = (Activity) context;

        new Thread(() -> {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    sourceIntent.putExtra("DISABLE_SPLASH_SCREEN", true);
                }
                sourceIntent.setClass(context, MinecraftActivity.class);
                ApplicationInfo mcInfo = gameManager.getPackageContext().getApplicationInfo();
                sourceIntent.putExtra("MC_SRC", mcInfo.sourceDir);

                if (mcInfo.splitSourceDirs != null) {
                    sourceIntent.putExtra("MC_SPLIT_SRC", new ArrayList<>(Arrays.asList(mcInfo.splitSourceDirs)));
                }
                sourceIntent.putExtra("MODS_ENABLED", modsEnabled);
                sourceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                activity.runOnUiThread(() -> {
                    activity.finish();
                    context.startActivity(sourceIntent);
                });

            } catch (Exception e) {
                Logger.get().error("Failed to launch Minecraft activity: " + e.getMessage(), e);
                activity.runOnUiThread(() ->
                        Toast.makeText(context, "Failed to launch: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private boolean shouldLoadMaesdk(GameVersion version) {
        if (version == null || version.versionCode == null) {
            return false;
        }

        String versionCode = version.versionCode;

        if (versionCode.contains("beta")) {
            return isVersionAtLeast(versionCode, "1.21.110.22");
        } else {
            return isVersionAtLeast(versionCode, "1.21.110");
        }
    }

    private boolean isVersionAtLeast(String currentVersion, String targetVersion) {
        try {
            String[] current = currentVersion.replaceAll("[^0-9.]", "").split("\\.");
            String[] target = targetVersion.split("\\.");

            int maxLength = Math.max(current.length, target.length);

            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < current.length ? Integer.parseInt(current[i]) : 0;
                int targetPart = i < target.length ? Integer.parseInt(target[i]) : 0;

                if (currentPart > targetPart) {
                    return true;
                } else if (currentPart < targetPart) {
                    return false;
                }
            }

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showLoading() {
        new LoadingDialog(context).show();
    }

    private void showLaunchErrorOnUi(String message) {
        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> Toast.makeText(
                activity, "Failed to launch Minecraft: " + message, Toast.LENGTH_LONG).show()
        );
    }

    private native void nativeOnLauncherLoaded(String libPath);
}