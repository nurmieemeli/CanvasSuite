package gg.nurmi.teleport;

import gg.nurmi.OneSMPPlugin;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class SetHomeCommand implements CommandExecutor {

    private final OneSMPPlugin plugin;
    private final HomeManager homeManager;

    public SetHomeCommand(OneSMPPlugin plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
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

        String name = args.length > 0 ? args[0] : "home";
        int limit = homeManager.resolveLimit(player);

        homeManager.setHome(player.getUniqueId(), name, player.getLocation(), limit).thenAccept(result -> {
            switch (result) {
                case CREATED, UPDATED -> plugin.messages().send(player, "teleport.home-set", Placeholder.unparsed("name", name));
                case LIMIT_REACHED -> plugin.messages().send(player, "teleport.home-limit-reached",
                        Placeholder.unparsed("limit", String.valueOf(limit)));
            }
        });
        return true;
    }
}
