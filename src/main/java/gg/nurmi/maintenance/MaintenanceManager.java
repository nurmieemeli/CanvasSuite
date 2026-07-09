package gg.nurmi.maintenance;

import gg.nurmi.OneSMPPlugin;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

// Maintenance mode is just a thin wrapper around Bukkit's own whitelist (whitelist.json) instead of a
// separate allow-list: enabling it flips the real server whitelist on, and onesmp.maintenance.bypass lets
// staff through regardless of whether they're actually on that whitelist.
public final class MaintenanceManager {

    private static final String BYPASS_PERMISSION = "onesmp.maintenance.bypass";

    private final OneSMPPlugin plugin;

    public MaintenanceManager(OneSMPPlugin plugin) {
        this.plugin = plugin;
        Bukkit.setWhitelist(isEnabled());
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("maintenance.enabled", false);
    }

    public boolean canBypass(Player player) {
        return player.hasPermission(BYPASS_PERMISSION);
    }

    // Used from AsyncPlayerPreLoginEvent, where there's no Player/Permissible yet to call hasPermission on.
    // Ops always bypass; anything beyond that needs LuckPerms to resolve the permission for a UUID that
    // hasn't joined yet - falls back to op-only if LuckPerms isn't installed.
    public boolean canBypass(UUID uniqueId) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uniqueId);
        if (offlinePlayer.isOp()) {
            return true;
        }
        try {
            User user = LuckPermsProvider.get().getUserManager().loadUser(uniqueId).join();
            return user.getCachedData().getPermissionData().checkPermission(BYPASS_PERMISSION).asBoolean();
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    public void enable() {
        plugin.getConfig().set("maintenance.enabled", true);
        plugin.saveConfig();
        Bukkit.setWhitelist(true);
        kickNonBypassing();
    }

    public void disable() {
        plugin.getConfig().set("maintenance.enabled", false);
        plugin.saveConfig();
        Bukkit.setWhitelist(false);
    }

    // Re-applies config.yml's maintenance.enabled to the real whitelist, in case an admin hand-edited it
    // and ran /onesmp reload rather than using /maintenance on|off.
    public void sync() {
        Bukkit.setWhitelist(isEnabled());
    }

    private void kickNonBypassing() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (canBypass(online)) {
                continue;
            }
            plugin.scheduler().runAtEntity(online,
                    () -> online.kick(plugin.messages().render(online, "maintenance.kick-message")),
                    () -> {});
        }
    }
}
