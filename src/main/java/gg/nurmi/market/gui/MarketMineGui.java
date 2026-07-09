package gg.nurmi.market.gui;

import gg.nurmi.OneSMPPlugin;
import gg.nurmi.gui.AbstractGui;
import gg.nurmi.gui.Pagination;
import gg.nurmi.market.MarketActions;
import gg.nurmi.market.MarketListing;
import gg.nurmi.market.MarketManager;
import gg.nurmi.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class MarketMineGui extends AbstractGui {

    public MarketMineGui(OneSMPPlugin plugin, MarketManager marketManager, List<MarketListing> listings, int page) {
        super(plugin, plugin.messages().parse("<gradient:#34d399:#10b981><bold>My Listings</bold></gradient>"), 6);

        Pagination<MarketListing> pagination = new Pagination<>(listings, PAGE_SIZE);
        List<MarketListing> pageListings = pagination.page(page);

        int slot = 0;
        for (MarketListing listing : pageListings) {
            setButton(slot++, buildIcon(plugin, listing), event -> {
                if (event.getWhoClicked() instanceof Player player) {
                    player.closeInventory();
                    MarketActions.cancel(plugin, marketManager, player, listing.id());
                }
            });
        }

        addPaginationFooter(pagination, page, (player, targetPage) ->
                new MarketMineGui(plugin, marketManager, listings, targetPage).open(player));
    }

    private static ItemStack buildIcon(OneSMPPlugin plugin, MarketListing listing) {
        ItemMeta meta = listing.item().getItemMeta();
        List<Component> lore = new ArrayList<>(meta != null && meta.hasLore() ? meta.lore() : List.of());
        lore.add(plugin.messages().parse(" "));
        lore.add(plugin.messages().parse("<gray>Price: <green><price>",
                Placeholder.unparsed("price", plugin.economy().format(listing.price()))));
        lore.add(plugin.messages().parse("<dark_gray>Click to cancel and reclaim"));

        return new ItemBuilder(listing.item())
                .lore(lore)
                .build();
    }
}
