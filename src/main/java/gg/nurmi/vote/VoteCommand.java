package gg.nurmi.vote;

import gg.nurmi.OneSMPPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

public final class VoteCommand implements CommandExecutor {

    private final OneSMPPlugin plugin;
    private final VoteManager voteManager;

    public VoteCommand(OneSMPPlugin plugin, VoteManager voteManager) {
        this.plugin = plugin;
        this.voteManager = voteManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!sender.hasPermission("onesmp.vote.use")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }

        List<Map<?, ?>> sites = plugin.getConfig().getMapList("vote.sites");
        if (sites.isEmpty()) {
            plugin.messages().send(sender, "vote.not-configured");
            return true;
        }

        plugin.messages().send(sender, "vote.header");
        for (Map<?, ?> entry : sites) {
            Object nameValue = entry.get("name");
            Object urlValue = entry.get("url");
            String name = nameValue != null ? String.valueOf(nameValue) : "";
            String url = urlValue != null ? String.valueOf(urlValue) : "";
            if (url.isBlank()) {
                continue;
            }
            Component urlComponent = Component.text(url)
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.openUrl(url))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to open", NamedTextColor.GRAY)));
            plugin.messages().send(sender, "vote.site-entry",
                    Placeholder.unparsed("name", name), Placeholder.component("url", urlComponent));
        }

        if (sender instanceof Player player) {
            voteManager.snapshot(player.getUniqueId()).thenAccept(snapshot ->
                    plugin.scheduler().runAtEntity(player, () -> plugin.messages().send(player, "vote.your-stats",
                            Placeholder.unparsed("total", String.valueOf(snapshot.totalVotes())),
                            Placeholder.unparsed("streak", String.valueOf(snapshot.currentStreak()))), () -> {}));
        }
        return true;
    }
}
