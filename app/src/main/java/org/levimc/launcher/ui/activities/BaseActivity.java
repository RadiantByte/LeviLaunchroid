package org.levimc.launcher.ui.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.levimc.launcher.util.ThemeManager;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager themeManager = new ThemeManager(this);
        themeManager.setThemeMode(themeManager.getCurrentMode());
        super.onCreate(savedInstanceState);
    }

}