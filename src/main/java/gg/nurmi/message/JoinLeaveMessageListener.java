package gg.nurmi.message;

import gg.nurmi.OneSMPPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class JoinLeaveMessageListener implements Listener {

    private final OneSMPPlugin plugin;

    public JoinLeaveMessageListener(OneSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        plugin.effects().join(event.getPlayer());

        if (!plugin.getConfig().getBoolean("join-leave.join-enabled", true)) {
            event.joinMessage(null);
            return;
        }
        event.joinMessage(plugin.messages().render(event.getPlayer(), "join-leave.join"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("join-leave.leave-enabled", true)) {
            event.quitMessage(null);
            return;
        }
        event.quitMessage(plugin.messages().render(event.getPlayer(), "join-leave.leave"));
    }
}
