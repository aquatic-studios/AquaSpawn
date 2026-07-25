package com.aquaticstudios.aquaspawn.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class Placeholders {

    private static final boolean ENABLED = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

    public static String apply(Player player, String text) {
        if (!ENABLED || player == null || text == null || text.indexOf('%') < 0) {
            return text;
        }
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
    }
}
