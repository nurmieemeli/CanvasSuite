package gg.nurmi.spawn;

import gg.nurmi.OneSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// PlayerRespawnEvent/PlayerPostRespawnEvent don't reliably fire on this Folia/Canvas build (confirmed via
// logging - neither fired for a plain drowning death), so this sidesteps them entirely: PlayerDeathEvent
// (which does fire reliably) flags players with no valid bed/anchor, then a poll picks them back up once
// they're alive again and redirects them if they didn't land in the spawn world.
public final class RespawnRedirectListener implements Listener {

    private final OneSMPPlugin plugin;
    private final SpawnWorldManager spawnWorldManager;
    private final Set<UUID> pendingRedirect = ConcurrentHashMap.newKeySet();

    public RespawnRedirectListener(OneSMPPlugin plugin, SpawnWorldManager spawnWorldManager) {
        this.plugin = plugin;
        this.spawnWorldManager = spawnWorldManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        boolean hasValidRespawn;
        try {
            hasValidRespawn = player.getRespawnLocation() != null;
        } catch (IllegalStateException ex) {
            // On this build, a stored bed/anchor location in a different world than the one the player
            // is currently in throws instead of just failing validation. That's a corrupted respawn point
            // (it'd also break vanilla's own respawn screen for this player), so clear it outright.
            plugin.getLogger().warning("Clearing corrupted respawn location for " + player.getName()
                    + ": " + ex.getMessage());
            player.setRespawnLocation(null, true);
            hasValidRespawn = false;
        }
        if (!hasValidRespawn) {
            pendingRedirect.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingRedirect.remove(event.getPlayer().getUniqueId());
    }

    public void checkPendingRespawns() {
        if (pendingRedirect.isEmpty()) {
            return;
        }
        for (UUID uuid : pendingRedirect) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                pendingRedirect.remove(uuid);
                continue;
            }
            plugin.scheduler().runAtEntity(player, () -> {
                if (player.isDead()) {
                    return;
                }
                pendingRedirect.remove(uuid);
                if (!spawnWorldManager.isVoidWorld(player.getWorld())) {
                    spawnWorldManager.teleportToSpawn(player);
                }
            }, () -> pendingRedirect.remove(uuid));
        }
    }
}
