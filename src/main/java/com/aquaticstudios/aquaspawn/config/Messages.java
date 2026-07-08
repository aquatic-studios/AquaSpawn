package com.aquaticstudios.aquaspawn.config;

import com.aquaticstudios.aquaspawn.utils.ColorUtils;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class Messages {

    private final ConfigFile file;

    public Messages(ConfigFile file) {
        this.file = file;
    }

    public String prefix() {
        return file.get().getString("prefix", "");
    }

    public String format(String key, String... replacements) {
        String raw = file.get().getString("messages." + key, key);
        raw = raw.replace("%prefix%", prefix());
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return ColorUtils.color(raw);
    }

    public void send(CommandSender target, String key, String... replacements) {
        target.sendMessage(format(key, replacements));
    }

    public void sendList(CommandSender target, String key) {
        List<String> lines = file.get().getStringList(key);
        for (String line : lines) {
            target.sendMessage(ColorUtils.color(line.replace("%prefix%", prefix())));
        }
    }
}
