package org.levimc.launcher.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import org.levimc.launcher.R;

public class LoadingDialog extends Dialog {
    private TextView messageView;
    public LoadingDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_loading, null);
        setContentView(view);
        setCancelable(false);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        messageView = view.findViewById(R.id.tv_message);
    }

    public void setMessage(CharSequence message) {
        if (messageView != null) {
            messageView.setText(message);
        }
    }
}