package org.levimc.launcher.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import org.levimc.launcher.R;
import org.levimc.launcher.core.minecraft.MinecraftLauncher;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;

import java.util.concurrent.ExecutorService;

public class ResourcepackHandler {

    private final Activity activity;
    private final MinecraftLauncher minecraftLauncher;
    private final ExecutorService executor;
    private final ProgressBar progressLoader;
    private final Button launchBtn;

    public ResourcepackHandler(Activity activity, MinecraftLauncher minecraftLauncher,
                               ExecutorService executor, ProgressBar progressLoader, Button launchBtn) {
        this.activity = activity;
        this.minecraftLauncher = minecraftLauncher;
        this.executor = executor;
        this.progressLoader = progressLoader;
        this.launchBtn = launchBtn;
    }

    public void checkIntentForResourcepack() {
        Intent intent = activity.getIntent();
        Uri data = intent.getData();
        if (data != null) {
            String path = data.getPath();
            if (path != null && isMinecraftResourceFile(path)) {
                AlertDialog dialog = new AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.resourcepack_detected_title))
                        .setMessage(activity.getString(R.string.resourcepack_detected_message, path))
                        .setPositiveButton(activity.getString(R.string.launch_now), (d, which) -> launchMinecraft(intent))
                        .setNegativeButton(activity.getString(R.string.launch_later), null)
                        .create();
                dialog.setOnShowListener(dialogInterface -> {
                    Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                    positive.setTextColor(ContextCompat.getColor(activity, R.color.on_surface));
                    negative.setTextColor(ContextCompat.getColor(activity, R.color.on_surface));
                });
                dialog.show();
            }
        }
    }

    private boolean isMinecraftResourceFile(String path) {
        String lowerPath = path.toLowerCase();
        return lowerPath.endsWith(".mcworld") ||
                lowerPath.endsWith(".mcpack") ||
                lowerPath.endsWith(".mcaddon") ||
                lowerPath.endsWith(".mctemplate");
    }
    private void launchMinecraft(Intent intent) {
        launchBtn.setEnabled(false);
        progressLoader.setVisibility(android.view.View.VISIBLE);
        executor.execute(() -> {
            VersionManager versionManager = VersionManager.get(activity);
            GameVersion currentVersion = versionManager.getSelectedVersion();

            minecraftLauncher.launch(intent, currentVersion);
            activity.runOnUiThread(() -> {
                progressLoader.setVisibility(android.view.View.GONE);
                launchBtn.setEnabled(true);
            });
        });
    }
}