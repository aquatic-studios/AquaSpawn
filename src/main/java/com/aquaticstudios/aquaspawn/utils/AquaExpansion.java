package com.aquaticstudios.aquaspawn.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class AquaExpansion extends PlaceholderExpansion {

    private final Plugin plugin;
    private final PlayerData data;

    public AquaExpansion(Plugin plugin, PlayerData data) {
        this.plugin = plugin;
        this.data = data;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "aquaspawn";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Senkex";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("total_players")) {
            return String.valueOf(data.total());
        }
        return null;
    }
}
