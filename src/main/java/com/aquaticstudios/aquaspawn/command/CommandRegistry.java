package com.aquaticstudios.aquaspawn.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class CommandRegistry {

    private CommandRegistry() {
    }

    public static void register(Plugin plugin, String name, String description, List<String> aliases,
                                CommandExecutor executor, TabCompleter completer) {
        CommandMap map = commandMap();
        if (map == null) {
            plugin.getLogger().warning("Could not access the command map, /" + name + " was not registered.");
            return;
        }
        map.register(plugin.getName().toLowerCase(), new Wrapper(name, description, aliases, executor, completer));
    }

    private static CommandMap commandMap() {
        try {
            return (CommandMap) Bukkit.getServer().getClass().getMethod("getCommandMap").invoke(Bukkit.getServer());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final class Wrapper extends Command {

        private final CommandExecutor executor;
        private final TabCompleter completer;

        Wrapper(String name, String description, List<String> aliases,
                CommandExecutor executor, TabCompleter completer) {
            super(name);
            if (description != null) {
                setDescription(description);
            }
            if (aliases != null && !aliases.isEmpty()) {
                setAliases(aliases);
            }
            this.executor = executor;
            this.completer = completer;
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return executor.onCommand(sender, this, label, args);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            if (completer != null) {
                List<String> result = completer.onTabComplete(sender, this, alias, args);
                if (result != null) {
                    return result;
                }
            }
            return new ArrayList<>();
        }
    }
}
