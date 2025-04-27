package org.levimc.launcher.settings;

import android.content.Context;

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
    }

    private void autoSave() {
        if (appContext  != null) {
            SettingsStorage.save(appContext , this);
        }
    }
}