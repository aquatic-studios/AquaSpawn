package com.aquaticstudios.aquaspawn.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class Sounds {

    private Sounds() {
    }

    public static void play(Player player, String data) {
        if (player == null || data == null || data.trim().isEmpty()) {
            return;
        }
        String[] parts = data.split(":");
        Sound sound = Items.sound(parts[0]);
        float volume = parts.length > 1 ? parseFloat(parts[1], 1.0F) : 1.0F;
        float pitch = parts.length > 2 ? parseFloat(parts[2], 1.0F) : 1.0F;
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
