package com.aquaticstudios.aquaspawn.listener;

import com.aquaticstudios.aquaspawn.utils.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Assigns each player their permanent join number as early as possible, so later listeners
 * (welcome messages, placeholders) can rely on the value already being present.
 */
public final class PlayerDataListener implements Listener {

    private final PlayerData data;

    public PlayerDataListener(PlayerData data) {
        this.data = data;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        data.register(event.getPlayer().getUniqueId());
    }
}
