package com.aquaticstudios.aquaspawn.listener;

import com.aquaticstudios.aquaspawn.join.CustomJoin;
import com.aquaticstudios.aquaspawn.utils.PlayerData;
import com.aquaticstudios.aquaspawn.utils.config.ConfigFile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/**
 * Delivers the configurable welcome experience on join, delegating to {@link CustomJoin}. First-join
 * status is read from {@link PlayerData}, which {@code PlayerDataListener} populates at LOWEST
 * priority before this listener runs.
 */
public final class JoinListener implements Listener {

    private final PlayerData playerData;
    private final CustomJoin customJoin;

    public JoinListener(Plugin plugin, ConfigFile config, PlayerData playerData) {
        this.playerData = playerData;
        this.customJoin = new CustomJoin(plugin, config);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean firstJoin = playerData.consumeFirstJoin(player.getUniqueId());
        customJoin.handle(player, firstJoin);
    }
}
