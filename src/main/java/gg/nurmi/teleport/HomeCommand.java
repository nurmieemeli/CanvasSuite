package gg.nurmi.teleport;

import gg.nurmi.OneSMPPlugin;
import gg.nurmi.teleport.gui.HomesGui;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class HomeCommand implements CommandExecutor {

    private final OneSMPPlugin plugin;
    private final HomeManager homeManager;
    private final TeleportExecutor teleportExecutor;

    public HomeCommand(OneSMPPlugin plugin, HomeManager homeManager, TeleportExecutor teleportExecutor) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.teleportExecutor = teleportExecutor;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("onesmp.home.use")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            homeManager.listHomes(player.getUniqueId()).thenAccept(homes -> {
                if (homes.isEmpty()) {
                    plugin.messages().send(player, "teleport.no-homes");
                } else if (homes.size() == 1) {
                    teleportExecutor.executeSafely(player, homes.getFirst().toLocation());
                } else {
                    plugin.scheduler().runAtEntity(player, () -> new HomesGui(plugin, homeManager, teleportExecutor, homes).open(player), () -> {});
                }
            });
            return true;
        }

        String name = args[0];
        homeManager.getHome(player.getUniqueId(), name).thenAccept(optionalHome -> {
            if (optionalHome.isEmpty()) {
                plugin.messages().send(player, "teleport.home-not-found", Placeholder.unparsed("name", name));
                return;
            }
            teleportExecutor.executeSafely(player, optionalHome.get().toLocation());
        });
        return true;
    }
}
