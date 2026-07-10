package gg.nurmi.teleport;

import gg.nurmi.OneSMPPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportWarmup {

    private final OneSMPPlugin plugin;
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    public TeleportWarmup(OneSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isPending(UUID uuid) {
        return pending.contains(uuid);
    }

    public void start(Player player, Runnable onComplete) {
        start(player, onComplete, () -> {});
    }

    // onCancelled fires for every way the warmup can end without onComplete: movement-cancel, the
    // entity being retired mid-warmup (e.g. the player disconnecting), or a duplicate request being
    // ignored because one is already pending - so callers with a side effect riding on completion
    // (e.g. RTP's cost) always get a chance to undo it.
    public void start(Player player, Runnable onComplete, Runnable onCancelled) {
        int warmupSeconds = plugin.getConfig().getInt("teleport.teleport-warmup-seconds", 3);
        if (warmupSeconds <= 0) {
            onComplete.run();
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!pending.add(uuid)) {
            onCancelled.run(); // already warming up, ignore the duplicate request
            return;
        }

        Location start = player.getLocation();
        boolean cancelOnMove = plugin.getConfig().getBoolean("teleport.cancel-warmup-on-move", true);
        plugin.messages().send(player, "teleport.teleporting", Placeholder.unparsed("seconds", String.valueOf(warmupSeconds)));

        ScheduledTask tick = plugin.scheduler().runAtEntityRepeating(player,
                () -> plugin.effects().teleportWarmupTick(player), () -> {}, 20L, 20L);

        boolean scheduled = plugin.scheduler().runAtEntityDelayed(player, () -> {
            pending.remove(uuid);
            tick.cancel();
            if (cancelOnMove && hasMoved(start, player.getLocation())) {
                plugin.messages().send(player, "teleport.teleport-cancelled-move");
                onCancelled.run();
                return;
            }
            onComplete.run();
        }, () -> {
            pending.remove(uuid);
            tick.cancel();
            onCancelled.run();
        }, warmupSeconds * 20L);

        if (!scheduled) {
            pending.remove(uuid);
            tick.cancel();
            onCancelled.run();
        }
    }

    private boolean hasMoved(Location a, Location b) {
        return a.getWorld() != b.getWorld() || a.distanceSquared(b) > 0.09;
    }
}
