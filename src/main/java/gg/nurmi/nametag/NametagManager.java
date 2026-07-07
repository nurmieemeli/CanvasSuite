package gg.nurmi.nametag;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.CollisionRule;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.NameTagVisibility;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.OptionData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.ScoreBoardTeamInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.TeamMode;
import gg.nurmi.CanvasSuitePlugin;
import gg.nurmi.guild.Guild;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Overhead nametags, entirely packet-driven (no real Bukkit Scoreboard/Team or entity is ever
 * registered):
 * <ul>
 *   <li>Line 1 (the prefix in front of the player's real name) is a per-player scoreboard team
 *   packet, same as before.</li>
 *   <li>Line 2 (the guild tag, only present while the player is in a guild) is a fake TEXT_DISPLAY
 *   entity mounted as a passenger of the player - the same technique plugins like
 *   <a href="https://github.com/alexdev03/UnlimitedNametags">UnlimitedNametags</a> use, since
 *   vanilla team prefixes/suffixes can only add text to the *same* line as a player's name, never
 *   a second stacked line. Riding as a passenger means the client keeps it positioned correctly on
 *   its own; we only resend anything when the entity is created/destroyed or its text changes.</li>
 * </ul>
 *
 * <p>There's no LuckPerms hook available here (prefixes are resolved purely through the
 * MiniPlaceholders-LuckPerms expansion, never the LuckPerms API directly), so permission-group
 * changes can only be picked up by periodically re-rendering every online player's prefix — see
 * {@link #refreshAll()}, wired to a repeating task. The same refresh cycle also re-checks guild
 * membership.</p>
 */
public final class NametagManager {

    private final CanvasSuitePlugin plugin;
    private final Map<UUID, Component> lastSent = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> guildTagEntityIds = new ConcurrentHashMap<>();
    private final Map<UUID, Component> lastGuildTag = new ConcurrentHashMap<>();
    private final AtomicInteger fakeEntityIdCounter = new AtomicInteger(2_000_000_000);

    public NametagManager(CanvasSuitePlugin plugin) {
        this.plugin = plugin;
    }

    public void handleJoin(Player joined) {
        if (!plugin.packetEvents().available()) {
            return;
        }
        refresh(joined);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(joined)) {
                continue;
            }
            Component prefix = lastSent.get(other.getUniqueId());
            if (prefix != null) {
                sendPacketTo(joined, buildPacket(other, prefix, TeamMode.CREATE));
            }

            Integer entityId = guildTagEntityIds.get(other.getUniqueId());
            Component tag = lastGuildTag.get(other.getUniqueId());
            if (entityId != null && tag != null) {
                // Placeholder position only - the follow-up mount packet repositions it onto
                // `other` immediately, so where exactly this spawns for one tick doesn't matter.
                introduceGuildTagEntity(joined, entityId, joined.getLocation(), other.getEntityId(), tag);
            }
        }
    }

    public void handleQuit(Player left) {
        if (!plugin.packetEvents().available()) {
            return;
        }
        lastSent.remove(left.getUniqueId());
        WrapperPlayServerTeams removePacket = new WrapperPlayServerTeams(teamName(left), TeamMode.REMOVE, (ScoreBoardTeamInfo) null, List.of());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(left)) {
                sendPacketTo(viewer, removePacket);
            }
        }

        Integer entityId = guildTagEntityIds.remove(left.getUniqueId());
        lastGuildTag.remove(left.getUniqueId());
        if (entityId != null) {
            destroyGuildTagEntity(entityId);
        }
    }

    /** Re-renders one player's prefix/guild tag and broadcasts either only if actually changed since last sent. */
    public void refresh(Player player) {
        if (!plugin.packetEvents().available()) {
            return;
        }
        plugin.scheduler().runAtEntity(player, () -> {
            Component prefix = plugin.messages().render(player, "nametag.format");
            Component previous = lastSent.put(player.getUniqueId(), prefix);
            boolean firstTime = previous == null;
            if (firstTime || !previous.equals(prefix)) {
                WrapperPlayServerTeams packet = buildPacket(player, prefix, firstTime ? TeamMode.CREATE : TeamMode.UPDATE);
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    sendPacketTo(viewer, packet);
                }
            }

            Location location = player.getLocation();
            plugin.guilds().getGuildByMember(player.getUniqueId())
                    .thenAccept(optionalGuild -> applyGuildTag(player, location, optionalGuild.map(Guild::tag).orElse(null)));
        }, () -> {});
    }

    public void refreshAll() {
        if (!plugin.packetEvents().available()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    private void applyGuildTag(Player player, Location spawnLocation, String tag) {
        UUID uuid = player.getUniqueId();

        if (tag == null) {
            Integer entityId = guildTagEntityIds.remove(uuid);
            lastGuildTag.remove(uuid);
            if (entityId != null) {
                destroyGuildTagEntity(entityId);
            }
            return;
        }

        String format = plugin.getConfig().getString("nametag.guild-tag.format", "<gray>[<tag>]");
        Component rendered = plugin.messages().parse(format, player, Placeholder.unparsed("tag", tag));

        Integer entityId = guildTagEntityIds.get(uuid);
        if (entityId == null) {
            int newId = fakeEntityIdCounter.decrementAndGet();
            guildTagEntityIds.put(uuid, newId);
            lastGuildTag.put(uuid, rendered);
            spawnGuildTagEntity(newId, player, spawnLocation, rendered);
            return;
        }

        Component previous = lastGuildTag.put(uuid, rendered);
        if (!rendered.equals(previous)) {
            sendTextUpdate(entityId, rendered);
        }
    }

    private void spawnGuildTagEntity(int entityId, Player owner, Location location, Component text) {
        PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
        WrapperPlayServerSpawnEntity spawn = buildSpawnPacket(entityId, location);
        WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, buildMetadata(text));

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            playerManager.sendPacket(viewer, spawn);
            playerManager.sendPacket(viewer, metaPacket);
        }
        mountNextTick(entityId, owner.getEntityId());
    }

    /** Spawns+mounts the entity for a single newly-joined viewer, who has never seen it before. */
    private void introduceGuildTagEntity(Player viewer, int entityId, Location placeholderLocation, int ownerEntityId, Component text) {
        PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
        playerManager.sendPacket(viewer, buildSpawnPacket(entityId, placeholderLocation));
        playerManager.sendPacket(viewer, new WrapperPlayServerEntityMetadata(entityId, buildMetadata(text)));
        plugin.scheduler().runGlobalDelayed(() ->
                playerManager.sendPacket(viewer, new WrapperPlayServerSetPassengers(ownerEntityId, new int[]{entityId})), 1L);
    }

    private void mountNextTick(int entityId, int ownerEntityId) {
        plugin.scheduler().runGlobalDelayed(() -> {
            PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
            WrapperPlayServerSetPassengers mount = new WrapperPlayServerSetPassengers(ownerEntityId, new int[]{entityId});
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                playerManager.sendPacket(viewer, mount);
            }
        }, 1L);
    }

    private WrapperPlayServerSpawnEntity buildSpawnPacket(int entityId, Location location) {
        Vector3d position = new Vector3d(location.getX(), location.getY(), location.getZ());
        return new WrapperPlayServerSpawnEntity(entityId, java.util.Optional.of(UUID.randomUUID()),
                EntityTypes.TEXT_DISPLAY, position, 0f, 0f, 0f, 0, java.util.Optional.empty());
    }

    /** Index table per the vanilla Display/TextDisplay entity metadata layout - see Minecraft's protocol docs. */
    private List<EntityData<?>> buildMetadata(Component text) {
        List<EntityData<?>> list = new ArrayList<>();
        list.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true)); // no gravity
        float yOffset = (float) plugin.getConfig().getDouble("nametag.guild-tag.y-offset", 0.3);
        list.add(new EntityData<>(11, EntityDataTypes.VECTOR3F, new Vector3f(0f, yOffset, 0f))); // translation
        list.add(new EntityData<>(15, EntityDataTypes.BYTE, (byte) 3)); // billboard: CENTER (always faces viewer)
        list.add(new EntityData<>(23, EntityDataTypes.ADV_COMPONENT, text)); // text
        return list;
    }

    private void sendTextUpdate(int entityId, Component text) {
        List<EntityData<?>> metadata = new ArrayList<>();
        metadata.add(new EntityData<>(23, EntityDataTypes.ADV_COMPONENT, text));
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            playerManager.sendPacket(viewer, packet);
        }
    }

    private void destroyGuildTagEntity(int entityId) {
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(entityId);
        PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            playerManager.sendPacket(viewer, destroy);
        }
    }

    private WrapperPlayServerTeams buildPacket(Player subject, Component prefix, TeamMode mode) {
        ScoreBoardTeamInfo info = new ScoreBoardTeamInfo(Component.empty(), prefix, Component.empty(),
                NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, NamedTextColor.WHITE, OptionData.NONE);
        List<String> members = mode == TeamMode.CREATE ? List.of(subject.getName()) : List.of();
        return new WrapperPlayServerTeams(teamName(subject), mode, info, members);
    }

    private void sendPacketTo(Player viewer, WrapperPlayServerTeams packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }

    private String teamName(Player player) {
        // Team name cap is only 16 chars pre-1.18; stay conservative for older clients.
        return "csnt_" + player.getUniqueId().toString().replace("-", "").substring(0, 11);
    }
}
