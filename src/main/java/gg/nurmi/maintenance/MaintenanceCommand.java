package gg.nurmi.maintenance;

import gg.nurmi.OneSMPPlugin;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Locale;

public final class MaintenanceCommand implements CommandExecutor, TabCompleter {

    private static final String USAGE = "/maintenance <on|off|status>";

    private final OneSMPPlugin plugin;
    private final MaintenanceManager maintenanceManager;

    public MaintenanceCommand(OneSMPPlugin plugin, MaintenanceManager maintenanceManager) {
        this.plugin = plugin;
        this.maintenanceManager = maintenanceManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!sender.hasPermission("onesmp.maintenance.admin")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.messages().send(sender, "general.unknown-command", Placeholder.unparsed("usage", USAGE));
            return true;
        }

        switch (plugin.subcommandAliases().resolve("maintenance", args[0])) {
            case "on" -> {
                if (maintenanceManager.isEnabled()) {
                    plugin.messages().send(sender, "maintenance.already-enabled");
                    return true;
                }
                maintenanceManager.enable();
                plugin.messages().send(sender, "maintenance.enabled");
            }
            case "off" -> {
                if (!maintenanceManager.isEnabled()) {
                    plugin.messages().send(sender, "maintenance.already-disabled");
                    return true;
                }
                maintenanceManager.disable();
                plugin.messages().send(sender, "maintenance.disabled");
            }
            case "status" -> plugin.messages().send(sender,
                    maintenanceManager.isEnabled() ? "maintenance.status-enabled" : "maintenance.status-disabled");
            default -> plugin.messages().send(sender, "general.unknown-command", Placeholder.unparsed("usage", USAGE));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return plugin.subcommandAliases().labels("maintenance").stream().filter(s -> s.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
