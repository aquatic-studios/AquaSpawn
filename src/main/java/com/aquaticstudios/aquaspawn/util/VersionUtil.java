package com.aquaticstudios.aquaspawn.util;
import org.bukkit.Bukkit;

public final class VersionUtil {

    private static final String VERSION =
            Bukkit.getBukkitVersion().split("-")[0];

    public static String getVersion() {
        return VERSION;
    }

}