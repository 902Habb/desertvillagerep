package dev.hamzah.desertvillagerep.service;

import dev.hamzah.desertvillagerep.model.CuboidRegion;
import dev.hamzah.desertvillagerep.model.RegionType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class RegionService {
    private final Database database;
    private final Map<UUID, Location> pos1Selections = new HashMap<>();
    private final Map<UUID, Location> pos2Selections = new HashMap<>();

    public RegionService(Database database) {
        this.database = database;
    }

    public Location setPos1(Player player) {
        Location location = resolveSelectionLocation(player);
        pos1Selections.put(player.getUniqueId(), location);
        return location;
    }

    public Location setPos2(Player player) {
        Location location = resolveSelectionLocation(player);
        pos2Selections.put(player.getUniqueId(), location);
        return location;
    }

    public CuboidRegion saveRegion(Player player, RegionType type) {
        Location first = pos1Selections.get(player.getUniqueId());
        Location second = pos2Selections.get(player.getUniqueId());
        if (first == null || second == null) {
            return null;
        }
        if (first.getWorld() == null || second.getWorld() == null || !first.getWorld().equals(second.getWorld())) {
            throw new IllegalArgumentException("Selections must be in the same world");
        }
        CuboidRegion region = CuboidRegion.fromLocations(first, second);
        database.saveRegion(type, region);
        return region;
    }

    public CuboidRegion getRegion(RegionType type) {
        return database.getRegion(type);
    }

    public boolean isInRegion(Location location, RegionType type) {
        CuboidRegion region = getRegion(type);
        return region != null && region.contains(location);
    }

    public boolean isInVillageOrMarket(Location location) {
        return isInRegion(location, RegionType.VILLAGE) || isInRegion(location, RegionType.MARKET);
    }

    private Location resolveSelectionLocation(Player player) {
        Block targetBlock = player.getTargetBlockExact(100);
        Location selected = targetBlock != null
                ? targetBlock.getLocation()
                : player.getLocation().getBlock().getLocation();
        return selected.clone();
    }
}

