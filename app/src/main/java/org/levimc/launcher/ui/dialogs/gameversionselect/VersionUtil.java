package org.levimc.launcher.ui.dialogs.gameversionselect;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VersionUtil {
    public static List<BigGroup> buildBigGroups(List<GameVersion> installed, List<GameVersion> custom) {
        List<BigGroup> bigGroups = new ArrayList<>();
        if (!installed.isEmpty()) {
            BigGroup bg = new BigGroup(R.string.installed_packages);
            bg.versionGroups.addAll(groupByVersion(installed).values());
            bigGroups.add(bg);
        }
        if (!custom.isEmpty()) {
            BigGroup bg = new BigGroup(R.string.local_custom);
            bg.versionGroups.addAll(groupByVersion(custom).values());
            bigGroups.add(bg);
        }
        return bigGroups;
    }

    public static LinkedHashMap<String, VersionGroup> groupByVersion(List<GameVersion> list) {
        Map<String, VersionGroup> tempMap = new HashMap<>();
        for (GameVersion gv : list) {
            VersionGroup vg = tempMap.get(gv.versionCode);
            if (vg == null) {
                vg = new VersionGroup(gv.versionCode);
                tempMap.put(gv.versionCode, vg);
            }
            vg.versions.add(gv);
        }

        List<String> sortedKeys = new ArrayList<>(tempMap.keySet());
        sortedKeys.sort((a, b) -> compareVersionCode(b, a));

        LinkedHashMap<String, VersionGroup> sortedMap = new LinkedHashMap<>();
        for (String key : sortedKeys) {
            sortedMap.put(key, tempMap.get(key));
        }
        return sortedMap;
    }

    public static int compareVersionCode(String v1, String v2) {
        String[] arr1 = v1.split("\\.");
        String[] arr2 = v2.split("\\.");
        int len = Math.max(arr1.length, arr2.length);
        for (int i = 0; i < len; i++) {
            int n1 = (i < arr1.length) ? Integer.parseInt(arr1[i]) : 0;
            int n2 = (i < arr2.length) ? Integer.parseInt(arr2[i]) : 0;
            if (n1 != n2) return n1 - n2;
        }
        return 0;
    }
}