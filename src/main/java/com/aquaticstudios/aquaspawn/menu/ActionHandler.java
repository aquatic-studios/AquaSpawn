package com.aquaticstudios.aquaspawn.menu;

import com.aquaticstudios.aquaspawn.util.ColorUtils;
import com.aquaticstudios.aquaspawn.util.Items;
import com.aquaticstudios.aquaspawn.util.Placeholders;
import com.aquaticstudios.aquaspawn.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

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
                player.sendMessage(ColorUtils.color(Placeholders.apply(player, data)));
                break;
            case "command":
                String command = Placeholders.apply(player, data).replace("%player%", player.getName());
                Bukkit.dispatchCommand(player, command);
                break;
            case "sound":
                playSound(player, data);
                break;
            case "teleport":
                teleport(player, data);
                break;
            default:
                plugin.getLogger().warning("Unknown menu action: [" + type + "]");
        }
    }

    private void playSound(Player player, String data) {
        String[] parts = data.split(":");
        Sound sound = Items.sound(parts[0]);
        float volume = parts.length > 1 ? parseFloat(parts[1], 1.0F) : 1.0F;
        float pitch = parts.length > 2 ? parseFloat(parts[2], 1.0F) : 1.0F;
        player.playSound(player.getLocation(), sound, volume, pitch);
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
}
