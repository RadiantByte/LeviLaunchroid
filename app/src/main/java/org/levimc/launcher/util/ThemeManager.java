package org.levimc.launcher.util;

import android.app.Activity;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    public static final int MODE_FOLLOW_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    private static final String THEME_PREFS = "theme_prefs";
    private static final String THEME_MODE_KEY = "theme_mode";
    private final SharedPreferences prefs;

    public ThemeManager(Activity activity) {
        prefs = activity.getSharedPreferences(THEME_PREFS, Activity.MODE_PRIVATE);
    }

    public void applyTheme() {
        setThemeMode(prefs.getInt(THEME_MODE_KEY, MODE_FOLLOW_SYSTEM));
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(THEME_MODE_KEY, mode).apply();
        updateNightMode();
    }

    private void updateNightMode() {
        int mode = prefs.getInt(THEME_MODE_KEY, MODE_FOLLOW_SYSTEM);
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default: // MODE_FOLLOW_SYSTEM
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public int getCurrentMode() {
        return prefs.getInt(THEME_MODE_KEY, MODE_FOLLOW_SYSTEM);
    }
}