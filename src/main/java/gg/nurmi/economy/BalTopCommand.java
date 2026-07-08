package gg.nurmi.economy;

import gg.nurmi.OneSMPPlugin;
import gg.nurmi.economy.gui.BalTopGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class BalTopCommand implements CommandExecutor {

    private final OneSMPPlugin plugin;
    private final EconomyManager economyManager;

    public BalTopCommand(OneSMPPlugin plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("onesmp.economy.use")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }

        economyManager.topBalances(45).thenAccept(entries -> {
            BalTopGui gui = new BalTopGui(plugin, entries);
            plugin.scheduler().runAtEntity(player, () -> gui.open(player), () -> {});
        });
        return true;
    }
}
