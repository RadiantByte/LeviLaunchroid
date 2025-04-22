package org.levimc.launcher.util;

import android.content.Context;
import android.net.Uri;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ApkUtils {
    public static String extractMinecraftVersionNameFromUri(Context context, Uri uri) {
        File tempFile = null;
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return "Error Apk";

            tempFile = new File(context.getCacheDir(), "temp_apk_" + System.currentTimeMillis() + ".apk");
            try (OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            }
            try (ApkFile apkFile = new ApkFile(tempFile)) {
                ApkMeta apkMeta = apkFile.getApkMeta();
                String packageName = apkMeta.getPackageName();
                String versionName = apkMeta.getVersionName();

                if ("com.mojang.minecraftpe".equals(packageName) && versionName != null && !versionName.isEmpty()) {
                    return "Minecraft_" + versionName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
        return "Error Apk";
    }
}
