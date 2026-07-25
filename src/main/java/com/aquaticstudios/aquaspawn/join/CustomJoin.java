package com.aquaticstudios.aquaspawn.join;

import com.aquaticstudios.aquaspawn.action.Channels;
import com.aquaticstudios.aquaspawn.menu.ActionHandler;
import com.aquaticstudios.aquaspawn.scheduler.Scheduler;
import com.aquaticstudios.aquaspawn.utils.CC;
import com.aquaticstudios.aquaspawn.utils.config.ConfigFile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CustomJoin {

    private final Plugin plugin;
    private final ConfigFile config;
    private final Channels channels;
    private final ActionHandler actions;

    public CustomJoin(Plugin plugin, ConfigFile config) {
        this.plugin = plugin;
        this.config = config;
        this.channels = new Channels(plugin);
        this.actions = new ActionHandler(plugin);
    }

    public CompletableFuture<Void> handle(Player player, boolean firstJoin) {
        ConfigurationSection root = config.get().getConfigurationSection("custom-events");
        if (root == null || !root.getBoolean("enabled", true)) {
            return CompletableFuture.completedFuture(null);
        }

        if (config.get().getBoolean("preload-skins", true) && usesHead(root, firstJoin)) {
            CC.preloadHead(player);
        }

        List<ConfigurationSection> blocks = new ArrayList<>();
        boolean handledByFirstJoin = false;
        if (firstJoin) {
            ConfigurationSection section = root.getConfigurationSection("first-join");
            if (section != null && section.getBoolean("enabled", true)) {
                blocks.add(section);
                handledByFirstJoin = section.getBoolean("override", true);
            }
        }
        if (!handledByFirstJoin) {
            ConfigurationSection join = root.getConfigurationSection("join");
            if (join != null && join.getBoolean("enabled", true)) {
                blocks.add(join);
            }
        }
        if (blocks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> chatDone = new ArrayList<>();
        for (ConfigurationSection block : blocks) {
            chatDone.add(channels.playInstant(player, block));
            actions.run(player, block.getStringList("actions"));
        }

        long delay = Math.max(1, root.getInt("delay-ticks", 20));
        Scheduler.runForEntityLater(plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            for (ConfigurationSection block : blocks) {
                channels.playDelayed(player, block);
            }
        }, delay);

        return CompletableFuture.allOf(chatDone.toArray(new CompletableFuture[0]));
    }

    private boolean usesHead(ConfigurationSection root, boolean firstJoin) {
        ConfigurationSection block = firstJoin ? root.getConfigurationSection("first-join") : null;
        if (block == null || !block.getBoolean("enabled", true)) {
            block = root.getConfigurationSection("join");
        }
        if (block == null || !block.getBoolean("enabled", true)) {
            return false;
        }
        ConfigurationSection chat = block.getConfigurationSection("chat");
        if (chat == null || !chat.getBoolean("enabled", true)) {
            return false;
        }
        List<String> message = chat.getStringList("message");
        for (String line : message) {
            if (line != null && line.contains("<hd>")) {
                return true;
            }
        }
        return false;
    }

}
