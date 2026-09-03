package com.aquaticstudios.aquaspawn.placeholder;

import com.aquaticstudios.aquaspawn.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Placeholders {

    private static final Pattern TOKEN = Pattern.compile("%([a-zA-Z0-9_]+)%");

    private static PlayerData data;
    private static boolean papi;

    public static void init(PlayerData playerData) {
        data = playerData;
        papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public static String apply(Player player, String text) {
        if (text == null || text.indexOf('%') < 0) {
            return text;
        }
        String out = text;
        if (papi && player != null) {
            try {
                out = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, out);
            } catch (Throwable ignored) {
            }
        }
        return fallback(player, out);
    }

    private static String fallback(Player player, String text) {
        if (text.indexOf('%') < 0) {
            return text;
        }
        Matcher matcher = TOKEN.matcher(text);
        StringBuffer out = new StringBuffer(text.length());
        boolean hit = false;
        while (matcher.find()) {
            String value = resolve(player, matcher.group(1).toLowerCase(Locale.ROOT));
            if (value == null) {
                continue;
            }
            hit = true;
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        if (!hit) {
            return text;
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String resolve(Player player, String key) {
        switch (key) {
            case "player":
            case "player_name":
            case "aquaspawn_player":
                return player == null ? null : player.getName();
            case "player_displayname":
                return player == null ? null : player.getDisplayName();
            case "player_uuid":
                return player == null ? null : player.getUniqueId().toString();
            case "player_world":
                return player == null ? null : player.getWorld().getName();
            case "aquaspawn_total_players":
                return data == null ? null : String.valueOf(data.total());
            case "server_online":
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            case "server_max_players":
                return String.valueOf(Bukkit.getMaxPlayers());
            default:
                return null;
        }
    }
}
