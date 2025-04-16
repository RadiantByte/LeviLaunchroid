package org.levimc.launcher.core.mods;

import android.content.Context;
import android.os.Environment;
import android.os.FileObserver;

import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ModManager {
    private static ModManager instance;

    private final File modsDir;
    private final File configFile;
    private final Map<String, Boolean> configMap = new HashMap<>();
    private final Gson gson = new Gson();
    private FileObserver modDirObserver;
    private final MutableLiveData<Void> modsChangedLiveData = new MutableLiveData<>();
    private ModManager(Context context) {
        modsDir = new File(
                Environment.getExternalStorageDirectory(),
                "games/org.levimc/mods"
        );
        configFile = new File(modsDir, "mods_config.json");
        if (!modsDir.exists()) modsDir.mkdirs();
        loadConfig();
        initFileObserver();
    }

    public static synchronized ModManager getInstance(Context context) {
        if (instance == null) {
            instance = new ModManager(context.getApplicationContext());
        }
        return instance;
    }

    public List<Mod> getMods() {
        File[] files = modsDir.listFiles((dir, name) -> name.endsWith(".so"));
        List<Mod> mods = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                boolean enabled = configMap.getOrDefault(fileName, true);
                mods.add(new Mod(fileName, enabled));
            }
        }
        return mods;
    }

    public void setModEnabled(String fileName, boolean enabled) {
        if (!fileName.endsWith(".so")) fileName += ".so";
        configMap.put(fileName, enabled);
        saveConfig();
    }

    private void loadConfig() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            Map<String, Boolean> map = gson.fromJson(reader, Map.class);
            if (map != null) configMap.putAll(map);
        } catch (Exception ignored) {}
    }

    private void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(configMap, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initFileObserver() {
        if (modDirObserver != null) return;
        modDirObserver = new FileObserver(modsDir.getAbsolutePath(), FileObserver.CREATE | FileObserver.DELETE | FileObserver.MOVED_FROM | FileObserver.MOVED_TO) {
            @Override
            public void onEvent(int event, String path) {
                modsChangedLiveData.postValue(null);
            }
        };
        modDirObserver.startWatching();
    }

    public MutableLiveData<Void> getModsChangedLiveData() {
        return modsChangedLiveData;
    }
}