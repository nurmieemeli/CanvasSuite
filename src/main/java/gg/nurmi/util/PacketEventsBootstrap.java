package gg.nurmi.util;

import gg.nurmi.OneSMPPlugin;
import org.bukkit.Bukkit;

public final class PacketEventsBootstrap {

    private final OneSMPPlugin plugin;
    private boolean available;

    public PacketEventsBootstrap(OneSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void detect() {
        this.available = Bukkit.getPluginManager().getPlugin("packetevents") != null;
        if (!available) {
            plugin.getLogger().warning("PacketEvents not found - nametags, moderator vanish, and the "
                    + "reserved-slot tablist will be disabled. Install the PacketEvents plugin to enable them.");
        }
    }

    public boolean available() {
        return available;
    }
}
