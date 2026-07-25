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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A YAML config that is kept in sync with the packaged defaults.
 *
 * <p>On load, BoostedYAML merges the bundled resource into the file on disk: it adds any new
 * options, restores dropped comments and bumps the {@code version} field, while
 * {@link UpdaterSettings.Builder#setKeepAll(boolean) keepAll} preserves everything the user or
 * the plugin added at runtime (e.g. created spawns in {@code menu.yml}). The rest of the plugin
 * keeps reading through Bukkit's {@link FileConfiguration}, so nothing else has to change.</p>
 */
public final class ConfigFile {

    /** Route of the schema version field used for migrations. */
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

    /** Creates the file from defaults if missing, then merges/updates it via BoostedYAML. */
    private void synchronize() {
        InputStream defaults = plugin.getResource(name);
        if (defaults == null) {
            // no packaged default to merge against; fall back to a plain copy.
            if (!file.exists()) {
                plugin.saveResource(name, false);
            }
            return;
        }
        try {
            YamlDocument document = YamlDocument.create(
                    file,
                    defaults,
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder()
                            .setVersioning(new BasicVersioning(VERSION_ROUTE))
                            .setKeepAll(true)
                            .build());
            document.save();
        } catch (IOException e) {
            plugin.getLogger().severe("Could not update " + name + ": " + e.getMessage());
            if (!file.exists()) {
                plugin.saveResource(name, false);
            }
        }
    }
}
