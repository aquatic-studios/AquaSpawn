package com.aquaticstudios.aquaspawn.listener;

import com.aquaticstudios.aquaspawn.config.ConfigFile;
import com.aquaticstudios.aquaspawn.utils.ColorUtils;
import com.aquaticstudios.aquaspawn.utils.Placeholders;
import com.aquaticstudios.aquaspawn.utils.Scheduler;
import com.github.senkex.centermessage.CenterMessage;
import com.github.senkex.headrender.HeadRender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class JoinListener implements Listener {

    private final Plugin plugin;
    private final ConfigFile config;

    public JoinListener(Plugin plugin, ConfigFile config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.get().getBoolean("join.enabled", true)) {
            return;
        }
        List<String> lines = config.get().getStringList("join.message");
        if (lines.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        CompletableFuture<List<String>> pipeline = CompletableFuture.completedFuture(new ArrayList<>());
        for (String raw : lines) {
            String line = raw == null ? "" : raw;
            if (line.contains("<head>")) {
                pipeline = pipeline.thenCompose(acc -> HeadRender.render(player.getName())
                        .thenApply(head -> {
                            acc.addAll(head);
                            return acc;
                        })
                        .exceptionally(ex -> acc));
            } else {
                String rendered = process(player, line);
                pipeline = pipeline.thenApply(acc -> {
                    acc.add(rendered);
                    return acc;
                });
            }
        }

        pipeline.thenAccept(out -> Scheduler.runForEntity(plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            out.forEach(player::sendMessage);
        }));
    }

    private String process(Player player, String line) {
        if (line.trim().equalsIgnoreCase("<empty>")) {
            return ColorUtils.color("&r");
        }
        String resolved = Placeholders.apply(player, line);
        if (resolved.contains("<center>")) {
            return CenterMessage.center(resolved.replace("<center>", ""));
        }
        return ColorUtils.color(resolved);
    }
}
