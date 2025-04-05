package org.levimc.launcher.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

public class UIHelper {

    public static void setTransparentNavigationBar(Activity activity) {
        Window window = activity.getWindow();
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    public static void showToast(Context context, String message) {
        runOnUiThread(context, () ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }

    public static void showToast(Context context, int resId, Object... formatArgs) {
        runOnUiThread(context, () -> {
            String message = context.getString(resId, formatArgs);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }

    public static void showLongToast(Context context, String message) {
        runOnUiThread(context, () ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        );
    }

    private static void runOnUiThread(Context context, Runnable action) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(action);
        } else {
            new android.os.Handler(
                    android.os.Looper.getMainLooper()
            ).post(action);
        }
    }
}