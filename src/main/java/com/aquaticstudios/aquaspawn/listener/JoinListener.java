package com.aquaticstudios.aquaspawn.listener;

import com.aquaticstudios.aquaspawn.join.CustomJoin;
import com.aquaticstudios.aquaspawn.data.PlayerData;
import com.aquaticstudios.aquaspawn.scheduler.Scheduler;
import com.aquaticstudios.aquaspawn.utils.UpdateChecker;
import com.aquaticstudios.aquaspawn.utils.config.ConfigFile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public final class JoinListener implements Listener {

    private static final long UPDATE_NOTICE_DELAY_TICKS = 40L;

    private final Plugin plugin;
    private final ConfigFile config;
    private final PlayerData playerData;
    private final CustomJoin customJoin;
    private final UpdateChecker updateChecker;

    public JoinListener(Plugin plugin, ConfigFile config, PlayerData playerData, UpdateChecker updateChecker) {
        this.plugin = plugin;
        this.config = config;
        this.playerData = playerData;
        this.customJoin = new CustomJoin(plugin, config);
        this.updateChecker = updateChecker;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        boolean firstJoin = playerData.registerFirstJoin(player.getName());

        customJoin.handle(player, firstJoin).whenComplete((ignored, error) ->
                Scheduler.runForEntity(plugin, player,
                        () -> DefaultListener.broadcast(config, player, "settings.join-message")));

        Scheduler.runForEntityLater(plugin, player, () -> {
            if (player.isOnline()) {
                updateChecker.announce(player);
            }
        }, UPDATE_NOTICE_DELAY_TICKS);
    }
}
