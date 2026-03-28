package dev.hamzah.desertvillagerep.listener;

import dev.hamzah.desertvillagerep.config.PluginConfig;
import dev.hamzah.desertvillagerep.model.RepCategory;
import dev.hamzah.desertvillagerep.service.BoardService;
import dev.hamzah.desertvillagerep.service.RegionService;
import dev.hamzah.desertvillagerep.service.RepService;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public final class BuilderListener implements Listener {
    private final PluginConfig pluginConfig;
    private final RegionService regionService;
    private final RepService repService;
    private final BoardService boardService;

    public BuilderListener(PluginConfig pluginConfig, RegionService regionService, RepService repService, BoardService boardService) {
        this.pluginConfig = pluginConfig;
        this.regionService = regionService;
        this.repService = repService;
        this.boardService = boardService;
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

        repService.changeRep(event.getPlayer().getUniqueId(), event.getPlayer().getName(), RepCategory.BUILDER, points, null, null);
        boardService.scheduleRefresh();
    }
}
