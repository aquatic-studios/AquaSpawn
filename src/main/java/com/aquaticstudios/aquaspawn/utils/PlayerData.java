package com.aquaticstudios.aquaspawn.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerData {

    private static final String TOTAL_ROUTE = "total-players";
    private static final String PLAYERS_ROUTE = "players.";

    private final Plugin plugin;
    private final File file;
    private FileConfiguration data;

    private final Set<UUID> firstJoins = ConcurrentHashMap.newKeySet();

    public PlayerData(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized int total() {
        return data.getInt(TOTAL_ROUTE, 0);
    }

    public synchronized void register(UUID uuid) {
        String route = PLAYERS_ROUTE + uuid;
        if (data.contains(route)) {
            return;
        }
        data.set(TOTAL_ROUTE, data.getInt(TOTAL_ROUTE, 0) + 1);
        data.set(route, true);
        firstJoins.add(uuid);
        save();
    }

    public boolean consumeFirstJoin(UUID uuid) {
        return firstJoins.remove(uuid);
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }
}
