package com.aquaticstudios.aquaspawn.data;

import com.aquaticstudios.aquaspawn.scheduler.Scheduler;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlayerData {

    private static final Pattern TOTAL = Pattern.compile("\"total-players\"\\s*:\\s*(\\d+)");
    private static final Pattern JOINED_ARRAY = Pattern.compile("\"joined\"\\s*:\\s*\\[([^]]*)]", Pattern.DOTALL);
    private static final Pattern LEGACY_ARRAY = Pattern.compile("\"legacy\"\\s*:\\s*\\[([^]]*)]", Pattern.DOTALL);
    private static final Pattern TOKEN = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern UUID_TOKEN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final long SAVE_INTERVAL_TICKS = 200L;

    private final Plugin plugin;
    private final File file;
    private final Object saveLock = new Object();

    private int total;
    private final Set<String> joined = new LinkedHashSet<>();
    private final Set<String> legacy = new LinkedHashSet<>();
    private volatile boolean dirty;

    public PlayerData(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.json");
        load();
        Scheduler.runAsyncTimer(plugin, this::flushIfDirty, SAVE_INTERVAL_TICKS);
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
            Matcher joinedMatcher = JOINED_ARRAY.matcher(content);
            if (joinedMatcher.find()) {
                collect(joinedMatcher.group(1));
            }
            Matcher legacyMatcher = LEGACY_ARRAY.matcher(content);
            if (legacyMatcher.find()) {
                collect(legacyMatcher.group(1));
            }
        } catch (IOException | NumberFormatException e) {
            plugin.getLogger().severe("Could not read data.json: " + e.getMessage());
        }
    }

    private void collect(String array) {
        Matcher token = TOKEN.matcher(array);
        while (token.find()) {
            String value = token.group(1).toLowerCase(Locale.ROOT);
            if (UUID_TOKEN.matcher(value).matches()) {
                joined.add(value);
            } else if (!value.isEmpty()) {
                legacy.add(value);
            }
        }
    }

    public synchronized int total() {
        return total;
    }

    public synchronized boolean registerFirstJoin(UUID uuid, String name) {
        String id = uuid.toString().toLowerCase(Locale.ROOT);
        if (joined.contains(id)) {
            return false;
        }
        boolean migrated = name != null && legacy.remove(name.toLowerCase(Locale.ROOT));
        joined.add(id);
        if (!migrated) {
            total++;
        }
        dirty = true;
        return !migrated;
    }

    public synchronized void reset() {
        total = 0;
        joined.clear();
        legacy.clear();
        dirty = true;
    }

    public void flush() {
        String snapshot;
        synchronized (this) {
            snapshot = serialize();
            dirty = false;
        }
        write(snapshot);
    }

    private void flushIfDirty() {
        String snapshot;
        synchronized (this) {
            if (!dirty) {
                return;
            }
            snapshot = serialize();
            dirty = false;
        }
        if (!write(snapshot)) {
            dirty = true;
        }
    }

    private String serialize() {
        StringBuilder json = new StringBuilder(64 + (joined.size() + legacy.size()) * 40);
        json.append("{\n  \"total-players\": ").append(total);
        json.append(",\n  \"joined\": ");
        appendArray(json, joined);
        if (!legacy.isEmpty()) {
            json.append(",\n  \"legacy\": ");
            appendArray(json, legacy);
        }
        json.append("\n}\n");
        return json.toString();
    }

    private static void appendArray(StringBuilder json, Set<String> values) {
        if (values.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[");
        boolean first = true;
        for (String value : values) {
            json.append(first ? "\n    \"" : ",\n    \"").append(value).append('"');
            first = false;
        }
        json.append("\n  ]");
    }

    private boolean write(String snapshot) {
        synchronized (saveLock) {
            try {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                Path target = file.toPath();
                Path temp = new File(parent, file.getName() + ".tmp").toPath();
                Files.write(temp, snapshot.getBytes(StandardCharsets.UTF_8));
                try {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save data.json: " + e.getMessage());
                return false;
            }
        }
    }
}
