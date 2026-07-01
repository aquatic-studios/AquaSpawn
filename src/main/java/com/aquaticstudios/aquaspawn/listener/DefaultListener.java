package com.aquaticstudios.aquaspawn.listener;

import com.aquaticstudios.aquaspawn.config.ConfigFile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class DefaultListener implements Listener {

    private final ConfigFile config;

    public DefaultListener(ConfigFile config) {
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (config.get().getBoolean("settings.hide-join-message", false)) {
            event.setJoinMessage(null);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (config.get().getBoolean("settings.hide-quit-message", false)) {
            event.setQuitMessage(null);
        }
    }
}
