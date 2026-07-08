package gg.nurmi.crate;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public record CrateReward(String key, double weight, String displayName, Map<Material, Integer> items,
                           double money, List<String> commands, boolean broadcast, Material displayItem) {

    // displayItem overrides the icon shown in the reel/preview; otherwise falls back to the first item, or a generic icon for money/command-only rewards.
    public Material iconMaterial() {
        if (displayItem != null) {
            return displayItem;
        }
        return items.keySet().stream().findFirst().orElse(money > 0 ? Material.GOLD_INGOT : Material.NETHER_STAR);
    }
}
