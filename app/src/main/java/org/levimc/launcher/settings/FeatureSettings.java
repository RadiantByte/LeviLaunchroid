package org.levimc.launcher.settings;

import android.content.Context;
import android.widget.Toast;

import org.levimc.launcher.R;
import org.levimc.launcher.service.LogOverlay;
import org.levimc.launcher.util.PermissionsHandler;

public class FeatureSettings {
    private static volatile FeatureSettings INSTANCE;
    private static Context appContext;
    private boolean debugLogDialogEnabled = false;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static FeatureSettings getInstance() {
        if (INSTANCE == null) {
            synchronized (FeatureSettings.class) {
                if (INSTANCE == null) {
                    INSTANCE = SettingsStorage.load(appContext);
                    if (INSTANCE == null) {
                        INSTANCE = new FeatureSettings();
                    }
                }
            }
        }
        return INSTANCE;
    }

    public boolean isDebugLogDialogEnabled() {
        return debugLogDialogEnabled;
    }

    public void setDebugLogDialogEnabled(boolean enabled) {
        this.debugLogDialogEnabled = enabled;
        autoSave();
        LogOverlay logOverlay = LogOverlay.getInstance(appContext);
        if (enabled) {
            PermissionsHandler.getInstance().requestPermission(PermissionsHandler.PermissionType.OVERLAY, new PermissionsHandler.PermissionResultCallback() {
                @Override
                public void onPermissionGranted(PermissionsHandler.PermissionType type) {
                    if (type == PermissionsHandler.PermissionType.OVERLAY) {
                        LogOverlay.getInstance(appContext).show();
                    }
                }

                @Override
                public void onPermissionDenied(PermissionsHandler.PermissionType type, boolean permanentlyDenied) {
                    Toast.makeText(appContext, R.string.overlay_permission_not_granted, Toast.LENGTH_SHORT).show();

                }
            });
        } else {
            logOverlay.hide();
        }
    }

    private void autoSave() {
        if (appContext != null) {
            SettingsStorage.save(appContext, this);
        }
    }
}