package com.aquaticstudios.aquaspawn.util;

import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.Locale;

public final class Items {

    public static Material material(String name) {
        if (name != null && !name.isEmpty()) {
            Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
            if (material != null && material.isItem()) {
                return material;
            }
        }
        return Material.STONE;
    }

    public static Sound sound(String name) {
        try {
            return Sound.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return Sound.ENTITY_PLAYER_LEVELUP;
        }
    }
}
