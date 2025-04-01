package org.levimc.launcher;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;


import org.levimc.launcher.databinding.ActivityMainBinding;
import org.levimc.launcher.minecraft.MinecraftLauncher;
import org.levimc.launcher.mods.Mod;
import org.levimc.launcher.mods.ModManager;
import org.levimc.launcher.util.AnimationHelper;
import org.levimc.launcher.util.LanguageManager;
import org.levimc.launcher.util.PermissionsHandler;
import org.levimc.launcher.util.ResourcepackHandler;
import org.levimc.launcher.util.ThemeManager;
import org.levimc.launcher.util.UIHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ModManager.OnModsUpdateListener {

    private ActivityMainBinding binding;
    private MinecraftLauncher minecraftLauncher;
    private LanguageManager languageManager;
    private ThemeManager themeManager;
    private PermissionsHandler permissionsHandler;
    private ModManager modManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        languageManager = new LanguageManager(this);
        languageManager.applySavedLanguage();

        //themeManager = new ThemeManager(this);
        //themeManager.applyTheme();

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UIHelper.setTransparentNavigationBar(this);

        minecraftLauncher = new MinecraftLauncher(this, getClassLoader());
        modManager = ModManager.getInstance(this);
        modManager.setOnModsUpdateListener(this);

        permissionsHandler = new PermissionsHandler(this, modManager);

        initListeners();
        AnimationHelper.prepareInitialStates(binding);
        AnimationHelper.runInitializationSequence(binding);

       // binding.themeSwitch.setChecked(themeManager.isDarkMode());

        if (permissionsHandler.hasStoragePermission()) {
            modManager.refreshMods();
        } else {
            permissionsHandler.requestStoragePermission();
        }

        setTextMinecraftVersion();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        ResourcepackHandler resourcepackHandler = new ResourcepackHandler(
                this,
                minecraftLauncher,
                executorService,
                binding.progressLoader,
                binding.launchButton
        );
        resourcepackHandler.checkIntentForResourcepack();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionsHandler.handlePermissionResult(requestCode);
    }
    @SuppressLint({"ClickableViewAccessibility", "UnsafeIntentLaunch"})
    private void initListeners() {
        binding.launchButton.setOnClickListener(v -> {
            binding.launchButton.setEnabled(false);
            binding.progressLoader.setVisibility(View.VISIBLE);
            new Thread(() -> {
                minecraftLauncher.launch(getIntent());
                runOnUiThread(() -> {
                    binding.progressLoader.setVisibility(View.GONE);
                    binding.launchButton.setEnabled(true);
                });
            }).start();
        });

        binding.launchButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setInterpolator(new OvershootInterpolator()).setDuration(200).start();
            }
            return false;
        });

        binding.languageButton.setOnClickListener(v -> languageManager.showLanguageMenu(v));

        //binding.themeSwitch.setOnCheckedChangeListener((button, checked) -> themeManager.toggleTheme(checked));
    }

    @Override
    public void onModsUpdated(List<Mod> mods) {
        runOnUiThread(() -> updateModsUI(mods));
    }

    private String getMinecraftVersion() throws PackageManager.NameNotFoundException {
        return getPackageManager().getPackageInfo("com.mojang.minecraftpe", PackageManager.GET_CONFIGURATIONS).versionName;
    }

    private void setTextMinecraftVersion() {
        try {
            String str = getMinecraftVersion();
            binding.textMinecraftVersion.setText(str);
        } catch (PackageManager.NameNotFoundException e) {
            binding.textMinecraftVersion.setText("Null");
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("未安装Minecraft")
                    .setMessage("请先安装Minecraft")
                    .setPositiveButton("退出", (d, which) -> {
                        finish();
                    })
                    .setCancelable(false)
                    .create();

            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.on_surface));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        }
    }

    private void updateModsUI(List<Mod> mods) {
        binding.modContent.removeAllViews();

        //title.setText(getString(R.string.mods_title, mods.size()));
        String formattedTitle = getString(R.string.mods_title, mods.size());
        binding.modsTitleText.setText(formattedTitle);

        if (mods.isEmpty()) {
            binding.modContent.addView(binding.noModsText);
            binding.noModsText.setVisibility(View.VISIBLE);
            return;
        }

        binding.noModsText.setVisibility(View.GONE);
        for (Mod mod : mods)
            binding.modContent.addView(createModView(mod));
    }

    @SuppressLint("SetTextI18n")
    private View createModView(Mod mod) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        //layout.setPadding(0, 0, 0, 0);

        TextView tv = new TextView(this);
        tv.setText("· " + mod.getDisplayName());
        tv.setTextSize(16);

        int color = mod.isEnabled() ? ResourcesCompat.getColor(getResources(), R.color.on_background, null)
                : ResourcesCompat.getColor(getResources(), R.color.on_surface, null);
        tv.setTextColor(color);
        tv.setTypeface(ResourcesCompat.getFont(this, R.font.misans));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        Switch switchBtn = getSwitchBtn(mod, tv);

        layout.addView(tv);
        layout.addView(switchBtn);
        return layout;
    }

    @NonNull
    private Switch  getSwitchBtn(Mod mod, TextView tv) {
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        Switch  switchBtn = new Switch (this);
        switchBtn.setChecked(mod.isEnabled());
        switchBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mod.setEnabled(isChecked);
            modManager.setModEnabled(mod.getFileName(), isChecked);
            int newColor = isChecked ? ResourcesCompat.getColor(getResources(), R.color.on_background, null)
                    : ResourcesCompat.getColor(getResources(), R.color.on_surface, null);
            tv.setTextColor(newColor);
        });
        return switchBtn;
    }
}