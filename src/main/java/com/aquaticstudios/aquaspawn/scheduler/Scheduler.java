package com.aquaticstudios.aquaspawn.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Scheduler {

    private static final boolean FOLIA = detect();

    private static boolean detect() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runForEntity(Plugin plugin, Entity entity, Runnable task) {
        if (!FOLIA) {
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
            return;
        }
        try {
            Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
            Method run = scheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class);
            run.invoke(scheduler, plugin, (Consumer<Object>) ignored -> task.run(), null);
        } catch (Throwable t) {
            task.run();
        }
    }

    public static void runForEntityLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        long delay = Math.max(1L, delayTicks);
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
            return;
        }
        try {
            Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
            Method runDelayed = scheduler.getClass()
                    .getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
            runDelayed.invoke(scheduler, plugin, (Consumer<Object>) ignored -> task.run(), null, delay);
        } catch (Throwable t) {
            task.run();
        }
    }

    public static void teleport(Plugin plugin, Entity entity, Location location) {
        entity.teleportAsync(location);
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            return;
        }
        try {
            Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            Method runNow = scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
            runNow.invoke(scheduler, plugin, (Consumer<Object>) ignored -> task.run());
        } catch (Throwable t) {
            task.run();
        }
    }

    public static void runAsyncTimer(Plugin plugin, Runnable task, long intervalTicks) {
        long interval = Math.max(1L, intervalTicks);
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, interval, interval);
            return;
        }
        try {
            Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            Method runAtFixedRate = scheduler.getClass().getMethod("runAtFixedRate",
                    Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
            long ms = interval * 50L;
            runAtFixedRate.invoke(scheduler, plugin,
                    (Consumer<Object>) ignored -> task.run(), ms, ms, TimeUnit.MILLISECONDS);
        } catch (Throwable ignored) {
        }
    }
}
