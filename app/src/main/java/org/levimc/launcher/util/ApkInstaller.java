package org.levimc.launcher.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ApkInstaller {

    public interface InstallCallback {
        void onProgress(int progress);
        void onSuccess(String versionName);
        void onError(String errorMessage);
    }

    private static final String APK_FILE_NAME = "base.apk.levi";

    private final Context context;
    private final ExecutorService executor;
    private final InstallCallback callback;

    public ApkInstaller(Context context, ExecutorService executor, InstallCallback callback) {
        this.context = context;
        this.executor = executor;
        this.callback = callback;
    }

    public static String abiToSystemLibDir(String abi){
        if(abi == null) return "unknown";
        switch (abi) {
            case "armeabi-v7a": return "arm";
            case "arm64-v8a": return "arm64";
            default: return abi;
        }
    }

    private void unzipLibsToSystemAbi(File baseDir, ZipInputStream zis) throws IOException {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            String name = entry.getName();
            if (name.startsWith("lib/") && !entry.isDirectory()) {
                String[] parts = name.split("/");
                if(parts.length < 3) continue;
                String abi = parts[1];
                String systemAbi = abiToSystemLibDir(abi);
                String newName = "lib/" + systemAbi + "/" + parts[2];
                File outFile = new File(baseDir, newName);
                File parent = outFile.getParentFile();
                if (!parent.exists()) parent.mkdirs();
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
            zis.closeEntry();
        }
    }

    public void install(final Uri apkOrApksUri, final String dirName) {
        executor.submit(() -> {
            try {
                String versionName = extractVersionName(apkOrApksUri);

                File baseDir = new File(Environment.getExternalStorageDirectory(), "games/org.levimc/minecraft/" + dirName);
                if (!baseDir.exists() && !baseDir.mkdirs()) {
                    postError("无法创建目录: " + baseDir.getAbsolutePath());
                    return;
                }

                String fileName = getFileName(apkOrApksUri);
                if (fileName != null && fileName.toLowerCase().endsWith(".apks")) {
                    boolean foundApk = false;
                    try (InputStream is = context.getContentResolver().openInputStream(apkOrApksUri);
                         ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
                        ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null) {
                            String outputName;
                            if (entry.isDirectory()) {
                                outputName = entry.getName();
                                File outDir = new File(baseDir, outputName);
                                outDir.mkdirs();
                                zis.closeEntry();
                                continue;
                            } else {
                                // base.apk特判
                                if (entry.getName().equals("base.apk")) {
                                    outputName = APK_FILE_NAME;
                                } else {
                                    outputName = entry.getName();
                                }
                            }
                            File outFile = new File(baseDir, outputName);
                            File parent = outFile.getParentFile();
                            if (!parent.exists()) parent.mkdirs();
                            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                byte[] buffer = new byte[8192];
                                int len;
                                while ((len = zis.read(buffer)) != -1) {
                                    fos.write(buffer, 0, len);
                                }
                            }

                            if (outputName.equals(APK_FILE_NAME)) {
                                // base.apk.levi，立即解libs
                                try (InputStream is2 = new FileInputStream(outFile);
                                     ZipInputStream zis2 = new ZipInputStream(new BufferedInputStream(is2))) {
                                    unzipLibsToSystemAbi(baseDir, zis2);
                                }
                                foundApk = true;
                            } else if (outputName.endsWith(".apk")) {
                                foundApk = true;
                            }
                            zis.closeEntry();
                        }
                    }
                    if (!foundApk) {
                        postError("apks文件中找不到任何apk");
                        return;
                    }
                } else {
                    File dstApkFile = new File(baseDir, APK_FILE_NAME);
                    try (InputStream is = context.getContentResolver().openInputStream(apkOrApksUri);
                         OutputStream os = new FileOutputStream(dstApkFile)) {
                        if (is == null) {
                            postError("打开apk失败");
                            return;
                        }
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                    try (InputStream is2 = new FileInputStream(dstApkFile);
                         ZipInputStream zis2 = new ZipInputStream(new BufferedInputStream(is2))) {
                        unzipLibsToSystemAbi(baseDir, zis2);
                    }
                    new File(baseDir, "oat").mkdirs();
                }

                JSONObject json = new JSONObject();
                json.put("name", dirName);
                json.put("uuid", UUID.randomUUID().toString());
                json.put("version", versionName);

                File versionJson = new File(baseDir, "version.json");
                try (FileWriter writer = new FileWriter(versionJson)) {
                    writer.write(json.toString(4));
                }
                postSuccess(versionName);
            } catch (Exception e) {
                postError("安装失败：" + e.getMessage());
            }
        });
    }

    private void postProgress(int progress) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) callback.onProgress(progress);
        });
    }

    private void postSuccess(String versionName) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) callback.onSuccess(versionName);
        });
    }

    private void postError(String error) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) callback.onError(error);
        });
    }

    private String extractVersionName(Uri apkOrApksUri) throws Exception {
        File tempFile = new File(context.getCacheDir(), "temp_apk_" + System.currentTimeMillis() + ".apk");
        InputStream is = null;
        OutputStream os = null;
        try {
            String fileName = getFileName(apkOrApksUri);
            if (fileName != null && fileName.toLowerCase().endsWith(".apks")) {
                try (InputStream apksIs = context.getContentResolver().openInputStream(apkOrApksUri);
                     ZipInputStream zis = new ZipInputStream(new BufferedInputStream(apksIs))) {
                    ZipEntry entry;
                    boolean found = false;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory() && entry.getName().endsWith(".apk")) {
                            if (entry.getName().equals("base.apk") || !found) {
                                os = new FileOutputStream(tempFile);
                                byte[] buffer = new byte[8192];
                                int len;
                                while ((len = zis.read(buffer)) != -1) {
                                    os.write(buffer, 0, len);
                                }
                                os.close();
                                found = true;
                                if (entry.getName().equals("base.apk")) break;
                            }
                        }
                        zis.closeEntry();
                    }
                    if (!found) throw new FileNotFoundException("apks 包中没有 base.apk!");
                }
            } else {
                is = context.getContentResolver().openInputStream(apkOrApksUri);
                if (is == null) throw new FileNotFoundException("打开apk失败");
                os = new FileOutputStream(tempFile);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                os.close();
            }

            try (ApkFile apkFile = new ApkFile(tempFile)) {
                ApkMeta apkMeta = apkFile.getApkMeta();
                String pkgName = apkMeta.getPackageName();
                String vName = apkMeta.getVersionName();
                if ("com.mojang.minecraftpe".equals(pkgName) && vName != null && !vName.isEmpty()) {
                    return vName;
                }
            }
        } finally {
            if (is != null) try { is.close(); } catch (Throwable ignored) {}
            if (os != null) try { os.close(); } catch (Throwable ignored) {}
            tempFile.delete();
        }
        return "unknown_version";
    }

    private String getFileName(Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        String result = null;
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1 && cursor.moveToFirst()) {
                result = cursor.getString(nameIndex);
            }
            cursor.close();
        }
        return result;
    }
}