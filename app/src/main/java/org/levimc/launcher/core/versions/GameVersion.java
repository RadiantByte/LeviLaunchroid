package org.levimc.launcher.core.versions;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

public class GameVersion implements Parcelable {
    public String versionCode;
    public String displayName;
    public File versionDir;
    public boolean isInstalled;
    public String packageName;
    public final File modsDir;

    public GameVersion(String displayName, String versionCode, File versionDir, boolean isOfficial, String packageName) {
        this.displayName = displayName;
        this.versionCode = versionCode;
        this.versionDir = versionDir;
        this.isInstalled = isOfficial;
        this.packageName = packageName;
        this.modsDir = versionDir == null ? null : new File(versionDir, "mods");
    }

    protected GameVersion(Parcel in) {
        displayName = in.readString();
        versionCode = in.readString();
        String versionDirPath = in.readString();
        versionDir = versionDirPath == null ? null : new File(versionDirPath);
        isInstalled = in.readByte() != 0;
        packageName = in.readString();
        String modsDirPath = in.readString();
        modsDir = modsDirPath == null ? null : new File(modsDirPath);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(displayName);
        dest.writeString(versionCode);
        dest.writeString(versionDir == null ? null : versionDir.getAbsolutePath());
        dest.writeByte((byte) (isInstalled ? 1 : 0));
        dest.writeString(packageName);
        dest.writeString(modsDir == null ? null : modsDir.getAbsolutePath());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GameVersion> CREATOR = new Creator<>() {
        @Override
        public GameVersion createFromParcel(Parcel in) {
            return new GameVersion(in);
        }

        @Override
        public GameVersion[] newArray(int size) {
            return new GameVersion[size];
        }
    };
}