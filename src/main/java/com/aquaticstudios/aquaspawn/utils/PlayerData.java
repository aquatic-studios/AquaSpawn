package com.aquaticstudios.aquaspawn.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent player bookkeeping stored in {@code data.yml}.
 *
 * <p>Tracks the total number of unique players that have ever joined and, for each of them, the
 * ordinal they were assigned the first time they joined (player #38 keeps being #38 forever).</p>
 */
public final class PlayerData {

    private static final String TOTAL_ROUTE = "total-players";
    private static final String PLAYERS_ROUTE = "players.";

    private final Plugin plugin;
    private final File file;
    private FileConfiguration data;

    /** UUIDs registered for the first time this session, pending consumption by the join handler. */
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

    /** @return the total number of unique players that have ever joined. */
    public synchronized int total() {
        return data.getInt(TOTAL_ROUTE, 0);
    }

    /** @return the player's permanent join number, or {@code 0} if they have never joined. */
    public synchronized int number(UUID uuid) {
        return data.getInt(PLAYERS_ROUTE + uuid, 0);
    }

    public synchronized boolean hasJoined(UUID uuid) {
        return data.contains(PLAYERS_ROUTE + uuid);
    }

    /**
     * Assigns and persists the player's number on their very first join. Idempotent: returning
     * players keep the number they already have.
     *
     * @return the player's permanent number
     */
    public synchronized int register(UUID uuid) {
        String route = PLAYERS_ROUTE + uuid;
        if (data.contains(route)) {
            return data.getInt(route);
        }
        int assigned = data.getInt(TOTAL_ROUTE, 0) + 1;
        data.set(TOTAL_ROUTE, assigned);
        data.set(route, assigned);
        firstJoins.add(uuid);
        save();
        return assigned;
    }

    /**
     * Returns whether this player joined for the very first time this session, clearing the flag.
     * Set by {@link #register(UUID)}, consumed once by the join handler.
     */
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
