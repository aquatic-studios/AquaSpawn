package com.aquaticstudios.aquaspawn.utils;

import com.aquaticstudios.aquaspawn.AquaSpawn;
import com.aquaticstudios.aquaspawn.scheduler.Scheduler;
import com.aquaticstudios.aquaspawn.utils.config.ConfigFile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class UpdateChecker {

    private static final String PERMISSION = "aquaspawn.update";
    private static final int RESOURCE_ID = 136871;
    private static final String API = "https://api.spigotmc.org/legacy/update.php?resource=" + RESOURCE_ID;
    private static final String PAGE = "https://www.spigotmc.org/resources/" + RESOURCE_ID + "/";

    private final AquaSpawn plugin;
    private final ConfigFile config;

    private volatile String latest;

    public UpdateChecker(AquaSpawn plugin, ConfigFile config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void check() {
        if (!enabled()) {
            return;
        }
        Scheduler.runAsync(plugin, () -> {
            String version = fetch();
            if (version == null || version.isEmpty() || version.equalsIgnoreCase(current())) {
                return;
            }
            this.latest = version;
            CC.send(Bukkit.getConsoleSender(),
                    "&#54ADF4[" + name() + "] &fA newer version is available: &#54ADF4" + version
                            + " &f(you are on &#FF3F3F" + current() + "&f)");
        });
    }

    public void announce(Player player) {
        String version = latest;
        if (version == null || !enabled() || !Permissions.has(player, PERMISSION)) {
            return;
        }
        CC.send(player, "");
        CC.send(player, "  &#54ADF4&l" + name() + " &fA new version is available!");
        CC.send(player, "  &fYour version: &#FF3F3F" + current() + " &f| Latest: &#54ADF4" + version);
        CC.send(player, "");
        CC.send(player, "  &fDownload it here:");
        CC.send(player, "  &7" + PAGE);
        CC.send(player, "");
    }

    private boolean enabled() {
        return config.get().getBoolean("update-notify", true);
    }

    private String name() {
        return plugin.getDescription().getName();
    }

    private String current() {
        return plugin.getDescription().getVersion();
    }

    private String fetch() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "AquaSpawn-UpdateChecker");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                return line == null ? null : line.trim();
            }
        } catch (IOException ex) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
