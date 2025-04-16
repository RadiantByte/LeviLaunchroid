package org.levimc.launcher.ui.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;

import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import org.levimc.launcher.R;
import org.levimc.launcher.databinding.ActivityMainBinding;

import org.levimc.launcher.service.LogOverlay;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.views.MainViewModel;
import org.levimc.launcher.ui.views.MainViewModelFactory;
import org.levimc.launcher.util.Logger;
import org.levimc.launcher.core.minecraft.MinecraftLauncher;
import org.levimc.launcher.core.mods.FileHandler;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.ui.animation.AnimationHelper;
import org.levimc.launcher.util.LanguageManager;
import org.levimc.launcher.util.PermissionsHandler;
import org.levimc.launcher.util.ResourcepackHandler;
import org.levimc.launcher.util.ThemeManager;
import org.levimc.launcher.util.UIHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity  {

    static {
        System.loadLibrary("leviutils");
    }

    private ActivityMainBinding binding;
    private MinecraftLauncher minecraftLauncher;
    private LanguageManager languageManager;
    private ThemeManager themeManager;
    private PermissionsHandler permissionsHandler;
    private FileHandler fileHandler;
    public static Logger logger;
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        languageManager = new LanguageManager(this);
        languageManager.applySavedLanguage();
        logger = new Logger("LeviMC");

        //themeManager = new ThemeManager(this);
        //themeManager.applyTheme();

        //logger.info("auto close logger when exited block.");

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(
                this,
                new MainViewModelFactory(getApplication())
        ).get(MainViewModel.class);
        viewModel.getModsLiveData().observe(this, this::updateModsUI);

        UIHelper.setTransparentNavigationBar(this);

        minecraftLauncher = new MinecraftLauncher(this, getClassLoader());

        initListeners();
        AnimationHelper.prepareInitialStates(binding);
        AnimationHelper.runInitializationSequence(binding);

       // binding.themeSwitch.setChecked(themeManager.isDarkMode());

        permissionsHandler = new PermissionsHandler(this);

        permissionsHandler.requestPermission(PermissionsHandler.PermissionType.STORAGE, new PermissionsHandler.PermissionResultCallback() {
            @Override
            public void onPermissionGranted(PermissionsHandler.PermissionType type) {
                if (type == PermissionsHandler.PermissionType.STORAGE) {
                    viewModel.refreshMods();
                }
            }
            @Override
            public void onPermissionDenied(PermissionsHandler.PermissionType type, boolean permanentlyDenied) {
                if (type == PermissionsHandler.PermissionType.STORAGE) {
                    Toast.makeText(MainActivity.this, R.string.storage_permission_not_granted, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        viewModel.refreshMods();

        fileHandler = new FileHandler(this, viewModel);

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
        handleIncomingFiles();
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getModsLiveData().observe(this, this::updateModsUI);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionsHandler.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

        binding.logOverlaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            LogOverlay logOverlay = LogOverlay.getInstance(this);
            if (isChecked) {
                permissionsHandler.requestPermission(PermissionsHandler.PermissionType.OVERLAY, new PermissionsHandler.PermissionResultCallback() {
                    @Override
                    public void onPermissionGranted(PermissionsHandler.PermissionType type) {
                        if (type == PermissionsHandler.PermissionType.OVERLAY) {
                            LogOverlay.getInstance(MainActivity.this).show();
                        }
                    }
                    @Override
                    public void onPermissionDenied(PermissionsHandler.PermissionType type, boolean permanentlyDenied) {
                        Toast.makeText(MainActivity.this, R.string.overlay_permission_not_granted, Toast.LENGTH_SHORT).show();
                        binding.logOverlaySwitch.setChecked(false);
                    }
                });
            } else {
                logOverlay.hide();
            }
        });
        //binding.themeSwitch.setOnCheckedChangeListener((button, checked) -> themeManager.toggleTheme(checked));
    }

    private void onVersionSelected(String version) {
        Toast.makeText(this, "已选择版本：" + version, Toast.LENGTH_SHORT).show();
    }

    private String getMinecraftVersion() throws PackageManager.NameNotFoundException {
        return getPackageManager().getPackageInfo("com.mojang.minecraftpe", PackageManager.GET_CONFIGURATIONS).versionName;
    }

    private void setTextMinecraftVersion() {
        try {
            String str = getMinecraftVersion();
            binding.textMinecraftVersion.setText(str);
        } catch (PackageManager.NameNotFoundException e) {
            new CustomAlertDialog(this)
                    .setTitleText(getString(R.string.no_minecraft))
                    .setMessage(getString(R.string.no_install_minecraft))
                    .setPositiveButton(getString(R.string.exit), (v) -> {
                        finish();
                    }).show();
        }
    }

    private void handleIncomingFiles() {
        fileHandler.processIncomingFilesWithConfirmation(getIntent(), new FileHandler.FileOperationCallback() {
            @Override
            public void onSuccess(int processedFiles) {
                if (processedFiles > 0) {
                    UIHelper.showToast(MainActivity.this,
                            getString(R.string.files_processed, processedFiles));
                }
            }

            @Override
            public void onError(String errorMessage) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.error)
                        .setMessage(errorMessage)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }

            @Override
            public void onProgressUpdate(int progress) {
                binding.progressLoader.setProgress(progress);
            }
        });
    }
    private void updateModsUI(List<Mod> mods) {
        binding.modContent.removeAllViews();
        if (mods == null || mods.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No Mods Found");
            binding.modContent.addView(tv);
            return;
        }
        String formattedTitle = getString(R.string.mods_title, mods.size());
        binding.modsTitleText.setText(formattedTitle);
        for (Mod mod : mods) {
            binding.modContent.addView(createModView(mod));
        }
    }

    private View createModView(Mod mod) {
        LayoutInflater inflater = LayoutInflater.from(this);

        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.item_mod, null);
        TextView name = view.findViewById(R.id.mod_name);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchBtn = view.findViewById(R.id.mod_switch);

        name.setText(mod.getDisplayName());
        switchBtn.setChecked(mod.isEnabled());

        switchBtn.setOnCheckedChangeListener((btn, isChecked) -> {
            viewModel.setModEnabled(mod.getFileName(), isChecked);
        });

        return view;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}