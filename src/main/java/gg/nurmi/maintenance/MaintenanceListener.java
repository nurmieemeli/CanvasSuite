package gg.nurmi.maintenance;

import gg.nurmi.OneSMPPlugin;
import net.kyori.adventure.audience.Audience;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

// Bukkit presets AsyncPlayerPreLoginEvent's result to KICK_WHITELIST before any listener runs if the
// server's whitelist is enabled and the connecting UUID isn't whitelisted/op - this only needs to grant
// bypass holders an exception and swap in our own kick message. Rejecting here (rather than at
// PlayerLoginEvent) turns away maintenance-blocked connections off the main thread, before a Player object
// is ever constructed for them.
public final class MaintenanceListener implements Listener {

    private final OneSMPPlugin plugin;
    private final MaintenanceManager maintenanceManager;

    public MaintenanceListener(OneSMPPlugin plugin, MaintenanceManager maintenanceManager) {
        this.plugin = plugin;
        this.maintenanceManager = maintenanceManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!maintenanceManager.isEnabled()) {
            return;
        }

        if (maintenanceManager.canBypass(event.getUniqueId())) {
            event.allow();
            return;
        }

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                plugin.messages().render(Audience.empty(), "maintenance.kick-message"));
    }
}
