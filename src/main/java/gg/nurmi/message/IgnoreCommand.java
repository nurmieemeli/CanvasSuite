package gg.nurmi.message;

import gg.nurmi.OneSMPPlugin;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class IgnoreCommand implements CommandExecutor {

    private final OneSMPPlugin plugin;
    private final PrivateMessageManager messageManager;

    public IgnoreCommand(OneSMPPlugin plugin, PrivateMessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("onesmp.msg.use")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.messages().send(player, "general.unknown-command", Placeholder.unparsed("usage", "/ignore <player>"));
            return true;
        }

        Player online = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayerIfCached(args[0]);
        if (target == null) {
            plugin.messages().send(player, "general.player-not-found", Placeholder.unparsed("target", args[0]));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            plugin.messages().send(player, "msg.ignore-self");
            return true;
        }

        boolean nowIgnoring = messageManager.toggleIgnore(player.getUniqueId(), target.getUniqueId());
        String targetName = target.getName() != null ? target.getName() : args[0];
        plugin.messages().send(player, nowIgnoring ? "msg.ignore-enabled" : "msg.ignore-disabled",
                Placeholder.unparsed("target", targetName));
        return true;
    }
}
