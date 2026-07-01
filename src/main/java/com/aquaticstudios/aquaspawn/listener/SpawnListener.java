package com.aquaticstudios.aquaspawn.listener;

import com.aquaticstudios.aquaspawn.config.ConfigFile;
import com.aquaticstudios.aquaspawn.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

public final class SpawnListener implements Listener {

    private final Plugin plugin;
    private final ConfigFile config;
    private final ConfigFile menu;

    public SpawnListener(Plugin plugin, ConfigFile config, ConfigFile menu) {
        this.plugin = plugin;
        this.config = config;
        this.menu = menu;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String type = config.get().getString("settings.type-spawn", "force").toLowerCase(Locale.ROOT);
        boolean firstJoin = !player.hasPlayedBefore();

        if (type.equals("custom")) {
            String target = firstJoin
                    ? config.get().getString("settings.custom.first")
                    : config.get().getString("settings.custom.force");
            if (!isDisabled(target)) {
                teleportToSpawn(player, target);
            }
            return;
        }

        if (type.equals("first") && !firstJoin) {
            return;
        }
        String spawnName = config.get().getString("settings.join-spawn", "");
        if (!isDisabled(spawnName)) {
            teleportToSpawn(player, spawnName);
        }
    }

    private void teleportToSpawn(Player player, String spawnName) {
        String path = "items." + spawnName + ".";
        String worldName = menu.get().getString(path + "world");
        String cord = menu.get().getString(path + "cord");
        if (worldName == null || cord == null) {
            plugin.getLogger().warning("Join spawn '" + spawnName + "' has no world/cord data in menu.yml");
            return;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' for spawn '" + spawnName + "' is not loaded");
            return;
        }
        String[] parts = cord.split(",");
        if (parts.length < 3) {
            plugin.getLogger().warning("Invalid coordinates for spawn '" + spawnName + "' in menu.yml");
            return;
        }
        try {
            double x = Double.parseDouble(parts[0].trim());
            double y = Double.parseDouble(parts[1].trim());
            double z = Double.parseDouble(parts[2].trim());
            float yaw = parts.length > 3 ? Float.parseFloat(parts[3].trim()) : 0.0F;
            float pitch = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0.0F;
            Scheduler.teleport(plugin, player, new Location(world, x, y, z, yaw, pitch));
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Could not parse coordinates for spawn '" + spawnName + "'");
        }
    }

    private static boolean isDisabled(String value) {
        if (value == null) {
            return true;
        }
        String v = value.trim();
        return v.isEmpty()
                || v.equalsIgnoreCase("none")
                || v.equalsIgnoreCase("null")
                || v.equalsIgnoreCase("false");
    }
}
