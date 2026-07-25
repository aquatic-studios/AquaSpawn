package com.aquaticstudios.aquaspawn.data;

import com.aquaticstudios.aquaspawn.scheduler.Scheduler;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlayerData {

    private static final Pattern TOTAL = Pattern.compile("\"total-players\"\\s*:\\s*(\\d+)");
    private static final Pattern JOINED_ARRAY = Pattern.compile("\"joined\"\\s*:\\s*\\[([^]]*)]", Pattern.DOTALL);
    private static final Pattern NAME = Pattern.compile("\"([a-zA-Z0-9_]{1,16})\"");

    private final Plugin plugin;
    private final File file;
    private final Object saveLock = new Object();

    private int total;
    private final Set<String> joined = new LinkedHashSet<>();

    public PlayerData(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.json");
        load();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Matcher totalMatcher = TOTAL.matcher(content);
            if (totalMatcher.find()) {
                total = Integer.parseInt(totalMatcher.group(1));
            }
            Matcher arrayMatcher = JOINED_ARRAY.matcher(content);
            if (arrayMatcher.find()) {
                Matcher nameMatcher = NAME.matcher(arrayMatcher.group(1));
                while (nameMatcher.find()) {
                    joined.add(nameMatcher.group(1).toLowerCase(Locale.ROOT));
                }
            }
        } catch (IOException | NumberFormatException e) {
            plugin.getLogger().severe("Could not read data.json: " + e.getMessage());
        }
    }

    public synchronized int total() {
        return total;
    }

    public synchronized boolean registerFirstJoin(String name) {
        if (!joined.add(name.toLowerCase(Locale.ROOT))) {
            return false;
        }
        total++;
        saveAsync(serialize());
        return true;
    }

    public synchronized void reset() {
        total = 0;
        joined.clear();
        saveAsync(serialize());
    }

    public void flush() {
        String snapshot;
        synchronized (this) {
            snapshot = serialize();
        }
        write(snapshot);
    }

    private String serialize() {
        StringBuilder json = new StringBuilder(64 + joined.size() * 16);
        json.append("{\n  \"total-players\": ").append(total).append(",\n  \"joined\": [");
        boolean first = true;
        for (String name : joined) {
            json.append(first ? "\n    \"" : ",\n    \"").append(name).append('"');
            first = false;
        }
        json.append(joined.isEmpty() ? "]\n}\n" : "\n  ]\n}\n");
        return json.toString();
    }

    private void saveAsync(String snapshot) {
        Scheduler.runAsync(plugin, () -> write(snapshot));
    }

    private void write(String snapshot) {
        synchronized (saveLock) {
            try {
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                Files.write(file.toPath(), snapshot.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save data.json: " + e.getMessage());
            }
        }
    }
}
