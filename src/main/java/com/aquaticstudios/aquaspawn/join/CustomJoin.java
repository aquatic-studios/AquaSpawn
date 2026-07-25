package com.aquaticstudios.aquaspawn.join;

import com.aquaticstudios.aquaspawn.action.Channels;
import com.aquaticstudios.aquaspawn.menu.ActionHandler;
import com.aquaticstudios.aquaspawn.utils.config.ConfigFile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class CustomJoin {

    private final ConfigFile config;
    private final Channels channels;
    private final ActionHandler actions;

    public CustomJoin(Plugin plugin, ConfigFile config) {
        this.config = config;
        this.channels = new Channels(plugin);
        this.actions = new ActionHandler(plugin);
    }

    public void handle(Player player, boolean firstJoin) {
        ConfigurationSection root = config.get().getConfigurationSection("custom-events");
        if (root == null || !root.getBoolean("enabled", true)) {
            return;
        }

        boolean handledByFirstJoin = false;
        if (firstJoin) {
            ConfigurationSection section = root.getConfigurationSection("first-join");
            if (section != null && section.getBoolean("enabled", true)) {
                channels.play(player, section);
                handledByFirstJoin = section.getBoolean("override", true);
            }
        }

        if (!handledByFirstJoin) {
            ConfigurationSection join = root.getConfigurationSection("join");
            if (join != null && join.getBoolean("enabled", true)) {
                channels.play(player, join);
            }
        }

        actions.run(player, root.getStringList("actions"));
    }
}
