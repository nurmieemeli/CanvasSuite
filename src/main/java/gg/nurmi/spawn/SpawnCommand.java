package gg.nurmi.spawn;

import gg.nurmi.OneSMPPlugin;
import gg.nurmi.teleport.TeleportExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class SpawnCommand implements CommandExecutor {

    private final OneSMPPlugin plugin;
    private final SpawnWorldManager spawnWorldManager;
    private final TeleportExecutor teleportExecutor;

    public SpawnCommand(OneSMPPlugin plugin, SpawnWorldManager spawnWorldManager, TeleportExecutor teleportExecutor) {
        this.plugin = plugin;
        this.spawnWorldManager = spawnWorldManager;
        this.teleportExecutor = teleportExecutor;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("onesmp.spawn.use")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }

        teleportExecutor.executeSafely(player, spawnWorldManager.getSpawn(), "spawn.teleported");
        return true;
    }
}
