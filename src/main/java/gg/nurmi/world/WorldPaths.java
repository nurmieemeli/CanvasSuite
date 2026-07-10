package gg.nurmi.world;

// Bukkit treats a '/'-containing WorldCreator name as both a nested path and the world's identifier,
// so a container prefix just needs folding into the name consistently.
public final class WorldPaths {

    private WorldPaths() {}

    public static String resolve(String container, String worldName) {
        if (container == null || container.isBlank()) {
            return worldName;
        }
        String normalized = normalize(container);
        return normalized.isEmpty() ? worldName : normalized + "/" + worldName;
    }

    // Reverses resolve(): strips the container prefix off a Bukkit world name so it can be matched
    // against the logical name admins actually configure (e.g. in rtp.worlds).
    public static String strip(String container, String storedName) {
        if (container == null || container.isBlank()) {
            return storedName;
        }
        String prefix = normalize(container) + "/";
        return storedName.startsWith(prefix) ? storedName.substring(prefix.length()) : storedName;
    }

    private static String normalize(String container) {
        String normalized = container.replace('\\', '/');
        int start = 0;
        int end = normalized.length();
        while (start < end && normalized.charAt(start) == '/') {
            start++;
        }
        while (end > start && normalized.charAt(end - 1) == '/') {
            end--;
        }
        return normalized.substring(start, end);
    }
}
