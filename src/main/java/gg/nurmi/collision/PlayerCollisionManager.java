package gg.nurmi.collision;

import gg.nurmi.OneSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

// Makes players permanently non-collidable with every entity, server-wide. Player-vs-player collision
// can't be controlled via Entity#setCollidable() - the client predicts that collision itself, so the
// flag is unreliable for players (see LivingEntity#setCollidable javadoc). A real scoreboard team
// collision rule is the only mechanism that actually works.
public final class PlayerCollisionManager {

    private static final String NO_COLLISION_TEAM = "onesmp_nocollide";

    private final OneSMPPlugin plugin;

    public PlayerCollisionManager(OneSMPPlugin plugin) {
        this.plugin = plugin;
    }

    // Scoreboard team registration/mutation is server-wide state and must happen on Folia's global tick
    // thread (throws IllegalStateException otherwise) - never call this from an entity/region thread.
    public void ensureTeamExists() {
        plugin.scheduler().runGlobal(() -> {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            if (scoreboard.getTeam(NO_COLLISION_TEAM) == null) {
                Team team = scoreboard.registerNewTeam(NO_COLLISION_TEAM);
                team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            }
        });
    }

    // Added once on join and never removed. Requires paper-world-defaults.yml collisions.only-players-collide
    // + collisions.allow-vehicle-collisions to be true, or the guild-tag passenger mount (see NametagManager)
    // re-breaks collision through the vanilla vehicle exemption anyway.
    public void disableCollision(Player player) {
        String playerName = player.getName();
        plugin.scheduler().runGlobal(() -> {
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(NO_COLLISION_TEAM);
            if (team != null && !team.hasEntry(playerName)) {
                team.addEntry(playerName);
            }
        });
    }
}
