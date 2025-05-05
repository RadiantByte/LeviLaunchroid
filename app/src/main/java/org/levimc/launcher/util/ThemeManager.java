package org.levimc.launcher.util;

import android.app.Activity;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String THEME_PREFS = "theme_prefs";
    private static final String DARK_MODE_KEY = "dark_mode";
    private final SharedPreferences prefs;

    public ThemeManager(Activity activity) {
        prefs = activity.getSharedPreferences(THEME_PREFS, Activity.MODE_PRIVATE);
    }

    public void applyTheme() {
        boolean isDark = prefs.getBoolean(DARK_MODE_KEY, false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public boolean toggleTheme(boolean isDarkMode) {
        prefs.edit().putBoolean(DARK_MODE_KEY, isDarkMode).apply();
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        return isDarkMode;
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(DARK_MODE_KEY, false);
    }
}