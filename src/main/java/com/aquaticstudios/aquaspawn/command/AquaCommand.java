package com.aquaticstudios.aquaspawn.command;

import com.aquaticstudios.aquaspawn.config.ConfigFile;
import com.aquaticstudios.aquaspawn.config.Messages;
import com.aquaticstudios.aquaspawn.menu.MenuManager;
import com.aquaticstudios.aquaspawn.util.ColorUtils;
import com.aquaticstudios.aquaspawn.util.VersionUtil;
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

    public AquaCommand(Plugin plugin, ConfigFile config, ConfigFile menuFile,
                       ConfigFile messagesFile, Messages messages, MenuManager menu) {
        this.plugin = plugin;
        this.pluginVersion = plugin.getDescription().getVersion();
        this.config = config;
        this.menuFile = menuFile;
        this.messagesFile = messagesFile;
        this.messages = messages;
        this.menu = menu;
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
            case "create":
                create(sender, args);
                return true;
            case "set":
                set(sender, args);
                return true;
            default:
                messages.send(sender, "unknown-command");
                return true;
        }
    }

    private void banner(CommandSender sender) {
        sender.sendMessage(ColorUtils.color(" "));
        sender.sendMessage(ColorUtils.color("            &#54ADF4&lAquaSpawn &fversion &#8DFF87[" + pluginVersion + "] &7(" + VersionUtil.getVersion() + ")"));
        sender.sendMessage(ColorUtils.color("           &fPowered by &#8BD5FFSenkex @ Aquatic Studios"));
        sender.sendMessage(ColorUtils.color(" "));
    }

    private void help(CommandSender sender) {
        if (!sender.hasPermission("aquaspawn.help")) {
            messages.send(sender, "no-permission");
            return;
        }
        messages.sendList(sender, "help");
    }

    private void menu(CommandSender sender) {
        if (!sender.hasPermission("aquaspawn.menu")) {
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
        if (!sender.hasPermission("aquaspawn.reload")) {
            messages.send(sender, "no-permission");
            return;
        }
        config.reload();
        menuFile.reload();
        messagesFile.reload();
        menu.load();
        messages.send(sender, "reload");
    }

    private void set(CommandSender sender, String[] args) {
        if (!sender.hasPermission("aquaspawn.set")) {
            messages.send(sender, "no-permission");
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "spawn-usage-set");
            return;
        }
        String name = args[1];
        String type = args[2].toLowerCase(Locale.ROOT);
        if (!type.equals("first") && !type.equals("force") && !type.equals("custom")) {
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
        if (type.equals("custom")) {
            config.get().set("settings.custom.first", resolved);
            config.get().set("settings.custom.force", resolved);
        }
        config.save();
        config.reload();
        messages.send(sender, "spawn-set", "%aquaspawn_name%", resolved, "%aquaspawn_type%", type);
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
        if (!sender.hasPermission("aquaspawn.create")) {
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
            return filter(Arrays.asList("menu", "help", "reload", "create", "set"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return filter(spawnNames(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filter(Arrays.asList("first", "force", "custom"), args[2]);
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
