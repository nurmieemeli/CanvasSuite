package gg.nurmi.world;

// Bukkit treats a WorldCreator name containing '/' as a nested path relative to the world container, and
// uses that same string as the world's identifier (Bukkit.getWorld(name)) - so a configurable container
// prefix just needs to be folded into the name consistently everywhere a world is created or looked up.
public final class WorldPaths {

    private WorldPaths() {}

    public static String resolve(String container, String worldName) {
        if (container == null || container.isBlank()) {
            return worldName;
        }
        String normalized = container.replace('\\', '/');
        int start = 0;
        int end = normalized.length();
        while (start < end && normalized.charAt(start) == '/') {
            start++;
        }
        while (end > start && normalized.charAt(end - 1) == '/') {
            end--;
        }
        normalized = normalized.substring(start, end);
        return normalized.isEmpty() ? worldName : normalized + "/" + worldName;
    }
}
