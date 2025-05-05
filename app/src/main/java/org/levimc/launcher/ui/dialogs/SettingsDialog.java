package org.levimc.launcher.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import org.levimc.launcher.R;
import org.levimc.launcher.util.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsDialog extends Dialog {

    private LinearLayout settingsItemsContainer;
    private final List<Runnable> itemAddQueue = new ArrayList<>();

    public SettingsDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        setContentView(R.layout.dialog_settings);
        settingsItemsContainer = findViewById(R.id.settings_items);

        for (Runnable r : itemAddQueue) r.run();
        itemAddQueue.clear();
    }

    public void addSwitchItem(String label, boolean defChecked, CompoundButton.OnCheckedChangeListener listener) {
        int switchId = View.generateViewId();
        Runnable action = () -> {
            View ll = LayoutInflater.from(getContext()).inflate(R.layout.item_settings_switch, settingsItemsContainer, false);
            ((TextView) ll.findViewById(R.id.tv_title)).setText(label);
            Switch sw = ll.findViewById(R.id.switch_value);
            sw.setId(switchId);
            sw.setChecked(defChecked);
            if (listener != null) sw.setOnCheckedChangeListener(listener);
            settingsItemsContainer.addView(ll);
        };
        if (settingsItemsContainer == null) itemAddQueue.add(action);
        else action.run();
    }

    public Spinner addSpinnerItem(String label, String[] options, int defaultIdx) {
        final Context ctx = getContext();
        final Spinner[] out = new Spinner[1];
        Runnable action = () -> {
            View ll = LayoutInflater.from(ctx).inflate(R.layout.item_settings_spinner, settingsItemsContainer, false);
            ((TextView) ll.findViewById(R.id.tv_title)).setText(label);
            Spinner spinner = ll.findViewById(R.id.spinner_value);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, options);
            spinner.setAdapter(adapter);
            spinner.setSelection(defaultIdx);
            settingsItemsContainer.addView(ll);
            out[0] = spinner;
        };
        if (settingsItemsContainer == null) {
            itemAddQueue.add(action);
            return null;
        } else {
            action.run();
            return out[0];
        }
    }

    public EditText addEditItem(String label, String hint) {
        final Context ctx = getContext();
        final EditText[] out = new EditText[1];
        Runnable action = () -> {
            View ll = LayoutInflater.from(ctx).inflate(R.layout.item_settings_edittext, settingsItemsContainer, false);
            ((TextView) ll.findViewById(R.id.tv_title)).setText(label);
            EditText et = ll.findViewById(R.id.edit_value);
            et.setHint(hint);
            settingsItemsContainer.addView(ll);
            out[0] = et;
        };
        if (settingsItemsContainer == null) {
            itemAddQueue.add(action);
            return null;
        } else {
            action.run();
            return out[0];
        }
    }

    public Button addActionButton(String label, String buttonText, View.OnClickListener listener) {
        final Context ctx = getContext();
        final Button[] out = new Button[1];
        Runnable action = () -> {
            View ll = LayoutInflater.from(ctx).inflate(R.layout.item_settings_button, settingsItemsContainer, false);
            ((TextView) ll.findViewById(R.id.tv_title)).setText(label);
            Button btn = ll.findViewById(R.id.btn_action);
            btn.setText(buttonText);
            btn.setOnClickListener(listener);
            settingsItemsContainer.addView(ll);
            out[0] = btn;
        };
        if (settingsItemsContainer == null) {
            itemAddQueue.add(action);
            return null;
        } else {
            action.run();
            return out[0];
        }
    }

    public void addThemeSelectorItem(ThemeManager themeManager) {
        String[] themeOptions = {
                getContext().getString(R.string.theme_follow_system),
                getContext().getString(R.string.theme_light),
                getContext().getString(R.string.theme_dark)
        };

        int currentMode = themeManager.getCurrentMode();

        final Spinner[] spinnerRef = new Spinner[1];
        Runnable action = () -> {
            Spinner spinner = addSpinnerItem(
                    getContext().getString(R.string.theme_title),
                    themeOptions,
                    currentMode
            );
            if (spinner != null) {
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        themeManager.setThemeMode(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
                spinnerRef[0] = spinner;
            }
        };

        if (settingsItemsContainer == null) {
            itemAddQueue.add(action);
        } else {
            action.run();
        }

    }
}