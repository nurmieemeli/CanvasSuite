package gg.nurmi.collision;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerCollisionListener implements Listener {

    private final PlayerCollisionManager collisionManager;

    public PlayerCollisionListener(PlayerCollisionManager collisionManager) {
        this.collisionManager = collisionManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        collisionManager.disableCollision(event.getPlayer());
    }
}
