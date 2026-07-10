package gg.nurmi.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SchedulerUtil {

    private final Plugin plugin;

    public SchedulerUtil(Plugin plugin) {
        this.plugin = plugin;
    }

    public BukkitTask runGlobal(Runnable task) {
        return Bukkit.getScheduler().runTask(plugin, task);
    }

    public BukkitTask runGlobalDelayed(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(1, delayTicks));
    }

    public BukkitTask runGlobalRepeating(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, Math.max(1, delayTicks), periodTicks);
    }

    public BukkitTask runAtLocation(Location location, Runnable task) {
        return runGlobal(task);
    }

    public BukkitTask runAtLocationDelayed(Location location, Runnable task, long delayTicks) {
        return runGlobalDelayed(task, delayTicks);
    }

    public void runAtChunk(org.bukkit.World world, int chunkX, int chunkZ, Runnable task) {
        runGlobal(task);
    }

    public void runAtEntity(Entity entity, Runnable task, Runnable ifRetired) {
        if (!entity.isValid()) {
            ifRetired.run();
            return;
        }
        runGlobal(() -> {
            if (entity.isValid()) {
                task.run();
            } else {
                ifRetired.run();
            }
        });
    }

    public boolean runAtEntityDelayed(Entity entity, Runnable task, Runnable ifRetired, long delayTicks) {
        if (!entity.isValid()) {
            return false;
        }
        runGlobalDelayed(() -> {
            if (entity.isValid()) {
                task.run();
            } else {
                ifRetired.run();
            }
        }, delayTicks);
        return true;
    }

    public BukkitTask runAtEntityRepeating(Entity entity, Runnable task, Runnable ifRetired, long delayTicks, long periodTicks) {
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (entity.isValid()) {
                task.run();
            } else {
                ifRetired.run();
                holder[0].cancel();
            }
        }, Math.max(1, delayTicks), periodTicks);
        return holder[0];
    }

    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    public BukkitTask runAsyncRepeating(Runnable task, long delayMillis, long periodMillis) {
        long delayTicks = Math.max(1, delayMillis / 50);
        long periodTicks = Math.max(1, periodMillis / 50);
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }

    public <T> void supplyAsyncThenAtEntity(Supplier<T> supplier, Entity entity, Consumer<T> consumer) {
        supplyAsync(supplier).thenAccept(result -> runAtEntity(entity, () -> consumer.accept(result), () -> {}));
    }
}
