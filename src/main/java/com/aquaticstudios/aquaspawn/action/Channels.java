package com.aquaticstudios.aquaspawn.action;

import com.aquaticstudios.aquaspawn.utils.CC;
import com.aquaticstudios.aquaspawn.utils.Fireworks;
import com.aquaticstudios.aquaspawn.utils.Scheduler;
import com.aquaticstudios.aquaspawn.utils.Sounds;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;

/**
 * Plays the configurable notification channels (chat, action bar, title, boss bar, sound and
 * firework) found under a parent config section such as {@code join-motd} or {@code first-join}.
 * Every channel is independently toggleable and fully editable (timings, colors, sounds).
 */
public final class Channels {

    private static final long TICKS_PER_SECOND = 20L;
    /** Vanilla action bars fade after ~3s; resend on this interval to honour longer durations. */
    private static final long ACTIONBAR_INTERVAL_TICKS = 40L;

    private final Plugin plugin;

    public Channels(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Plays every enabled channel defined under {@code section}. */
    public void play(Player player, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        chat(player, section.getConfigurationSection("chat"));
        actionBar(player, section.getConfigurationSection("actionbar"));
        title(player, section.getConfigurationSection("title"));
        bossBar(player, section.getConfigurationSection("bossbar"));
        sound(player, section.getConfigurationSection("sound"));
        Fireworks.spawn(plugin, player, section.getConfigurationSection("firework"));
    }

    private void chat(Player player, ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }
        List<String> message = section.getStringList("message");
        if (message.isEmpty()) {
            return;
        }
        CC.lines(player, message).thenAccept(components -> Scheduler.runForEntity(plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            for (Component component : components) {
                CC.audience(player).sendMessage(component);
            }
        }));
    }

    private void actionBar(Player player, ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }
        String text = section.getString("text", "");
        int seconds = Math.max(1, section.getInt("stay", 3));
        int resends = (int) Math.ceil(seconds / 2.0);
        resendActionBar(player, text, resends);
    }

    private void resendActionBar(Player player, String text, int remaining) {
        if (!player.isOnline() || remaining <= 0) {
            return;
        }
        CC.sendActionBar(player, text);
        if (remaining - 1 > 0) {
            Scheduler.runForEntityLater(plugin, player,
                    () -> resendActionBar(player, text, remaining - 1), ACTIONBAR_INTERVAL_TICKS);
        }
    }

    private void title(Player player, ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }
        CC.sendTitle(player,
                section.getInt("fade-in", 10),
                section.getInt("stay", 40),
                section.getInt("fade-out", 10),
                section.getString("title", ""),
                section.getString("subtitle", ""));
    }

    private void bossBar(Player player, ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }
        Component name = CC.parse(player, section.getString("text", ""));
        float progress = clamp01((float) section.getDouble("progress", 1.0));
        BossBar bar = BossBar.bossBar(name, progress,
                color(section.getString("color", "BLUE")),
                overlay(section.getString("overlay", "PROGRESS")));
        CC.audience(player).showBossBar(bar);

        int stay = section.getInt("stay", 5);
        if (stay > 0) {
            Scheduler.runForEntityLater(plugin, player,
                    () -> CC.audience(player).hideBossBar(bar), stay * TICKS_PER_SECOND);
        }
    }

    private void sound(Player player, ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }
        Sounds.play(player, section.getString("value", ""));
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static BossBar.Color color(String value) {
        try {
            return BossBar.Color.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            return BossBar.Color.BLUE;
        }
    }

    private static BossBar.Overlay overlay(String value) {
        try {
            return BossBar.Overlay.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}
