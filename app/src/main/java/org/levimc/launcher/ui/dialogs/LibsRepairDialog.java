package org.levimc.launcher.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.animation.DynamicAnim;

import java.util.Locale;
import java.util.Objects;

public class LibsRepairDialog extends Dialog {
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView titleText;
    private TextView statusText;

    public LibsRepairDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_libs_repair);

        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);
        titleText = findViewById(R.id.title);
        statusText = findViewById(R.id.status_text);

        setCancelable(false);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

        View content = findViewById(android.R.id.content);
        if (content != null) {
            DynamicAnim.animateDialogShow(content);
        }

    }

    public void updateProgress(int progress) {
        progressBar.setProgress(progress);
        progressText.setText(String.format(Locale.getDefault(), "%d%%", progress));
    }

    public void setIndeterminate(boolean indeterminate) {
        progressBar.setIndeterminate(indeterminate);
    }

    public void setTitleText(String text) {
        if (titleText != null) titleText.setText(text);
    }

    public void setStatusText(String text) {
        if (statusText != null) statusText.setText(text);
    }

    @Override
    public void dismiss() {
        Window window = getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0f;
            window.setAttributes(params);
        }
        View content = findViewById(android.R.id.content);
        if (content != null) {
            DynamicAnim.animateDialogDismiss(content, () -> LibsRepairDialog.super.dismiss());
        } else {
            super.dismiss();
        }
    }
}