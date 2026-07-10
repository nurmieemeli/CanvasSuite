package gg.nurmi.combat;

import gg.nurmi.OneSMPPlugin;
import gg.nurmi.util.RecentAttackerTracker;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

// Kills a player who quits shortly after being hit by another player, instead of letting them safely
// disconnect out of a losing fight. Goes through Player#damage() rather than setHealth(0) so totems of
// undying/absorption/resistance still apply exactly as they would for any other lethal hit. The resulting
// death is a normal PlayerDeathEvent, so it's credited and announced through the usual stats/death-message
// pipeline - no separate logic needed here for that part.
public final class CombatLogListener implements Listener {

    private final OneSMPPlugin plugin;
    private final RecentAttackerTracker attackerTracker;

    public CombatLogListener(OneSMPPlugin plugin, RecentAttackerTracker attackerTracker) {
        this.plugin = plugin;
        this.attackerTracker = attackerTracker;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("combat-log.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isDead() || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        long windowMillis = Math.max(0, plugin.getConfig().getInt("combat-log.timeout-seconds", 15)) * 1000L;
        if (attackerTracker.recentAttacker(player.getUniqueId(), windowMillis) == null) {
            return;
        }
        player.damage(1000.0);
    }
}
