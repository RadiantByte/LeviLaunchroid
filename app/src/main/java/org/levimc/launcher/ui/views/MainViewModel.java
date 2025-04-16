package org.levimc.launcher.ui.views;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.levimc.launcher.core.mods.Mod;
import org.levimc.launcher.core.mods.ModManager;
import java.util.List;

public class MainViewModel extends ViewModel {
    private final ModManager modManager;
    private final MutableLiveData<List<Mod>> modsLiveData = new MutableLiveData<>();

    public MainViewModel(ModManager modManager) {
        this.modManager = modManager;
        modManager.getModsChangedLiveData().observeForever(trigger -> refreshMods());
        refreshMods();
    }

    public void refreshMods() {
        new Thread(() -> {
            List<Mod> mods = modManager.getMods();
            modsLiveData.postValue(mods);
        }).start();
    }

    public LiveData<List<Mod>> getModsLiveData() {
        return modsLiveData;
    }

    public void setModEnabled(String fileName, boolean enabled) {
        modManager.setModEnabled(fileName, enabled);
        refreshMods();
    }
}