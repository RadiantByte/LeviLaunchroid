package org.levimc.launcher.core.versions;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

public class GameVersion implements Parcelable {
    public String directoryName;
    public String versionCode;
    public String displayName;
    public String uuid;
    public File versionDir;
    public boolean isInstalled;
    public String packageName;
    public boolean needsRepair;
    public final File modsDir;

    public GameVersion(String directoryName, String displayName, String versionCode, String uuid, File versionDir, boolean isOfficial, String packageName) {
        this.directoryName = directoryName;
        this.uuid = uuid;
        this.displayName = displayName;
        this.versionCode = versionCode;
        this.versionDir = versionDir;
        this.isInstalled = isOfficial;
        this.packageName = packageName;
        this.needsRepair = false;
        this.modsDir = versionDir == null ? null : new File(versionDir, "mods");
    }

    protected GameVersion(Parcel in) {
        directoryName = in.readString();
        displayName = in.readString();
        versionCode = in.readString();
        uuid = in.readString();
        String versionDirPath = in.readString();
        versionDir = versionDirPath == null ? null : new File(versionDirPath);
        isInstalled = in.readByte() != 0;
        packageName = in.readString();
        needsRepair = in.readByte() != 0;
        String modsDirPath = in.readString();
        modsDir = modsDirPath == null ? null : new File(modsDirPath);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(directoryName);
        dest.writeString(displayName);
        dest.writeString(versionCode);
        dest.writeString(uuid);
        dest.writeString(versionDir == null ? null : versionDir.getAbsolutePath());
        dest.writeByte((byte) (isInstalled ? 1 : 0));
        dest.writeString(packageName);
        dest.writeByte((byte) (needsRepair ? 1 : 0));
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