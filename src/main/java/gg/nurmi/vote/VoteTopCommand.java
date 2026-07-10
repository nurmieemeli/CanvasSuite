package gg.nurmi.vote;

import gg.nurmi.OneSMPPlugin;
import gg.nurmi.vote.gui.VoteTopGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class VoteTopCommand implements CommandExecutor {

    private final OneSMPPlugin plugin;
    private final VoteManager voteManager;

    public VoteTopCommand(OneSMPPlugin plugin, VoteManager voteManager) {
        this.plugin = plugin;
        this.voteManager = voteManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("onesmp.vote.use")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }

        voteManager.top(45).thenAccept(entries -> {
            VoteTopGui gui = new VoteTopGui(plugin, entries);
            plugin.scheduler().runAtEntity(player, () -> gui.open(player), () -> {});
        });
        return true;
    }
}
