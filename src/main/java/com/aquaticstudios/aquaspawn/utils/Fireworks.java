package com.aquaticstudios.aquaspawn.utils;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Fireworks {

    public static void spawn(Plugin plugin, Player player, ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }
        Scheduler.runForEntity(plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();

            FireworkEffect.Builder effect = FireworkEffect.builder()
                    .with(type(section.getString("type", "BALL")))
                    .flicker(section.getBoolean("flicker", false))
                    .trail(section.getBoolean("trail", false));

            List<Color> colors = parseColors(section.getStringList("colors"));
            if (colors.isEmpty()) {
                colors.add(Color.WHITE);
            }
            effect.withColor(colors);

            List<Color> fade = parseColors(section.getStringList("fade-colors"));
            if (!fade.isEmpty()) {
                effect.withFade(fade);
            }

            meta.addEffect(effect.build());
            meta.setPower(Math.max(0, section.getInt("power", 1)));
            firework.setFireworkMeta(meta);
        });
    }

    private static List<Color> parseColors(List<String> raw) {
        List<Color> colors = new ArrayList<>();
        for (String value : raw) {
            Color color = color(value);
            if (color != null) {
                colors.add(color);
            }
        }
        return colors;
    }

    private static Color color(String value) {
        if (value == null) {
            return null;
        }
        String hex = value.trim().replace("#", "").replace("&", "");
        try {
            return Color.fromRGB(Integer.parseInt(hex, 16));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static FireworkEffect.Type type(String value) {
        try {
            return FireworkEffect.Type.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            return FireworkEffect.Type.BALL;
        }
    }
}
