package gg.nurmi.message;

import gg.nurmi.OneSMPPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class SocialSpyCommand implements CommandExecutor {

    private final OneSMPPlugin plugin;
    private final SocialSpyToggle socialSpy;

    public SocialSpyCommand(OneSMPPlugin plugin, SocialSpyToggle socialSpy) {
        this.plugin = plugin;
        this.socialSpy = socialSpy;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("onesmp.msg.socialspy")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }

        boolean nowEnabled = socialSpy.toggle(player.getUniqueId());
        plugin.messages().send(player, nowEnabled ? "msg.socialspy-enabled" : "msg.socialspy-disabled");
        return true;
    }
}
