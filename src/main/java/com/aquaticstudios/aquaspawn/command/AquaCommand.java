package com.aquaticstudios.aquaspawn.command;

import com.aquaticstudios.aquaspawn.utils.CC;
import com.aquaticstudios.aquaspawn.utils.Permissions;
import com.aquaticstudios.aquaspawn.utils.config.ConfigFile;
import com.aquaticstudios.aquaspawn.utils.config.Messages;
import com.aquaticstudios.aquaspawn.menu.MenuManager;
import com.aquaticstudios.aquaspawn.data.PlayerData;
import com.aquaticstudios.aquaspawn.utils.VersionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class AquaCommand implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final String pluginVersion;
    private final ConfigFile config;
    private final ConfigFile menuFile;
    private final ConfigFile messagesFile;
    private final Messages messages;
    private final MenuManager menu;
    private final PlayerData playerData;

    public AquaCommand(Plugin plugin, ConfigFile config, ConfigFile menuFile,
                       ConfigFile messagesFile, Messages messages, MenuManager menu, PlayerData playerData) {
        this.plugin = plugin;
        this.pluginVersion = plugin.getDescription().getVersion();
        this.config = config;
        this.menuFile = menuFile;
        this.messagesFile = messagesFile;
        this.messages = messages;
        this.menu = menu;
        this.playerData = playerData;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            banner(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help":
                help(sender);
                return true;
            case "menu":
                menu(sender);
                return true;
            case "reload":
                reload(sender);
                return true;
            case "reset":
                reset(sender);
                return true;
            case "create":
                create(sender, args);
                return true;
            case "set":
                set(sender, args);
                return true;
            case "custom":
                custom(sender, args);
                return true;
            default:
                messages.send(sender, "unknown-command");
                return true;
        }
    }

    private void banner(CommandSender sender) {
        sender.sendMessage(CC.format(" "));
        sender.sendMessage(CC.format("            &#54ADF4&lAquaSpawn &fversion &#8DFF87[" + pluginVersion + "] &7(" + VersionUtil.getVersion() + ")"));
        sender.sendMessage(CC.format("           &fPowered by &#8BD5FFSenkex @ Aquatic Studios"));
        sender.sendMessage(CC.format(" "));
    }

    private void help(CommandSender sender) {
        if (!Permissions.has(sender,"aquaspawn.help")) {
            messages.send(sender, "no-permission");
            return;
        }
        messages.sendList(sender, "help");
    }

    private void menu(CommandSender sender) {
        if (!Permissions.has(sender,"aquaspawn.menu")) {
            messages.send(sender, "no-permission");
            return;
        }
        if (!(sender instanceof Player)) {
            messages.send(sender, "players-only");
            return;
        }
        menu.open((Player) sender);
    }

    private void reload(CommandSender sender) {
        if (!Permissions.has(sender,"aquaspawn.reload")) {
            messages.send(sender, "no-permission");
            return;
        }
        config.reload();
        menuFile.reload();
        messagesFile.reload();
        menu.load();
        messages.send(sender, "reload");
    }

    private void reset(CommandSender sender) {
        if (!Permissions.has(sender,"aquaspawn.reset")) {
            messages.send(sender, "no-permission");
            return;
        }
        playerData.reset();
        messages.send(sender, "reset");
    }

    private void set(CommandSender sender, String[] args) {
        if (!Permissions.has(sender,"aquaspawn.set")) {
            messages.send(sender, "no-permission");
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "spawn-usage-set");
            return;
        }
        String name = args[1];
        String type = args[2].toLowerCase(Locale.ROOT);
        if (!type.equals("first") && !type.equals("force")) {
            messages.send(sender, "spawn-type-invalid");
            return;
        }
        String resolved = resolveSpawn(name);
        if (resolved == null) {
            messages.send(sender, "spawn-not-created", "%aquaspawn_name%", name);
            return;
        }
        config.get().set("settings.join-spawn", resolved);
        config.get().set("settings.type-spawn", type);
        config.save();
        config.reload();
        messages.send(sender, "spawn-set", "%aquaspawn_name%", resolved, "%aquaspawn_type%", type);
    }

    private void custom(CommandSender sender, String[] args) {
        if (!Permissions.has(sender, "aquaspawn.set")) {
            messages.send(sender, "no-permission");
            return;
        }
        String firstArg = extract(args, "first:");
        String forceArg = extract(args, "force:");
        if (firstArg == null || forceArg == null) {
            messages.send(sender, "spawn-usage-custom");
            return;
        }
        String firstResolved = resolveSpawn(firstArg);
        if (firstResolved == null) {
            messages.send(sender, "spawn-not-created", "%aquaspawn_name%", firstArg);
            return;
        }
        String forceResolved = resolveSpawn(forceArg);
        if (forceResolved == null) {
            messages.send(sender, "spawn-not-created", "%aquaspawn_name%", forceArg);
            return;
        }
        if (firstResolved.equalsIgnoreCase(forceResolved)) {
            messages.send(sender, "spawn-custom-same");
            return;
        }
        config.get().set("settings.custom.first", firstResolved);
        config.get().set("settings.custom.force", forceResolved);
        config.get().set("settings.join-spawn", forceResolved);
        config.get().set("settings.type-spawn", "custom");
        config.save();
        config.reload();
        messages.send(sender, "spawn-set-custom", "%aquaspawn_first%", firstResolved, "%aquaspawn_force%", forceResolved);
    }

    private static String extract(String[] args, String key) {
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.length() > key.length() && arg.regionMatches(true, 0, key, 0, key.length())) {
                return arg.substring(key.length());
            }
        }
        return null;
    }

    private String resolveSpawn(String name) {
        ConfigurationSection items = menuFile.get().getConfigurationSection("items");
        if (items == null) {
            return null;
        }
        for (String key : items.getKeys(false)) {
            if (key.equalsIgnoreCase(name) && items.getString(key + ".cord") != null) {
                return key;
            }
        }
        return null;
    }

    private void create(CommandSender sender, String[] args) {
        if (!Permissions.has(sender,"aquaspawn.create")) {
            messages.send(sender, "no-permission");
            return;
        }
        if (!(sender instanceof Player)) {
            messages.send(sender, "players-only");
            return;
        }
        if (args.length < 2) {
            messages.send(sender, "spawn-usage-create");
            return;
        }
        Player player = (Player) sender;
        String name = args[1];

        ConfigurationSection items = menuFile.get().getConfigurationSection("items");
        if (items != null && items.getKeys(false).stream().anyMatch(k -> k.equalsIgnoreCase(name))) {
            messages.send(sender, "spawn-exists", "%aquaspawn_name%", name);
            return;
        }

        int size = clampSize(menuFile.get().getInt("size", 45));
        int slot = nextSlot(items, size);
        if (slot < 0) {
            messages.send(sender, "menu-full");
            return;
        }

        int number = 1;
        if (items != null) {
            number = (int) items.getKeys(false).stream().filter(k -> !k.equalsIgnoreCase("close")).count() + 1;
        }
        String worldName = player.getWorld().getName();
        String coords = player.getLocation().getBlockX() + ", " + player.getLocation().getBlockZ();
        String cord = player.getLocation().getX() + "," + player.getLocation().getY() + ","
                + player.getLocation().getZ() + "," + player.getLocation().getYaw() + ","
                + player.getLocation().getPitch();

        String base = "items." + name + ".";
        menuFile.get().set(base + "material", "MAP");
        menuFile.get().set(base + "slot", slot);
        menuFile.get().set(base + "display_name", "&#FFEE00&lSpawn #" + number);
        menuFile.get().set(base + "lore", Arrays.asList(
                "&7Server spawn point",
                "",
                "&#54ADF4Name: &f" + name,
                "&#54ADF4Created by: &f" + player.getName(),
                "&#54ADF4World: &f" + worldName,
                "&#54ADF4Coordinates: &f" + coords,
                "",
                "&#FF9E08Click to teleport"
        ));
        menuFile.get().set(base + "left_click_commands", Arrays.asList(
                "[sound] ENTITY_ENDERMAN_TELEPORT:1:1",
                "[teleport] " + cord,
                "[close]"
        ));
        menuFile.get().set(base + "right_click_commands", Arrays.asList(
                "[teleport] " + cord,
                "[close]"
        ));
        menuFile.get().set(base + "world", worldName);
        menuFile.get().set(base + "cord", cord);
        menuFile.save();
        menuFile.reload();
        menu.load();
        messages.send(sender, "spawn-created", "%aquaspawn_name%", name);
    }

    private int nextSlot(ConfigurationSection items, int size) {
        List<Integer> occupied = new ArrayList<>();
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection section = items.getConfigurationSection(key);
                if (section != null) {
                    occupied.add(section.getInt("slot", -1));
                }
            }
        }
        int rows = size / 9;
        for (int row = 1; row <= rows - 2; row++) {
            for (int col = 1; col <= 7; col++) {
                int slot = row * 9 + col;
                if (!occupied.contains(slot)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    private static int clampSize(int configured) {
        if (configured < 9) {
            return 9;
        }
        if (configured > 54) {
            return 54;
        }
        return (configured / 9) * 9 == configured ? configured : ((configured / 9) + 1) * 9;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("menu", "help", "reload", "reset", "create", "set", "custom"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return filter(spawnNames(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filter(Arrays.asList("first", "force"), args[2]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("custom")) {
            String token = args[args.length - 1];
            String lower = token.toLowerCase(Locale.ROOT);
            String prefix = lower.startsWith("force:") ? "force:" : lower.startsWith("first:") ? "first:" : null;
            if (prefix == null) {
                return filter(Arrays.asList("first:", "force:"), token);
            }
            List<String> options = new ArrayList<>();
            for (String spawn : spawnNames()) {
                options.add(prefix + spawn);
            }
            return filter(options, token);
        }
        return new ArrayList<>();
    }

    private List<String> spawnNames() {
        List<String> names = new ArrayList<>();
        ConfigurationSection items = menuFile.get().getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                if (!key.equalsIgnoreCase("close") && items.getString(key + ".cord") != null) {
                    names.add(key);
                }
            }
        }
        return names;
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }
}
