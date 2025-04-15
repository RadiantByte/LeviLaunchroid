package org.levimc.launcher.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.levimc.launcher.R;
import org.levimc.launcher.mods.ModManager;

public class PermissionsHandler {

    public static final int REQUEST_MANAGE_STORAGE = 1001;
    public static final int REQUEST_STORAGE_PERMISSION  = 1003;
    private final Activity activity;
    private final ModManager modManager;

    public PermissionsHandler(Activity activity, ModManager modManager) {
        this.activity = activity;
        this.modManager = modManager;
    }

    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(activity.getString(R.string.storage_permission_title))
                    .setMessage(activity.getString(R.string.storage_permission_message))
                    .setPositiveButton(activity.getString(R.string.grant_permission), (d, w) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
                    })
                    .setNegativeButton(activity.getString(R.string.cancel), (d, w) -> {
                        activity.finish();
                    })
                    .create();

            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(activity, R.color.on_surface));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(activity, R.color.on_surface));

        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        }
    }

    public void handlePermissionResult(int requestCode,String[] permissions, int[] grantResults) {

        if (requestCode == REQUEST_MANAGE_STORAGE && hasStoragePermission()) {
            modManager.refreshMods();
        } else {
            requestStoragePermission();
        }

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                modManager.refreshMods();
            }else{

            }
    }
    }
}