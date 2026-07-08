package gg.nurmi.spawn;

import gg.nurmi.OneSMPPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class FirstJoinListener implements Listener {

    private final OneSMPPlugin plugin;
    private final SpawnWorldManager spawnWorldManager;

    public FirstJoinListener(OneSMPPlugin plugin, SpawnWorldManager spawnWorldManager) {
        this.plugin = plugin;
        this.spawnWorldManager = spawnWorldManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) {
            return;
        }
        plugin.scheduler().runAtEntity(player, () -> spawnWorldManager.teleportToSpawn(player), () -> {});
    }
}
