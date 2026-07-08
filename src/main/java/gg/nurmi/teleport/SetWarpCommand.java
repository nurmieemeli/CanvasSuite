package gg.nurmi.teleport;

import gg.nurmi.CanvasSuitePlugin;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class SetWarpCommand implements CommandExecutor {

    private final CanvasSuitePlugin plugin;
    private final WarpManager warpManager;

    public SetWarpCommand(CanvasSuitePlugin plugin, WarpManager warpManager) {
        this.plugin = plugin;
        this.warpManager = warpManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("canvassuite.warp.admin")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.messages().send(sender, "general.unknown-command", Placeholder.unparsed("usage", "/setwarp <name>"));
            return true;
        }

        String name = args[0];
        warpManager.setWarp(name, player.getLocation(), player.getUniqueId()).thenRun(() ->
                plugin.messages().send(player, "teleport.warp-set", Placeholder.unparsed("name", name)));
        return true;
    }
}
