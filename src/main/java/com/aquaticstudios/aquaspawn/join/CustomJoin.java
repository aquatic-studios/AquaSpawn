package com.aquaticstudios.aquaspawn.join;

import com.aquaticstudios.aquaspawn.action.Channels;
import com.aquaticstudios.aquaspawn.action.Conditions;
import com.aquaticstudios.aquaspawn.menu.ActionHandler;
import com.aquaticstudios.aquaspawn.utils.config.ConfigFile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class CustomJoin {

    private final ConfigFile config;
    private final Channels channels;
    private final ActionHandler actions;
    private final Conditions conditions;

    public CustomJoin(Plugin plugin, ConfigFile config) {
        this.config = config;
        this.channels = new Channels(plugin);
        this.actions = new ActionHandler(plugin);
        this.conditions = new Conditions();
    }

    public void handle(Player player, boolean firstJoin) {
        ConfigurationSection root = config.get().getConfigurationSection("join");
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
            ConfigurationSection motd = root.getConfigurationSection("join-motd");
            if (motd != null && motd.getBoolean("enabled", true)) {
                channels.play(player, motd);
            }
        }

        runActions(player, root.getConfigurationSection("actions"));
    }

    private void runActions(Player player, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        List<String> disabledWorlds = section.getStringList("disable-worlds");
        if (disabledWorlds.contains(player.getWorld().getName())) {
            return;
        }
        if (!conditions.allMatch(player, section.getStringList("conditions"))) {
            return;
        }
        actions.run(player, section.getStringList("run"));
    }
}
