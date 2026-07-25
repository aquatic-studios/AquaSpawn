package com.aquaticstudios.aquaspawn.utils;

import com.aquaticstudios.aquaspawn.placeholder.Placeholders;
import com.github.senkex.centermessage.components.CenterMessageComponents;
import com.github.senkex.headrender.HeadRender;
import com.github.senkex.headrender.HeadRenderComponents;
import com.github.senkex.headrender.RenderOptions;
import com.github.senkex.headrender.text.adventure.HeadRenderTags;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CC {

    private static MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = buildSerializer();

    private static BukkitAudiences audiences;

    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern BUNGEE_HEX = Pattern.compile("&x(&[0-9a-fA-F]){6}", Pattern.CASE_INSENSITIVE);

    private static final Pattern HEAD_PLACEHOLDER = Pattern.compile("%head:([^%]+)%");
    private static final String HEAD_ROW_MARKER = "<hd>";
    private static final Pattern HEAD_CONTENT = Pattern.compile("<(?:hd|head)>\\s*([^<>\\s]+)\\s*</(?:hd|head)>");

    private static final String[] NAMED = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white"
    };
    private static final String CODES = "0123456789abcdef";

    public static void init(Plugin plugin) {
        audiences = BukkitAudiences.create(plugin);

        TagResolver.Builder tags = TagResolver.builder().resolver(TagResolver.standard());
        try {
            HeadRenderTags head = HeadRenderTags.create(HeadRender.service(), Key.key("aquaspawn", "head"));
            tags.resolver(head.resolver());
        } catch (Throwable ignored) {
        }
        MINI = MiniMessage.builder().tags(tags.build()).build();
    }

    public static void shutdown() {
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
    }

    public static Audience audience(CommandSender sender) {
        return audiences.sender(sender);
    }

    public static Audience audience(Player player) {
        return audiences.player(player);
    }

    public static Component parse(Player player, String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String s = normalizeHeadTags(Placeholders.apply(player, input));
        boolean centered = s.contains("<center>");
        if (centered) {
            s = s.replace("<center>", "").replace("</center>", "");
        }
        String mini = toMiniMessage(s);
        try {
            return centered ? CenterMessageComponents.centerComponent(mini) : MINI.deserialize(mini);
        } catch (Throwable t) {
            return MINI.deserialize(mini.replace("<", "\\<"));
        }
    }

    public static String format(String input) {
        if (input == null) {
            return "";
        }
        try {
            return LEGACY.serialize(parse(null, input));
        } catch (Throwable t) {
            return input;
        }
    }

    public static CompletableFuture<List<Component>> lines(Player player, List<String> raw) {
        List<String> resolved = new ArrayList<>();
        if (raw != null) {
            for (String line : raw) {
                resolved.add(line == null ? "" : Placeholders.apply(player, line));
            }
        }

        boolean hasHead = false;
        boolean centered = false;
        for (String line : resolved) {
            if (line.contains(HEAD_ROW_MARKER)) {
                hasHead = true;
                if (line.contains("<center>")) {
                    centered = true;
                }
            }
        }

        if (!hasHead) {
            return CompletableFuture.completedFuture(toComponents(player, resolved));
        }

        String name = player == null ? "" : player.getName();
        RenderOptions options = RenderOptions.builder().centered(centered).build();
        return HeadRenderComponents.render(name, options)
                .thenApply(rows -> assemble(player, resolved, rows))
                .exceptionally(ex -> toComponents(player, stripHeadMarkers(resolved)));
    }

    private static List<Component> assemble(Player player, List<String> lines, List<Component> rows) {
        List<Component> out = new ArrayList<>(lines.size());
        int row = 0;
        for (String line : lines) {
            if (line.contains(HEAD_ROW_MARKER)) {
                Component head = row < rows.size() ? rows.get(row) : Component.empty();
                row++;
                String rest = line.replace(HEAD_ROW_MARKER, "").replace("</hd>", "")
                        .replace("<center>", "").replace("</center>", "").trim();
                out.add(rest.isEmpty() ? head : head.append(Component.space()).append(parse(player, rest)));
            } else {
                out.add(lineComponent(player, line));
            }
        }
        return out;
    }

    private static List<Component> toComponents(Player player, List<String> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(lineComponent(player, line));
        }
        return out;
    }

    private static List<String> stripHeadMarkers(List<String> lines) {
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(line.replace(HEAD_ROW_MARKER, "").replace("</hd>", ""));
        }
        return out;
    }

    private static Component lineComponent(Player player, String line) {
        if (line.isEmpty() || line.trim().equalsIgnoreCase("<empty>")) {
            return Component.empty();
        }
        return parse(player, line);
    }

    public static void preloadHead(Player player) {
        if (player == null) {
            return;
        }
        try {
            HeadRender.render(player.getName());
        } catch (Throwable ignored) {
        }
    }

    public static void send(CommandSender sender, String input) {
        audience(sender).sendMessage(parse(sender instanceof Player ? (Player) sender : null, input));
    }

    public static void sendActionBar(Player player, String input) {
        String legacy = LEGACY.serialize(parse(player, input));
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(legacy));
    }

    public static void sendTitle(Player player, int fadeInTicks, int stayTicks, int fadeOutTicks,
                                 String title, String subtitle) {
        String titleLegacy = LEGACY.serialize(parse(player, title));
        String subtitleLegacy = LEGACY.serialize(parse(player, subtitle));
        player.sendTitle(titleLegacy, subtitleLegacy,
                Math.max(0, fadeInTicks), Math.max(0, stayTicks), Math.max(0, fadeOutTicks));
    }

    private static String normalizeHeadTags(String input) {
        String s = input;
        if (s.indexOf('%') >= 0) {
            s = replaceHead(HEAD_PLACEHOLDER, s);
        }
        if (s.indexOf('<') >= 0) {
            s = replaceHead(HEAD_CONTENT, s);
        }
        return s;
    }

    private static String replaceHead(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement("<head:" + matcher.group(1).trim() + ">"));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static LegacyComponentSerializer buildSerializer() {
        LegacyComponentSerializer.Builder builder = LegacyComponentSerializer.builder()
                .character('§')
                .hexCharacter('#')
                .useUnusualXRepeatedCharacterHexFormat();

        if (VersionSupport.supportsHexColors()) {
            builder.hexColors();
        }
        return builder.build();
    }

    private static String toMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String work = expandHex(normalizeBungeeHex(input));

        StringBuilder out = new StringBuilder(work.length() + 16);
        char[] chars = work.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if ((c == '&' || c == '§') && i + 1 < chars.length) {
                String tag = translate(Character.toLowerCase(chars[i + 1]));
                if (tag != null) {
                    out.append(tag);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String expandHex(String input) {
        Matcher matcher = HEX.matcher(input);
        StringBuffer buffer = new StringBuffer(input.length() + 16);
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("<reset><#" + matcher.group(1) + ">"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String normalizeBungeeHex(String input) {
        Matcher matcher = BUNGEE_HEX.matcher(input);
        StringBuffer buffer = new StringBuffer(input.length());
        while (matcher.find()) {
            String raw = matcher.group();
            StringBuilder hex = new StringBuilder(8).append("&#");
            for (int i = 3; i < raw.length(); i += 2) {
                hex.append(raw.charAt(i));
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(hex.toString()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String translate(char code) {
        int index = CODES.indexOf(code);
        if (index != -1) {
            return "<reset><" + NAMED[index] + ">";
        }
        switch (code) {
            case 'l':
                return "<bold>";
            case 'o':
                return "<italic>";
            case 'n':
                return "<underlined>";
            case 'm':
                return "<strikethrough>";
            case 'k':
                return "<obfuscated>";
            case 'r':
                return "<reset>";
            default:
                return null;
        }
    }
}
