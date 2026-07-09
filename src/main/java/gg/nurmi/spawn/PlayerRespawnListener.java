package gg.nurmi.spawn;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import gg.nurmi.OneSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public final class PlayerRespawnListener implements Listener {

    private final OneSMPPlugin plugin;
    private final SpawnWorldManager spawnWorldManager;

    public PlayerRespawnListener(OneSMPPlugin plugin, SpawnWorldManager spawnWorldManager) {
        this.plugin = plugin;
        this.spawnWorldManager = spawnWorldManager;
    }

    // Fires after the player is already placed at their respawn location.
    @EventHandler
    public void onRespawn(PlayerPostRespawnEvent event) {
        if (event.isBedSpawn() || event.isAnchorSpawn()) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.scheduler().runGlobalDelayed(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                spawnWorldManager.teleportToSpawn(player);
            }
        }, 2L);
    }
}
