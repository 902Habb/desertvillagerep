package dev.hamzah.desertvillagerep.listener;

import dev.hamzah.desertvillagerep.config.PluginConfig;
import dev.hamzah.desertvillagerep.model.PositionKey;
import dev.hamzah.desertvillagerep.model.RepCategory;
import dev.hamzah.desertvillagerep.service.BoardService;
import dev.hamzah.desertvillagerep.service.RegionService;
import dev.hamzah.desertvillagerep.service.RepService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.entity.ArmorStand;

public final class BuilderListener implements Listener {
    private static final long LOOP_WINDOW_MILLIS = 60_000L;

    private record BreakRecord(UUID playerUuid, long timestamp) {
    }

    private final PluginConfig pluginConfig;
    private final RegionService regionService;
    private final RepService repService;
    private final BoardService boardService;
    private final Map<PositionKey, BreakRecord> recentBreaks = new HashMap<>();

    public BuilderListener(PluginConfig pluginConfig, RegionService regionService, RepService repService, BoardService boardService) {
        this.pluginConfig = pluginConfig;
        this.regionService = regionService;
        this.repService = repService;
        this.boardService = boardService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        if (!regionService.isInVillageOrMarket(location)) {
            return;
        }
        recentBreaks.put(PositionKey.fromLocation(location), new BreakRecord(event.getPlayer().getUniqueId(), System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Location location = event.getBlockPlaced().getLocation();
        if (!regionService.isInVillageOrMarket(location)) {
            return;
        }

        int points = pluginConfig.builderPointsFor(event.getBlockPlaced().getType());
        if (points <= 0) {
            return;
        }

        PositionKey key = PositionKey.fromLocation(location);
        BreakRecord breakRecord = recentBreaks.get(key);
        if (breakRecord != null) {
            long age = System.currentTimeMillis() - breakRecord.timestamp();
            if (breakRecord.playerUuid().equals(event.getPlayer().getUniqueId()) && age <= LOOP_WINDOW_MILLIS) {
                return;
            }
            if (age > LOOP_WINDOW_MILLIS) {
                recentBreaks.remove(key);
            }
        }

        repService.changeRep(event.getPlayer().getUniqueId(), event.getPlayer().getName(), RepCategory.BUILDER, points, null, null);
        boardService.scheduleRefresh();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorStandPlace(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ArmorStand)) {
            return;
        }
        if (!regionService.isInVillageOrMarket(event.getEntity().getLocation())) {
            return;
        }
        repService.changeRep(event.getPlayer().getUniqueId(), event.getPlayer().getName(), RepCategory.BUILDER, 2, null, null);
        boardService.scheduleRefresh();
    }
}
