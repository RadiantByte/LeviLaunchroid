package org.levimc.launcher.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.levimc.launcher.R;

import java.util.Locale;
import java.util.Objects;

public class LibsRepairDialog extends Dialog {
    private ProgressBar progressBar;
    private TextView progressText;

    public LibsRepairDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_libs_repair);

        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);

        setCancelable(false);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

    }

    public void updateProgress(int progress) {
        progressBar.setProgress(progress);
        progressText.setText(String.format(Locale.getDefault(), "%d%%", progress));
    }
}