package org.levimc.launcher.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class IntentHandler extends BaseActivity {
    private static final String TAG = "IntentHandler";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleDeepLink(intent);
    }

    @SuppressLint("IntentReset")
    private void handleDeepLink(Intent originalIntent) {
        Intent newIntent = new Intent(originalIntent);

        if (isMcRunning()) {
            newIntent.setClassName(this, "com.mojang.minecraftpe.Launcher");
        } else {
            newIntent.setClassName(this, "org.levimc.launcher.MainActivity");
        }

        startActivity(newIntent);
        finish();
    }

    private boolean isMcRunning(){
        try {
            Class<?> clazz = Class.forName("com.mojang.minecraftpe.Launcher", false, getClassLoader());
            Log.d(TAG, "Minecraft PE Launcher class exists!");
            return true;
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "Minecraft PE Launcher class not found.");
            return false;
        }
    }
}