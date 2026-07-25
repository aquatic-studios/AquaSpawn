package com.aquaticstudios.aquaspawn.menu;

import com.aquaticstudios.aquaspawn.utils.CC;
import com.aquaticstudios.aquaspawn.utils.Fireworks;
import com.aquaticstudios.aquaspawn.placeholder.Placeholders;
import com.aquaticstudios.aquaspawn.scheduler.Scheduler;
import com.aquaticstudios.aquaspawn.utils.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;

public final class ActionHandler {

    private final Plugin plugin;

    public ActionHandler(Plugin plugin) {
        this.plugin = plugin;
    }

    public void run(Player player, List<String> actions) {
        if (actions == null) {
            return;
        }
        for (String action : actions) {
            handle(player, action.trim());
        }
    }

    private void handle(Player player, String action) {
        if (action.isEmpty()) {
            return;
        }
        int end = action.indexOf(']');
        if (!action.startsWith("[") || end < 0) {
            return;
        }
        String type = action.substring(1, end).trim().toLowerCase();
        String data = action.substring(end + 1).trim();

        switch (type) {
            case "close":
                Scheduler.runForEntityLater(plugin, player, player::closeInventory, 1L);
                break;
            case "message":
                CC.send(player, data);
                break;
            case "actionbar":
                CC.sendActionBar(player, data);
                break;
            case "title":
                title(player, data);
                break;
            case "command":
            case "player":
                Scheduler.runForEntity(plugin, player, () ->
                        Bukkit.dispatchCommand(player, resolveCommand(player, data)));
                break;
            case "console":
                Scheduler.runForEntity(plugin, player, () ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolveCommand(player, data)));
                break;
            case "sound":
                Sounds.play(player, data);
                break;
            case "effect":
                effect(player, data);
                break;
            case "firework":
                Fireworks.spawn(plugin, player, null);
                break;
            case "teleport":
                teleport(player, data);
                break;
            default:
                plugin.getLogger().warning("Unknown action: [" + type + "]");
        }
    }

    private String resolveCommand(Player player, String data) {
        return Placeholders.apply(player, data).replace("%player%", player.getName());
    }

    private void title(Player player, String data) {
        String[] parts = data.split(";", 5);
        int fadeIn = parts.length > 0 ? parseInt(parts[0], 10) : 10;
        int stay = parts.length > 1 ? parseInt(parts[1], 40) : 40;
        int fadeOut = parts.length > 2 ? parseInt(parts[2], 10) : 10;
        String title = parts.length > 3 ? parts[3] : "";
        String subtitle = parts.length > 4 ? parts[4] : "";
        CC.sendTitle(player, fadeIn, stay, fadeOut, title, subtitle);
    }

    private void effect(Player player, String data) {
        String[] parts = data.split(":");
        PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase(Locale.ROOT));
        if (type == null) {
            plugin.getLogger().warning("Unknown potion effect: " + parts[0]);
            return;
        }
        int amplifier = parts.length > 1 ? parseInt(parts[1], 0) : 0;
        int duration = parts.length > 2 ? parseInt(parts[2], 0) * 20 : Integer.MAX_VALUE;
        Scheduler.runForEntity(plugin, player, () ->
                player.addPotionEffect(new PotionEffect(type, duration, amplifier)));
    }

    private void teleport(Player player, String data) {
        String[] parts = data.split(",");
        if (parts.length < 3) {
            return;
        }
        try {
            double x = Double.parseDouble(parts[0].trim());
            double y = Double.parseDouble(parts[1].trim());
            double z = Double.parseDouble(parts[2].trim());
            float yaw = parts.length > 3 ? parseFloat(parts[3].trim(), 0.0F) : 0.0F;
            float pitch = parts.length > 4 ? parseFloat(parts[4].trim(), 0.0F) : 0.0F;
            Location target = new Location(player.getWorld(), x, y, z, yaw, pitch);
            Scheduler.teleport(plugin, player, target);
        } catch (NumberFormatException ignored) {
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
