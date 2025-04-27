package org.levimc.launcher.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;

import org.levimc.launcher.R;

import java.util.ArrayList;
import java.util.List;

public class SettingsDialog extends Dialog {

    private LinearLayout rootLayout;

    private List<View> customItems = new ArrayList<>();

    private Button btnCancel, btnConfirm;

    private View.OnClickListener onCancelListener, onConfirmListener;

    public SettingsDialog(Context context) {
        super(context);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootLayout = new LinearLayout(getContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(24);
        rootLayout.setPadding(pad, pad, pad, pad);
        rootLayout.setBackground(getContext().getDrawable(R.drawable.bg_rounded_card));
        rootLayout.setMinimumWidth(dp(320));
        rootLayout.setMinimumHeight(dp(320));

        LinearLayout topLayout = new LinearLayout(getContext());
        topLayout.setOrientation(LinearLayout.HORIZONTAL);
        topLayout.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(R.drawable.ic_settings);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(28), dp(28));
        icon.setLayoutParams(iconParams);

        LinearLayout titleDescLayout = new LinearLayout(getContext());
        titleDescLayout.setOrientation(LinearLayout.VERTICAL);
        titleDescLayout.setPadding(dp(12), 0, 0, 0);

        TextView title = new TextView(getContext());
        title.setText("设置");
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        TextView desc = new TextView(getContext());
        desc.setText("您的启动器设置");
        desc.setTextSize(14);
        desc.setTextColor(0xFF888888);

        titleDescLayout.addView(title);
        titleDescLayout.addView(desc);

        topLayout.addView(icon);
        topLayout.addView(titleDescLayout);

        rootLayout.addView(topLayout);
        addSpace(rootLayout, dp(12));

        for (View v : customItems) {
            rootLayout.addView(v);
        }

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setContentView(rootLayout);
    }

    private void addSpace(ViewGroup parent, int h) {
        Space s = new Space(getContext());
        s.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h));
        parent.addView(s);
    }

    private int dp(int v) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (v * scale + 0.5f);
    }


    public Switch addSwitchItem(String label, boolean defChecked) {
        LinearLayout ll = new LinearLayout(getContext());
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ll.setPadding(0, 0, 0, dp(8));
        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextSize(16);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);

        Switch sw = new Switch(getContext());
        sw.setChecked(defChecked);

        ll.addView(tv);
        ll.addView(sw);
        customItems.add(ll);
        return sw;
    }

    public Spinner addSpinnerItem(String label, String[] options, int defaultIdx) {
        LinearLayout ll = new LinearLayout(getContext());
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ll.setPadding(0, dp(8), 0, 0);
        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextSize(16);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);

        Spinner spinner = new Spinner(getContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, options);
        spinner.setAdapter(adapter);
        spinner.setSelection(defaultIdx);

        ll.addView(tv);
        ll.addView(spinner);
        customItems.add(ll);
        return spinner;
    }

    public EditText addEditItem(String label, String hint) {
        LinearLayout ll = new LinearLayout(getContext());
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ll.setPadding(0, dp(16), 0, 0);

        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextSize(16);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);

        EditText et = new EditText(getContext());
        et.setHint(hint);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(dp(160), ViewGroup.LayoutParams.WRAP_CONTENT);
        et.setLayoutParams(etParams);

        ll.addView(tv);
        ll.addView(et);
        customItems.add(ll);
        return et;
    }

    public Button addActionButton(String label, View.OnClickListener listener) {
        LinearLayout ll = new LinearLayout(getContext());
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ll.setPadding(0, dp(16), 0, 0);

        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextSize(16);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);

        Button btn = new Button(getContext());
        btn.setText("执行");
        btn.setOnClickListener(listener);

        ll.addView(tv);
        ll.addView(btn);
        customItems.add(ll);
        return btn;
    }

    public void setOnCancelListener(View.OnClickListener listener) {
        this.onCancelListener = listener;
    }

    public void setOnConfirmListener(View.OnClickListener listener) {
        this.onConfirmListener = listener;
    }
}