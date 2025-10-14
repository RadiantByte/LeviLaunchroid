package org.levimc.launcher.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import org.levimc.launcher.R;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.views.MainViewModel;
import org.levimc.launcher.ui.views.MainViewModelFactory;

public class ModDetailActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private Mod currentMod;
    private int modPosition;
    private TextView modNameText;
    private TextView modFilenameText;
    private TextView modOrderText;
    private Switch modSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod_detail);

        if (getIntent().hasExtra("mod_filename") && getIntent().hasExtra("mod_position")) {
            String modFilename = getIntent().getStringExtra("mod_filename");
            modPosition = getIntent().getIntExtra("mod_position", -1);
            
            setupViewModel();
            setupViews();
            
            loadModDetails(modFilename);
        } else {
            Toast.makeText(this, R.string.error_loading_mod, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this, new MainViewModelFactory(getApplication())).get(MainViewModel.class);
    }

    private void setupViews() {
        modNameText = findViewById(R.id.mod_name_detail);
        modFilenameText = findViewById(R.id.mod_filename_detail);
        modOrderText = findViewById(R.id.mod_order_detail);
        modSwitch = findViewById(R.id.mod_switch_detail);

        ImageButton closeButton = findViewById(R.id.close_detail_button);
        closeButton.setOnClickListener(v -> finish());

        Button deleteButton = findViewById(R.id.delete_mod_button);
        deleteButton.setOnClickListener(v -> confirmDeleteMod());

        modSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentMod != null && isChecked != currentMod.isEnabled()) {
                currentMod.setEnabled(isChecked);
                viewModel.setModEnabled(currentMod.getFileName(), isChecked);
                Toast.makeText(this, isChecked ? R.string.mod_enabled : R.string.mod_disabled, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadModDetails(String modFilename) {
        if (viewModel != null) {
            viewModel.getModsLiveData().observe(this, mods -> {
                if (mods != null) {
                    for (Mod mod : mods) {
                        if (mod.getFileName().equals(modFilename)) {
                            currentMod = mod;
                            updateModUI(mod);
                            break;
                        }
                    }
                }
            });
            
            viewModel.refreshMods();
        }
    }

    private void updateModUI(Mod mod) {
        if (mod != null) {
            modNameText.setText(mod.getDisplayName());
            modFilenameText.setText(getString(R.string.mod_filename_format, mod.getFileName()));
            modOrderText.setText(getString(R.string.mod_load_order, modPosition + 1));
            modSwitch.setChecked(mod.isEnabled());
        }
    }

    private void confirmDeleteMod() {
        if (currentMod != null) {
            new CustomAlertDialog(this)
                    .setTitleText(getString(R.string.dialog_title_delete_mod))
                    .setMessage(getString(R.string.dialog_message_delete_mod))
                    .setPositiveButton(getString(R.string.dialog_positive_delete), v -> {
                        viewModel.removeMod(currentMod);
                        Toast.makeText(this, R.string.delete_mod, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                    .show();
        }
    }
}