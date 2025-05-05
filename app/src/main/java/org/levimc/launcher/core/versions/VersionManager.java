package org.levimc.launcher.core.versions;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import org.json.JSONObject;
import org.levimc.launcher.R;
import org.levimc.launcher.ui.activities.MainActivity;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;
import org.levimc.launcher.ui.dialogs.LibsRepairDialog;
import org.levimc.launcher.util.ApkUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class VersionManager {
    private static VersionManager instance;
    private final Context context;
    private List<GameVersion> installedVersions = new ArrayList<>();
    private List<GameVersion> customVersions = new ArrayList<>();
    private GameVersion selectedVersion;
    private SharedPreferences prefs;

    public interface LibsRepairCallback {
        void onRepairStarted();

        void onRepairProgress(int progress);

        void onRepairCompleted(boolean success);

        void onRepairFailed(Exception e);
    }

    public static VersionManager get(Context ctx) {
        if (instance == null) instance = new VersionManager(ctx.getApplicationContext());
        return instance;
    }

    public static String getSelectedModsDir(Context ctx) {
        GameVersion v = get(ctx).getSelectedVersion();
        if (v == null || v.modsDir == null) return null;
        return v.modsDir.getAbsolutePath();
    }

    private VersionManager(Context ctx) {
        this.context = ctx;
        this.prefs = ctx.getSharedPreferences("version_manager", Context.MODE_PRIVATE);
        loadAllVersions();
    }

    private boolean isMinecraftPackage(String packageName) {
        return packageName.equals("com.mojang.minecraftpe")
                || packageName.startsWith("com.mojang.");
    }

    private boolean ensureLibsExist(File versionDir, String uuid, LibsRepairCallback callback) {
        File libDir = new File(context.getDataDir(), "minecraft/" + uuid + "/lib");
        File so1 = new File(libDir, "arm64/libminecraftpe.so");
        File so2 = new File(libDir, "arm/libminecraftpe.so");
        boolean exists = so1.exists() || so2.exists();

        if (exists) return true;

        File apkFile = new File(versionDir, "base.apk.levi");
        if (!apkFile.exists()) return false;

        return false;
    }

    public void repairLibsAsync(File versionDir, String uuid, LibsRepairCallback callback) {
        new Thread(() -> {
            try {
                callback.onRepairStarted();

                File libDir = new File(context.getDataDir(), "minecraft/" + uuid + "/lib");
                File apkFile = new File(versionDir, "base.apk.levi");

                try (InputStream is = new FileInputStream(apkFile);
                     ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {

                    ZipEntry entry;
                    long totalSize = 0;
                    long processedSize = 0;

                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.getName().startsWith("lib/") && !entry.isDirectory()) {
                            totalSize += entry.getSize();
                        }
                        zis.closeEntry();
                    }

                    zis.close();
                    is.close();
                    try (InputStream is2 = new FileInputStream(apkFile);
                         ZipInputStream zis2 = new ZipInputStream(new BufferedInputStream(is2))) {

                        while ((entry = zis2.getNextEntry()) != null) {
                            if (entry.getName().startsWith("lib/") && !entry.isDirectory()) {
                                String[] parts = entry.getName().split("/");
                                if (parts.length < 3) continue;

                                String abi = parts[1];
                                String systemAbi = ApkUtils.abiToSystemLibDir(abi);
                                String soName = parts[2];
                                File outFile = new File(libDir, systemAbi + "/" + soName);
                                File parent = outFile.getParentFile();
                                if (!parent.exists()) parent.mkdirs();

                                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                    byte[] buffer = new byte[8192];
                                    int len;
                                    while ((len = zis2.read(buffer)) != -1) {
                                        fos.write(buffer, 0, len);
                                        processedSize += len;
                                        int progress = (int) ((processedSize * 100) / totalSize);
                                        callback.onRepairProgress(progress);
                                    }
                                }
                            }
                            zis2.closeEntry();
                        }
                    }

                    callback.onRepairCompleted(true);
                }
            } catch (Exception e) {
                callback.onRepairFailed(e);
            }
        }).start();
    }

    public void loadAllVersions() {
        installedVersions.clear();
        customVersions.clear();

        PackageManager pm = context.getPackageManager();
        List<PackageInfo> pkgs = pm.getInstalledPackages(0);
        for (PackageInfo pi : pkgs) {
            if (isMinecraftPackage(pi.packageName)) {
                File versionDir = new File(
                        Environment.getExternalStorageDirectory(),
                        "games/org.levimc/minecraft/" + pi.packageName
                );
                if (!versionDir.exists()) versionDir.mkdirs();
                GameVersion gv = new GameVersion(
                        "",
                        pi.applicationInfo.loadLabel(pm) + " (" + pi.versionName + ")",
                        pi.versionName,
                        "",
                        versionDir,
                        true,
                        pi.packageName
                );
                installedVersions.add(gv);
            }
        }

        File baseDir = new File(Environment.getExternalStorageDirectory(), "games/org.levimc/minecraft/");
        File[] dirs = baseDir.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                File vj = new File(dir, "version.json");
                if (vj.exists()) {
                    boolean exists = false;
                    for (GameVersion g : installedVersions) {
                        if (g.versionDir.getAbsolutePath().equals(dir.getAbsolutePath())) {
                            exists = true;
                            break;
                        }
                    }
                    if (exists) continue;
                    try (FileInputStream fi = new FileInputStream(vj); Scanner sc = new Scanner(fi).useDelimiter("\\A")) {
                        String json = sc.hasNext() ? sc.next() : "";
                        JSONObject obj = new JSONObject(json);
                        String versionName = obj.optString("versionName", dir.getName());
                        String versionCode = obj.optString("version", "");
                        String uuid = obj.optString("uuid", "");

                        File libDir = new File(context.getDataDir(), "minecraft/" + uuid + "/lib");
                        File so1 = new File(libDir, "arm64/libminecraftpe.so");
                        File so2 = new File(libDir, "arm/libminecraftpe.so");
                        boolean libOk = so1.exists() || so2.exists();

                        if (libOk) {
                            GameVersion gv = new GameVersion(dir.getName(), versionName + " (" + versionCode + ")", versionCode, uuid, dir, false, null);
                            customVersions.add(gv);
                        } else {
                            GameVersion gv = new GameVersion(
                                    dir.getName(),
                                    versionName + " (" + versionCode + ") ❌",
                                    versionCode,
                                    uuid,
                                    dir,
                                    false,
                                    null
                            );
                            gv.needsRepair = true;
                            customVersions.add(gv);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        restoreSelectedVersion();
    }

    private void restoreSelectedVersion() {
        String type = prefs.getString("selected_type", null);
        if (type != null) {
            if (type.equals("official")) {
                String pkg = prefs.getString("selected_package", null);
                for (GameVersion gv : installedVersions) {
                    if (gv.packageName != null && gv.packageName.equals(pkg)) {
                        selectedVersion = gv;
                        break;
                    }
                }
            } else if (type.equals("custom")) {
                String dir = prefs.getString("selected_dir", null);
                for (GameVersion gv : customVersions) {
                    if (gv.versionDir.getAbsolutePath().equals(dir)) {
                        selectedVersion = gv;
                        break;
                    }
                }
            }
        }
    }

    public List<GameVersion> getInstalledVersions() {
        return installedVersions;
    }

    public List<GameVersion> getCustomVersions() {
        return customVersions;
    }

    public GameVersion getSelectedVersion() {
        if (selectedVersion != null) return selectedVersion;

        // Try to find a valid version
        if (!installedVersions.isEmpty()) {
            selectVersion(installedVersions.get(0));
            return installedVersions.get(0);
        }
        if (!customVersions.isEmpty()) {
            selectVersion(customVersions.get(0));
            return customVersions.get(0);
        }

        return null; // No versions available
    }

    public void selectVersion(GameVersion version) {
        this.selectedVersion = version;
        SharedPreferences.Editor editor = prefs.edit();
        if (version.isInstalled) {
            editor.putString("selected_type", "installed");
            editor.putString("selected_package", version.packageName);
            editor.remove("selected_dir");
        } else {
            editor.putString("selected_type", "custom");
            editor.putString("selected_dir", version.versionDir.getAbsolutePath());
            editor.remove("selected_package");
        }
        editor.apply();
    }

    public void reload() {
        loadAllVersions();
    }

    static public void attemptRepairLibs(Activity activity, GameVersion version) {
        LibsRepairDialog repairDialog = new LibsRepairDialog(activity);

        VersionManager.LibsRepairCallback callback = new VersionManager.LibsRepairCallback() {
            @Override
            public void onRepairStarted() {
                activity.runOnUiThread(() -> {
                    repairDialog.setTitle(activity.getString(R.string.repair_libs_in_progress));
                    repairDialog.updateProgress(0);
                });
            }

            @Override
            public void onRepairProgress(int progress) {
                activity.runOnUiThread(() -> repairDialog.updateProgress(progress));
            }

            @Override
            public void onRepairCompleted(boolean success) {
                activity.runOnUiThread(() -> {
                    repairDialog.dismiss();
                    if (success) {
                        new CustomAlertDialog(activity)
                                .setTitleText(activity.getString(R.string.repair_completed))
                                .setMessage(activity.getString(R.string.repair_libs_success_message))
                                .setPositiveButton(activity.getString(R.string.confirm), null)
                                .show();
                        VersionManager.get(activity).reload();
                        ((MainActivity) activity).setTextMinecraftVersion();
                    } else {
                        new CustomAlertDialog(activity)
                                .setTitleText(activity.getString(R.string.repair_failed))
                                .setMessage(activity.getString(R.string.repair_libs_failed_message))
                                .setPositiveButton(activity.getString(R.string.confirm), null)
                                .show();
                    }
                });
            }

            @Override
            public void onRepairFailed(Exception e) {
                activity.runOnUiThread(() -> {
                    repairDialog.dismiss();
                    new CustomAlertDialog(activity)
                            .setTitleText(activity.getString(R.string.repair_error))
                            .setMessage(String.format(activity.getString(R.string.repair_libs_error_message), e.getMessage()))
                            .setPositiveButton(activity.getString(R.string.confirm), null)
                            .show();
                });
            }
        };

        new CustomAlertDialog(activity)
                .setTitleText(String.format(activity.getString(R.string.missing_libs_title), version.directoryName))
                .setMessage(activity.getString(R.string.missing_libs_message))
                .setPositiveButton(activity.getString(R.string.repair), v -> {
                    repairDialog.show();
                    VersionManager.get(activity).repairLibsAsync(version.versionDir, version.uuid, callback);
                })
                .setNegativeButton(activity.getString(R.string.cancel), null)
                .show();
    }
}