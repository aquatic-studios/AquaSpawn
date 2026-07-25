package com.aquaticstudios.aquaspawn.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * PlaceholderAPI expansion exposing AquaSpawn data to other plugins and to our own configs:
 * <ul>
 *   <li>{@code %aquaspawn_total_players%} — total unique players that have ever joined.</li>
 *   <li>{@code %aquaspawn_player_number%} (alias {@code %aquaspawn_number%}) — the player's
 *       permanent join ordinal.</li>
 * </ul>
 *
 * <p>Only instantiated when PlaceholderAPI is present, so referencing its API here is safe.</p>
 */
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
        switch (params.toLowerCase(Locale.ROOT)) {
            case "total_players":
                return String.valueOf(data.total());
            case "player_number":
            case "number":
                return player == null ? "0" : String.valueOf(data.number(player.getUniqueId()));
            default:
                return null;
        }
    }
}
