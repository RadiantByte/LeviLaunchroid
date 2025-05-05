package org.levimc.launcher.ui.dialogs.gameversionselect;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

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
        LinkedHashMap<String, VersionGroup> map = new LinkedHashMap<>();
        for (GameVersion gv : list) {
            VersionGroup vg = map.get(gv.versionCode);
            if (vg == null) {
                vg = new VersionGroup(gv.versionCode);
                map.put(gv.versionCode, vg);
            }
            vg.versions.add(gv);
        }
        return map;
    }
}