package gg.nurmi.moderation;

import gg.nurmi.OneSMPPlugin;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class SpectateCommand implements CommandExecutor {

    private final OneSMPPlugin plugin;
    private final SpectateManager spectateManager;

    public SpectateCommand(OneSMPPlugin plugin, SpectateManager spectateManager) {
        this.plugin = plugin;
        this.spectateManager = spectateManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("onesmp.moderation.spectate")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }
        if (!plugin.packetEvents().available()) {
            plugin.messages().send(player, "moderation.packetevents-required");
            return true;
        }

        if (spectateManager.isSpectating(player.getUniqueId())) {
            spectateManager.exit(player, plugin.spawnWorld().getSpawn());
            plugin.messages().send(player, "moderation.spectate-stopped");
            return true;
        }

        if (args.length == 0) {
            plugin.messages().send(player, "general.unknown-command", Placeholder.unparsed("usage", "/spectate <player>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.messages().send(player, "general.player-not-found", Placeholder.unparsed("target", args[0]));
            return true;
        }

        plugin.scheduler().runAtEntity(target, () -> {
            Location targetLocation = target.getLocation();
            plugin.scheduler().runAtEntity(player, () -> spectateManager.enter(player, targetLocation), () -> {});
        }, () -> {});
        plugin.messages().send(player, "moderation.spectate-started", Placeholder.unparsed("target", target.getName()));
        return true;
    }
}
