package com.aquaticstudios.aquaspawn.listener;

import com.aquaticstudios.aquaspawn.utils.CC;
import com.aquaticstudios.aquaspawn.utils.config.ConfigFile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class DefaultListener implements Listener {

    private final ConfigFile config;

    public DefaultListener(ConfigFile config) {
        this.config = config;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        broadcast(config, event.getPlayer(), "settings.quit-message");
    }

    public static void broadcast(ConfigFile config, Player who, String route) {
        ConfigurationSection section = config.get().getConfigurationSection(route);
        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }
        for (String line : section.getStringList("text")) {
            Component message = CC.parse(who, line);
            for (Player online : Bukkit.getOnlinePlayers()) {
                CC.audience(online).sendMessage(message);
            }
            CC.audience(Bukkit.getConsoleSender()).sendMessage(message);
        }
    }
}
