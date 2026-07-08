package gg.nurmi.stats;

import gg.nurmi.CanvasSuitePlugin;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public final class StatsListener implements Listener {

    private final CanvasSuitePlugin plugin;
    private final StatsManager statsManager;

    public StatsListener(CanvasSuitePlugin plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        statsManager.handleJoin(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        statsManager.handleQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        statsManager.recordDeath(victim.getUniqueId(), victim.getName());

        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        int streak = statsManager.recordKill(killer.getUniqueId(), killer.getName());
        broadcastMilestone(killer, streak);
    }

    private void broadcastMilestone(Player killer, int streak) {
        List<Integer> milestones = plugin.getConfig().getIntegerList("stats.killstreak-broadcast-milestones");
        if (!milestones.contains(streak)) {
            return;
        }
        Bukkit.broadcast(plugin.messages().render(killer, "stats.killstreak-broadcast",
                Placeholder.unparsed("player", killer.getName()),
                Placeholder.unparsed("streak", String.valueOf(streak))));
    }
}
