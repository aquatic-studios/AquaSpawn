package com.aquaticstudios.aquaspawn.utils.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ConfigFile {

    private static final String VERSION_ROUTE = "version";

    private final Plugin plugin;
    private final String name;
    private final File file;
    private FileConfiguration config;

    public ConfigFile(Plugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.file = new File(plugin.getDataFolder(), name);
        synchronize();
        reload();
    }

    public FileConfiguration get() {
        return config;
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + name + ": " + e.getMessage());
        }
    }

    private void synchronize() {
        byte[] defaults = readResource();
        if (defaults == null) {
            if (!file.exists()) {
                plugin.saveResource(name, false);
            }
            return;
        }
        try {
            UpdaterSettings.Builder updater = UpdaterSettings.builder().setKeepAll(true);
            if (hasVersion(defaults)) {
                updater.setVersioning(new BasicVersioning(VERSION_ROUTE));
            }
            YamlDocument document = YamlDocument.create(
                    file,
                    new ByteArrayInputStream(defaults),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    updater.build());
            document.save();
        } catch (IOException e) {
            plugin.getLogger().severe("Could not update " + name + ": " + e.getMessage());
            if (!file.exists()) {
                plugin.saveResource(name, false);
            }
        }
    }

    private byte[] readResource() {
        try (InputStream in = plugin.getResource(name)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean hasVersion(byte[] defaults) {
        for (String line : new String(defaults, StandardCharsets.UTF_8).split("\n")) {
            if (line.startsWith(VERSION_ROUTE + ":")) {
                return true;
            }
        }
        return false;
    }
}
