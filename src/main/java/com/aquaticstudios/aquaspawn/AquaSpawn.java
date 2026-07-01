package com.aquaticstudios.aquaspawn;

import com.aquaticstudios.aquaspawn.command.AquaCommand;
import com.aquaticstudios.aquaspawn.command.SpawnCommand;
import com.aquaticstudios.aquaspawn.config.ConfigFile;
import com.aquaticstudios.aquaspawn.config.Messages;
import com.aquaticstudios.aquaspawn.listener.DefaultListener;
import com.aquaticstudios.aquaspawn.listener.JoinListener;
import com.aquaticstudios.aquaspawn.listener.SpawnListener;
import com.aquaticstudios.aquaspawn.menu.MenuListener;
import com.aquaticstudios.aquaspawn.menu.MenuManager;
import com.aquaticstudios.aquaspawn.util.Scheduler;
import com.github.senkex.headrender.HeadRender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AquaSpawn extends JavaPlugin {

    @Override
    public void onEnable() {
        ConfigFile config = new ConfigFile(this, "config.yml");
        ConfigFile menuFile = new ConfigFile(this, "menu.yml");
        ConfigFile messagesFile = new ConfigFile(this, "messages.yml");

        Messages messages = new Messages(messagesFile);
        MenuManager menu = new MenuManager(this, menuFile);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(menu), this);
        pm.registerEvents(new JoinListener(this, config), this);
        pm.registerEvents(new SpawnListener(this, config, menuFile), this);
        pm.registerEvents(new DefaultListener(config), this);

        AquaCommand command = new AquaCommand(this, config, menuFile, messagesFile, messages, menu);
        PluginCommand pluginCommand = getCommand("aquaspawn");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        PluginCommand spawnCommand = getCommand("spawn");
        if (spawnCommand != null) {
            spawnCommand.setExecutor(new SpawnCommand(this, config, menuFile, messages));
        }

        getLogger().info("AquaSpawn v" + getDescription().getVersion() + " enabled ("
                + (Scheduler.isFolia() ? "Folia" : "Bukkit") + " scheduler).");
    }

    @Override
    public void onDisable() {
        try {
            HeadRender.shutdown();
        } catch (Throwable ignored) {
        }
    }
}
