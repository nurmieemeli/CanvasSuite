package gg.nurmi.market;

import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.UUID;

public record MarketListing(int id, UUID sellerUuid, String sellerName, ItemStack item, BigDecimal price, long createdAt) {
}
