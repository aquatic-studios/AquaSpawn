package com.aquaticstudios.aquaspawn;

import com.aquaticstudios.aquaspawn.command.AquaCommand;
import com.aquaticstudios.aquaspawn.command.CommandRegistry;
import com.aquaticstudios.aquaspawn.command.SpawnCommand;
import com.aquaticstudios.aquaspawn.utils.config.ConfigFile;
import com.aquaticstudios.aquaspawn.utils.config.Messages;
import com.aquaticstudios.aquaspawn.listener.DefaultListener;
import com.aquaticstudios.aquaspawn.listener.JoinListener;
import com.aquaticstudios.aquaspawn.listener.SpawnListener;
import com.aquaticstudios.aquaspawn.menu.MenuListener;
import com.aquaticstudios.aquaspawn.menu.MenuManager;
import com.aquaticstudios.aquaspawn.placeholder.AquaExpansion;
import com.aquaticstudios.aquaspawn.placeholder.Placeholders;
import com.aquaticstudios.aquaspawn.utils.CC;
import com.aquaticstudios.aquaspawn.utils.Metrics;
import com.aquaticstudios.aquaspawn.utils.UpdateChecker;
import com.aquaticstudios.aquaspawn.data.PlayerData;
import com.aquaticstudios.aquaspawn.scheduler.Scheduler;
import com.github.senkex.headrender.HeadRender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;

public final class AquaSpawn extends JavaPlugin {

    private PlayerData playerData;

    @Override
    public void onEnable() {
        int pluginId = 32483;
        new Metrics(this, pluginId);

        CC.init(this);

        ConfigFile config = new ConfigFile(this, "config.yml");
        ConfigFile menuFile = new ConfigFile(this, "menu.yml");
        ConfigFile messagesFile = new ConfigFile(this, "messages.yml");

        Messages messages = new Messages(messagesFile);
        MenuManager menu = new MenuManager(this, menuFile);
        playerData = new PlayerData(this);
        Placeholders.init(playerData);

        UpdateChecker updateChecker = new UpdateChecker(this, config);
        updateChecker.check();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(menu), this);
        pm.registerEvents(new JoinListener(this, config, playerData, updateChecker), this);
        pm.registerEvents(new SpawnListener(this, config, menuFile, messages), this);
        pm.registerEvents(new DefaultListener(config), this);

        if (pm.isPluginEnabled("PlaceholderAPI")) {
            new AquaExpansion(this, playerData).register();
        }

        AquaCommand command = new AquaCommand(this, config, menuFile, messagesFile, messages, menu, playerData);
        CommandRegistry.register(this, "aquaspawn", "Main command.",
                Collections.singletonList("aqspawn"), command, command);

        SpawnCommand spawnCommand = new SpawnCommand(this, config, menuFile, messages);
        CommandRegistry.register(this, "spawn", "Teleport to the server spawn.",
                Collections.emptyList(), spawnCommand, null);

        getLogger().info("AquaSpawn v" + getDescription().getVersion() + " enabled ("
                + (Scheduler.isFolia() ? "Folia" : "Bukkit") + " scheduler).");
    }

    @Override
    public void onDisable() {
        if (playerData != null) {
            playerData.flush();
        }
        CC.shutdown();
        try {
            HeadRender.shutdown();
        } catch (Throwable ignored) {
        }
    }
}
