package gg.nurmi.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Silently rate-limits rapid repeat actions per player+category (e.g. spam-clicking a GUI button, or
// spamming a chat command faster than its async round-trip) - no message or sound on rejection, since the
// point is just to absorb the spam quietly, not to give feedback about it.
public final class ActionCooldown {

    private final Map<UUID, Map<String, Long>> lastActionMillis = new ConcurrentHashMap<>();

    // Returns true if the action may proceed (and records this as the new last-attempt time), false if
    // it's still within cooldownMillis of the last attempt for this player+category and should be ignored.
    public boolean tryAcquire(UUID uuid, String category, long cooldownMillis) {
        long now = System.currentTimeMillis();
        Long previous = lastActionMillis.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>()).put(category, now);
        return previous == null || now - previous >= cooldownMillis;
    }

    public void clear(UUID uuid) {
        lastActionMillis.remove(uuid);
    }
}
