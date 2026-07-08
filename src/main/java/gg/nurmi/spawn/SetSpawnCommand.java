package gg.nurmi.spawn;

import gg.nurmi.OneSMPPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class SetSpawnCommand implements CommandExecutor {

    private final OneSMPPlugin plugin;
    private final SpawnWorldManager spawnWorldManager;

    public SetSpawnCommand(OneSMPPlugin plugin, SpawnWorldManager spawnWorldManager) {
        this.plugin = plugin;
        this.spawnWorldManager = spawnWorldManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("onesmp.spawn.admin")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }

        spawnWorldManager.setSpawn(player.getLocation());
        plugin.messages().send(player, "spawn.set");
        return true;
    }
}
