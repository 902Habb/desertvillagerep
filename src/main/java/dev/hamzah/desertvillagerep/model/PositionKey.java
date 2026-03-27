package dev.hamzah.desertvillagerep.model;

import org.bukkit.Location;

public record PositionKey(String worldName, int x, int y, int z) {
    public static PositionKey fromLocation(Location location) {
        return new PositionKey(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }
}

