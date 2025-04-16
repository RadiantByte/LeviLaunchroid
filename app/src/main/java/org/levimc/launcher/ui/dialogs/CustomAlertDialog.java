package org.levimc.launcher.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import org.levimc.launcher.R;

public class CustomAlertDialog extends Dialog {

    private String mTitle;
    private String mMessage;
    private String mPositiveText;
    private String mNegativeText;
    private View.OnClickListener mPositiveListener;
    private View.OnClickListener mNegativeListener;

    public CustomAlertDialog(Context context) {
        super(context);
    }

    public CustomAlertDialog setTitleText(String title) {
        this.mTitle = title;
        return this;
    }

    public CustomAlertDialog setMessage(String message) {
        this.mMessage = message;
        return this;
    }

    public CustomAlertDialog setPositiveButton(String text, View.OnClickListener listener) {
        this.mPositiveText = text;
        this.mPositiveListener = listener;
        return this;
    }

    public CustomAlertDialog setNegativeButton(String text, View.OnClickListener listener) {
        this.mNegativeText = text;
        this.mNegativeListener = listener;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alert_dialog_custom);
        setCanceledOnTouchOutside(false);

        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvMessage = findViewById(R.id.tv_message);
        Button btnPositive = findViewById(R.id.btn_positive);
        Button btnNegative = findViewById(R.id.btn_negative);
        View spacingView = findViewById(R.id.btn_spacing);

        tvTitle.setText(mTitle != null ? mTitle : "");
        tvMessage.setText(mMessage != null ? mMessage : "");

        if (mNegativeText != null) {
            btnNegative.setText(mNegativeText);
            btnNegative.setVisibility(View.VISIBLE);
            if(spacingView != null) spacingView.setVisibility(View.VISIBLE);
        } else {
            btnNegative.setVisibility(View.GONE);
            if(spacingView != null) spacingView.setVisibility(View.GONE);
        }

        if (mPositiveText != null) {
            btnPositive.setText(mPositiveText);
            btnPositive.setVisibility(View.VISIBLE);
        } else {
            btnPositive.setVisibility(View.GONE);
        }

        btnPositive.setOnClickListener(v -> {
            if (mPositiveListener != null) mPositiveListener.onClick(v);
            dismiss();
        });

        btnNegative.setOnClickListener(v -> {
            if (mNegativeListener != null) mNegativeListener.onClick(v);
            dismiss();
        });

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}