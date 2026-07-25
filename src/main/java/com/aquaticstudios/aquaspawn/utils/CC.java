package com.aquaticstudios.aquaspawn.utils;

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
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
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

    private static final Pattern HEAD_TAG = Pattern.compile("<head:([^<>]+)>");
    private static final Pattern HEAD_PLACEHOLDER = Pattern.compile("%head:([^%]+)%");
    private static final Pattern HEAD_CONTENT = Pattern.compile("<(?:hd|head)>([^<>\\s]+)</(?:hd|head)>");

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
        CompletableFuture<List<Component>> pipeline = CompletableFuture.completedFuture(new ArrayList<>());
        if (raw == null) {
            return pipeline;
        }
        for (String line : raw) {
            String resolved = line == null ? "" : Placeholders.apply(player, line);
            pipeline = pipeline.thenCompose(acc -> lineToComponents(resolved)
                    .thenApply(parts -> {
                        acc.addAll(parts);
                        return acc;
                    })
                    .exceptionally(ex -> acc));
        }
        return pipeline;
    }

    private static CompletableFuture<List<Component>> lineToComponents(String resolved) {
        if (resolved.isEmpty() || resolved.trim().equalsIgnoreCase("<empty>")) {
            return CompletableFuture.completedFuture(one(Component.empty()));
        }
        String s = normalizeHeadTags(resolved);
        boolean centered = s.contains("<center>");
        if (centered) {
            s = s.replace("<center>", "").replace("</center>", "");
        }

        Matcher head = HEAD_TAG.matcher(s);
        if (head.find()) {
            String name = head.group(1).trim();
            RenderOptions options = RenderOptions.builder().centered(centered).build();
            return HeadRenderComponents.render(name, options).exceptionally(ex -> one(parse(null, resolved)));
        }

        String mini = toMiniMessage(s);
        Component component = centered ? CenterMessageComponents.centerComponent(mini) : MINI.deserialize(mini);
        return CompletableFuture.completedFuture(one(component));
    }

    private static List<Component> one(Component component) {
        List<Component> list = new ArrayList<>(1);
        list.add(component);
        return list;
    }

    public static void send(CommandSender sender, String input) {
        audience(sender).sendMessage(parse(sender instanceof Player ? (Player) sender : null, input));
    }

    public static void sendActionBar(Player player, String input) {
        audience(player).sendActionBar(parse(player, input));
    }

    public static void sendTitle(Player player, int fadeInTicks, int stayTicks, int fadeOutTicks,
                                 String title, String subtitle) {
        Title.Times times = Title.Times.times(
                Duration.ofMillis(Math.max(0, fadeInTicks) * 50L),
                Duration.ofMillis(Math.max(0, stayTicks) * 50L),
                Duration.ofMillis(Math.max(0, fadeOutTicks) * 50L));
        audience(player).showTitle(Title.title(parse(player, title), parse(player, subtitle), times));
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
