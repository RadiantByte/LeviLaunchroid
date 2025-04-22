package org.levimc.launcher.ui.views;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import org.levimc.launcher.core.mods.ModManager;
import org.levimc.launcher.core.versions.VersionManager;

public class MainViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;

    public MainViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> clazz) {
        ModManager modManager = ModManager.getInstance();
        VersionManager versionManager = VersionManager.get(application);
        return (T) new MainViewModel(modManager, versionManager);
    }
}