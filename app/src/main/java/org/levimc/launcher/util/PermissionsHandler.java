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
import org.levimc.launcher.core.mods.ModManager;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;

public class PermissionsHandler {

    public static final int REQUEST_STORAGE      = 1001;
    public static final int REQUEST_OVERLAY      = 1002;

    public interface PermissionResultCallback {
        void onPermissionGranted(PermissionType type);
        void onPermissionDenied(PermissionType type, boolean permanentlyDenied);
    }

    public enum PermissionType {
        STORAGE, OVERLAY
    }

    private final Activity activity;
    private PermissionResultCallback callback;

    public PermissionsHandler(Activity activity) {
        this.activity = activity;
    }

    public boolean hasPermission(PermissionType type) {
        switch (type) {
            case STORAGE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    return Environment.isExternalStorageManager();
                } else {
                    return ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;
                }
            case OVERLAY:
                return Settings.canDrawOverlays(activity);
        }
        return false;
    }

    public void requestPermission(PermissionType type, PermissionResultCallback callback) {
        this.callback = callback;
        switch (type) {
            case STORAGE:
                requestStoragePermission();
                break;
            case OVERLAY:
                requestOverlayPermission();
                break;
        }
    }

    private void requestStoragePermission() {
        if (!hasPermission(PermissionType.STORAGE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                new CustomAlertDialog(activity)
                        .setTitleText(activity.getString(R.string.storage_permission_title))
                        .setMessage(activity.getString(R.string.storage_permission_message))
                        .setPositiveButton(activity.getString(R.string.grant_permission), (v) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + activity.getPackageName()));
                            activity.startActivityForResult(intent, REQUEST_STORAGE);
                        })
                        .setNegativeButton(activity.getString(R.string.cancel), (v) -> callback.onPermissionDenied(PermissionType.STORAGE, false))
                        .show();
            } else {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE);
            }
        }
    }

    private void requestOverlayPermission() {
        if (!hasPermission(PermissionType.OVERLAY)) {
            if (!Settings.canDrawOverlays(activity)) {
            new CustomAlertDialog(activity)
                    .setTitleText(activity.getString(R.string.overlay_permission_message))
                    .setNegativeButton(activity.getString(R.string.cancel), (v) -> {
                        if (callback != null) {
                            callback.onPermissionDenied(PermissionType.OVERLAY, false);
                        }
                    })
                    .setPositiveButton(activity.getString(R.string.grant_permission), (v) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivityForResult(intent, REQUEST_OVERLAY);
                    })
                    .show();
        } else {
            if (callback != null) callback.onPermissionGranted(PermissionType.OVERLAY);
        }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (callback != null) callback.onPermissionGranted(PermissionType.STORAGE);
            } else {
                boolean deniedPermanently = false;
                if (permissions != null && permissions.length > 0) {
                    deniedPermanently = !ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[0]);
                }
                if (callback != null) callback.onPermissionDenied(PermissionType.STORAGE, deniedPermanently);
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_STORAGE) {
            if (hasPermission(PermissionType.STORAGE)) {
                if (callback != null) callback.onPermissionGranted(PermissionType.STORAGE);
            } else {
                if (callback != null) callback.onPermissionDenied(PermissionType.STORAGE, false);
            }
        } else if (requestCode == REQUEST_OVERLAY) {
            if (hasPermission(PermissionType.OVERLAY)) {
                if (callback != null) callback.onPermissionGranted(PermissionType.OVERLAY);
            } else {
                if (callback != null) callback.onPermissionDenied(PermissionType.OVERLAY, false);
            }
        }
    }
}
