package gg.nurmi;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

public final class OneSMPCommand implements CommandExecutor {

    private static final String USAGE = "/onesmp <reload>";

    private final OneSMPPlugin plugin;

    public OneSMPCommand(OneSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!sender.hasPermission("onesmp.admin")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }
        if (args.length == 0 || !plugin.subcommandAliases().resolve("onesmp", args[0]).equals("reload")) {
            plugin.messages().send(sender, "general.unknown-command", Placeholder.unparsed("usage", USAGE));
            return true;
        }

        plugin.reloadAll();
        plugin.messages().send(sender, "general.reloaded");
        return true;
    }
}
