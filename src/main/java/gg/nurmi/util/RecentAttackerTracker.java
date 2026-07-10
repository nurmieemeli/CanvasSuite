package gg.nurmi.util;

import gg.nurmi.OneSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// Tracks the last player to damage each victim - resolved through projectiles/TNT/crystals/anchors to the
// actual player - so stats kill-crediting, custom death messages, and combat-log detection all attribute an
// indirect hit (arrow, knockback into lava/the void, primed TNT, crystal PvP, a triggered respawn anchor) the
// same way, instead of PlayerDeathEvent#getKiller() (direct melee only) silently crediting no one. A bed
// exploding in the Nether/End is the same kind of block-only explosion as a respawn anchor but isn't tracked
// here yet. Every lookup here is read-only: entries are only ever replaced by a newer hit on that player,
// never actively removed, so multiple listeners can safely query the same state (e.g. on the same death, or
// on quit) without racing each other over who clears it first.
public final class RecentAttackerTracker implements Listener {

    public record KillCredit(UUID uuid, String name) {
    }

    private record RecentAttack(UUID attackerUuid, String attackerName, long atMillis) {
    }

    private record BlockTrigger(UUID playerUuid, String playerName, Location location, long atMillis) {
    }

    private static final long ANCHOR_TRIGGER_WINDOW_MILLIS = 2000L;
    private static final double ANCHOR_TRIGGER_RADIUS_SQUARED = 64.0; // 8 blocks - vanilla's anchor blast radius is 5

    private final OneSMPPlugin plugin;
    private final Map<UUID, RecentAttack> recentAttackers = new ConcurrentHashMap<>();
    // End Crystal explosions have no equivalent of TNTPrimed#getSource() - the crystal is the "damager" for
    // its own blast, so we track who last hit each crystal (by its entity UUID) to attribute crystal PvP.
    private final Map<UUID, RecentAttack> crystalBreakers = new ConcurrentHashMap<>();
    // Respawn anchor (and, in principle, bed) explosions have no entity damager at all - they arrive as a
    // plain EntityDamageEvent with cause BLOCK_EXPLOSION, so attribution has to correlate by location/timing
    // instead of an entity UUID. Small and short-lived enough that a linear scan on read is fine.
    private final List<BlockTrigger> recentAnchorTriggers = new CopyOnWriteArrayList<>();

    public RecentAttackerTracker(OneSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnchorUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.RESPAWN_ANCHOR
                || event.getPlayer().getWorld().getEnvironment() == World.Environment.NETHER) {
            return;
        }
        if (!(block.getBlockData() instanceof RespawnAnchor anchor) || anchor.getCharges() <= 0) {
            return;
        }
        recentAnchorTriggers.add(new BlockTrigger(event.getPlayer().getUniqueId(), event.getPlayer().getName(),
                block.getLocation(), System.currentTimeMillis()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplosionDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        BlockTrigger trigger = consumeNearbyAnchorTrigger(victim.getLocation());
        if (trigger == null || trigger.playerUuid().equals(victim.getUniqueId())) {
            return;
        }
        recentAttackers.put(victim.getUniqueId(),
                new RecentAttack(trigger.playerUuid(), trigger.playerName(), System.currentTimeMillis()));
    }

    private BlockTrigger consumeNearbyAnchorTrigger(Location damageLocation) {
        long now = System.currentTimeMillis();
        Iterator<BlockTrigger> iterator = recentAnchorTriggers.iterator();
        BlockTrigger match = null;
        while (iterator.hasNext()) {
            BlockTrigger trigger = iterator.next();
            boolean expired = now - trigger.atMillis() > ANCHOR_TRIGGER_WINDOW_MILLIS;
            boolean nearby = !expired && trigger.location().getWorld().equals(damageLocation.getWorld())
                    && trigger.location().distanceSquared(damageLocation) <= ANCHOR_TRIGGER_RADIUS_SQUARED;
            if (expired || nearby) {
                recentAnchorTriggers.remove(trigger);
            }
            if (nearby && match == null) {
                match = trigger;
            }
        }
        return match;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof EnderCrystal crystal && event.getDamager() instanceof Player breaker) {
            crystalBreakers.put(crystal.getUniqueId(),
                    new RecentAttack(breaker.getUniqueId(), breaker.getName(), System.currentTimeMillis()));
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        recentAttackers.put(victim.getUniqueId(),
                new RecentAttack(attacker.getUniqueId(), attacker.getName(), System.currentTimeMillis()));
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            return player;
        }
        if (damager instanceof EnderCrystal crystal) {
            RecentAttack breaker = crystalBreakers.remove(crystal.getUniqueId());
            if (breaker == null || System.currentTimeMillis() - breaker.atMillis() > 5000L) {
                return null;
            }
            return Bukkit.getPlayer(breaker.attackerUuid());
        }
        return null;
    }

    // Prefers the direct killer (melee, only ever set for that case); falls back to the last tracked hit
    // within stats.indirect-kill-window-seconds.
    public KillCredit resolve(Player victim) {
        Player direct = victim.getKiller();
        if (direct != null && !direct.getUniqueId().equals(victim.getUniqueId())) {
            return new KillCredit(direct.getUniqueId(), direct.getName());
        }
        long windowMillis = Math.max(0, plugin.getConfig().getInt("stats.indirect-kill-window-seconds", 15)) * 1000L;
        return recentAttacker(victim.getUniqueId(), windowMillis);
    }

    // Caller-specified window, independent of stats' indirect-kill window and getKiller() fallback -
    // used for things like combat-log detection where the victim isn't dead (yet).
    public KillCredit recentAttacker(UUID victimUuid, long windowMillis) {
        RecentAttack recent = recentAttackers.get(victimUuid);
        if (recent == null || System.currentTimeMillis() - recent.atMillis() > windowMillis) {
            return null;
        }
        return new KillCredit(recent.attackerUuid(), recent.attackerName());
    }
}
