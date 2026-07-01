package com.aquaticstudios.aquaspawn.command;

import com.aquaticstudios.aquaspawn.config.ConfigFile;
import com.aquaticstudios.aquaspawn.config.Messages;
import com.aquaticstudios.aquaspawn.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpawnCommand implements CommandExecutor {

    private final Plugin plugin;
    private final ConfigFile config;
    private final ConfigFile menuFile;
    private final Messages messages;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SpawnCommand(Plugin plugin, ConfigFile config, ConfigFile menuFile, Messages messages) {
        this.plugin = plugin;
        this.config = config;
        this.menuFile = menuFile;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messages.send(sender, "players-only");
            return true;
        }
        Player player = (Player) sender;

        String name = config.get().getString("settings.join-spawn", "");
        if (name == null || name.isEmpty()) {
            messages.send(player, "spawn-not-set");
            return true;
        }

        String path = "items." + name + ".";
        String worldName = menuFile.get().getString(path + "world");
        String cord = menuFile.get().getString(path + "cord");
        if (worldName == null || cord == null) {
            messages.send(player, "spawn-not-set");
            return true;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            messages.send(player, "spawn-world-missing");
            return true;
        }

        String[] parts = cord.split(",");
        if (parts.length < 3) {
            messages.send(player, "spawn-not-set");
            return true;
        }

        int cooldown = config.get().getInt("spawn.cooldown", 3);
        if (cooldown > 0 && !player.hasPermission("aquaspawn.spawn.bypass")) {
            long now = System.currentTimeMillis();
            Long last = cooldowns.get(player.getUniqueId());
            if (last != null) {
                long remaining = (cooldown * 1000L) - (now - last);
                if (remaining > 0) {
                    long seconds = (remaining + 999L) / 1000L;
                    messages.send(player, "spawn-cooldown", "%time%", String.valueOf(seconds));
                    return true;
                }
            }
        }

        try {
            double x = Double.parseDouble(parts[0].trim());
            double y = Double.parseDouble(parts[1].trim());
            double z = Double.parseDouble(parts[2].trim());
            float yaw = parts.length > 3 ? Float.parseFloat(parts[3].trim()) : 0.0F;
            float pitch = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0.0F;
            Scheduler.teleport(plugin, player, new Location(world, x, y, z, yaw, pitch));
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            messages.send(player, "spawn-teleported");
        } catch (NumberFormatException e) {
            messages.send(player, "spawn-not-set");
        }
        return true;
    }
}
