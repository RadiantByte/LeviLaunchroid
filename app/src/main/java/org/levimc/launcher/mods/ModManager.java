package org.levimc.launcher.mods;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModManager {
    private static final String TAG = "ModManager";
    private static final String CONFIG_NAME = "mods_config.json";
    private static ModManager instance;
    private final File modsDir;
    private final File configFile;
    private final Gson gson = new Gson();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Map<String, Boolean> configMap = new HashMap<>();
    private OnModsUpdateListener listener;
    private long lastRefreshTime = 0;
    private static final long REFRESH_INTERVAL = 1000;

    public interface OnModsUpdateListener {
        void onModsUpdated(List<Mod> mods);
    }

    private ModManager(Context context) {
        modsDir = new File(
                Environment.getExternalStorageDirectory(),
                "games/org.levimc/mods"
        );
        configFile = new File(modsDir, CONFIG_NAME);
        initialize();
    }

    private void initialize() {
        if (!modsDir.exists() && !modsDir.mkdirs()) {
            Log.e(TAG, "Failed to create mods directory");
        }
        loadConfig();
    }

    public static synchronized ModManager getInstance(Context context) {
        if (instance == null) {
            instance = new ModManager(context.getApplicationContext());
        }
        return instance;
    }


    private boolean syncConfig(File[] currentFiles) {
        Set<String> existingFiles = Arrays.stream(currentFiles)
                .map(File::getName)
                .collect(Collectors.toSet());

        boolean removed = configMap.keySet().removeIf(key ->
                !existingFiles.contains(key) && !key.equals(CONFIG_NAME));

        boolean added = false;
        for (String fileName : existingFiles) {
            if (!configMap.containsKey(fileName)) {
                configMap.put(fileName, true);
                Log.d(TAG, "发现新mod: " + fileName);
                added = true;
            }
        }

        return removed || added;
    }

    public void refreshMods() {
        if (System.currentTimeMillis() - lastRefreshTime < REFRESH_INTERVAL) {
            return;
        }
        lastRefreshTime = System.currentTimeMillis();

        new Thread(() -> {
            File[] files = modsDir.listFiles(file ->
                    file.isFile() && file.getName().endsWith(".so"));

            if (files != null) {
                boolean configChanged = syncConfig(files);
                if (configChanged) {
                    saveConfig();
                }

                List<Mod> mods = Arrays.stream(files)
                        .map(file -> new Mod(
                                file.getName(),
                                Boolean.TRUE.equals(configMap.getOrDefault(file.getName(), true))
                        ))
                        .collect(Collectors.toList());
                notifyUpdate(mods);
            }
        }).start();
    }

    public void setModEnabled(String fileName, boolean enabled) {
        if (!fileName.endsWith(".so")) {
            fileName += ".so";
        }

        configMap.put(fileName, enabled);
        saveConfig();

        handler.postDelayed(this::refreshMods, 100);
    }

    private void loadConfig() {
        try (FileReader reader = new FileReader(configFile)) {
            Type type = new TypeToken<HashMap<String, Boolean>>(){}.getType();
            configMap = gson.fromJson(reader, type);
        } catch (Exception e) {
            configMap = new HashMap<>();
        }
    }

    private void saveConfig() {
        try {
            if (!modsDir.exists() && !modsDir.mkdirs()) {
                Log.e(TAG, "无法创建mods目录");
                return;
            }

            File tempFile = new File(configFile.getAbsolutePath() + ".tmp");
            try (FileWriter writer = new FileWriter(tempFile)) {
                gson.toJson(configMap, writer);
                writer.flush();

                if (tempFile.renameTo(configFile)) {
                    Log.d(TAG, "配置保存成功");
                } else {
                    Log.e(TAG, "文件重命名失败");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "保存配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void notifyUpdate(List<Mod> mods) {
        if (listener != null) {
            handler.post(() -> listener.onModsUpdated(mods));
        }
    }

    public void setOnModsUpdateListener(OnModsUpdateListener listener) {
        this.listener = listener;
    }
}