package com.mojang.minecraftpe.packagesource;

import androidx.annotation.NonNull;
import org.jetbrains.annotations.Contract;



public class PackageSourceFactory {
    @NonNull
    @Contract(pure = true)
    static PackageSource createGooglePlayPackageSource(String googlePlayLicenseKey, PackageSourceListener packageSourceListener) {
        return new StubPackageSource(packageSourceListener);
    }
}