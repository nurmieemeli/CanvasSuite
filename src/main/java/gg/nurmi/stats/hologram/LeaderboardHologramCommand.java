package gg.nurmi.stats.hologram;

import gg.nurmi.CanvasSuitePlugin;
import gg.nurmi.stats.StatsManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Locale;

public final class LeaderboardHologramCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("create", "remove", "list");
    private static final List<String> STAT_TYPES = List.of("kills", "deaths", "killstreak", "playtime");

    private final CanvasSuitePlugin plugin;
    private final LeaderboardHologramManager hologramManager;

    public LeaderboardHologramCommand(CanvasSuitePlugin plugin, LeaderboardHologramManager hologramManager) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("canvassuite.stats.hologram.admin")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }
        if (hologramManager == null) {
            plugin.messages().send(sender, "stats.hologram-unavailable");
            return true;
        }
        if (args.length == 0) {
            plugin.messages().send(player, "general.unknown-command",
                    Placeholder.unparsed("usage", "/statshologram <" + String.join("|", SUBCOMMANDS) + ">"));
            return true;
        }

        switch (plugin.subcommandAliases().resolve("statshologram", args[0])) {
            case "create" -> handleCreate(player, args);
            case "remove" -> handleRemove(player, args);
            case "list" -> handleList(player);
            default -> plugin.messages().send(player, "general.unknown-command",
                    Placeholder.unparsed("usage", "/statshologram <" + String.join("|", SUBCOMMANDS) + ">"));
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(player, "general.unknown-command",
                    Placeholder.unparsed("usage", "/statshologram create <kills|deaths|killstreak|playtime> <name> [limit]"));
            return;
        }

        StatsManager.StatType type = StatsManager.StatType.fromKey(args[1]).orElse(null);
        if (type == null) {
            plugin.messages().send(player, "general.unknown-command",
                    Placeholder.unparsed("usage", "/statshologram create <kills|deaths|killstreak|playtime> <name> [limit]"));
            return;
        }

        String name = args[2];
        int limit = 10;
        if (args.length >= 4) {
            try {
                limit = Math.max(1, Math.min(45, Integer.parseInt(args[3])));
            } catch (NumberFormatException ex) {
                plugin.messages().send(player, "general.invalid-number", Placeholder.unparsed("input", args[3]));
                return;
            }
        }

        LeaderboardHologramManager.CreateResult result = hologramManager.create(name, player.getLocation(), type, limit);
        if (result == LeaderboardHologramManager.CreateResult.ALREADY_EXISTS) {
            plugin.messages().send(player, "stats.hologram-exists", Placeholder.unparsed("name", name));
            return;
        }
        plugin.messages().send(player, "stats.hologram-created",
                Placeholder.unparsed("name", name),
                Placeholder.unparsed("stat", args[1].toLowerCase(Locale.ROOT)));
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(player, "general.unknown-command", Placeholder.unparsed("usage", "/statshologram remove <name>"));
            return;
        }
        String name = args[1];
        if (!hologramManager.remove(name)) {
            plugin.messages().send(player, "stats.hologram-not-found", Placeholder.unparsed("name", name));
            return;
        }
        plugin.messages().send(player, "stats.hologram-removed", Placeholder.unparsed("name", name));
    }

    private void handleList(Player player) {
        List<LeaderboardHologramManager.HologramInfo> holograms = hologramManager.list();
        plugin.messages().send(player, "stats.hologram-list-header");
        for (LeaderboardHologramManager.HologramInfo info : holograms) {
            plugin.messages().send(player, "stats.hologram-list-entry",
                    Placeholder.unparsed("name", info.name()),
                    Placeholder.unparsed("stat", info.statType().name().toLowerCase(Locale.ROOT)));
        }
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
        if (hologramManager == null) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return plugin.subcommandAliases().labels("statshologram").stream()
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        String sub = plugin.subcommandAliases().resolve("statshologram", args[0]);
        if (args.length == 2 && sub.equals("create")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return STAT_TYPES.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && sub.equals("remove")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return hologramManager.list().stream().map(LeaderboardHologramManager.HologramInfo::name)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
        }
        return List.of();
    }
}
