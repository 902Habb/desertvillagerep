package dev.hamzah.desertvillagerep.listener;

import dev.hamzah.desertvillagerep.model.RepCategory;
import dev.hamzah.desertvillagerep.model.RegionType;
import dev.hamzah.desertvillagerep.service.BoardService;
import dev.hamzah.desertvillagerep.service.RegionService;
import dev.hamzah.desertvillagerep.service.RepService;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public final class ProtectorListener implements Listener {
    private static final Set<CreatureSpawnEvent.SpawnReason> ALLOWED_SPAWNS = EnumSet.of(
            CreatureSpawnEvent.SpawnReason.NATURAL,
            CreatureSpawnEvent.SpawnReason.CHUNK_GEN,
            CreatureSpawnEvent.SpawnReason.RAID,
            CreatureSpawnEvent.SpawnReason.PATROL,
            CreatureSpawnEvent.SpawnReason.REINFORCEMENTS
    );

    private static final Map<EntityType, Integer> REP_VALUES = createRepValues();

    private final RegionService regionService;
    private final RepService repService;
    private final BoardService boardService;
    private final Map<UUID, CreatureSpawnEvent.SpawnReason> spawnReasons = new java.util.HashMap<>();

    public ProtectorListener(RegionService regionService, RepService repService, BoardService boardService) {
        this.regionService = regionService;
        this.repService = repService;
        this.boardService = boardService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (REP_VALUES.containsKey(event.getEntityType())) {
            spawnReasons.put(event.getEntity().getUniqueId(), event.getSpawnReason());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Integer repValue = REP_VALUES.get(event.getEntityType());
        if (repValue == null) {
            return;
        }

        CreatureSpawnEvent.SpawnReason spawnReason = spawnReasons.remove(event.getEntity().getUniqueId());
        if (spawnReason == null || !ALLOWED_SPAWNS.contains(spawnReason)) {
            return;
        }

        if (!regionService.isInRegion(event.getEntity().getLocation(), RegionType.PROTECTOR)) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        repService.changeRep(killer.getUniqueId(), killer.getName(), RepCategory.PROTECTOR, repValue, null, null);
        boardService.scheduleRefresh();
    }

    private static Map<EntityType, Integer> createRepValues() {
        EnumMap<EntityType, Integer> map = new EnumMap<>(EntityType.class);
        map.put(EntityType.ZOMBIE, 1);
        map.put(EntityType.HUSK, 1);
        map.put(EntityType.SKELETON, 1);
        map.put(EntityType.SPIDER, 1);
        map.put(EntityType.CAVE_SPIDER, 1);
        map.put(EntityType.SLIME, 1);
        map.put(EntityType.CREEPER, 2);
        map.put(EntityType.WITCH, 2);
        map.put(EntityType.STRAY, 2);
        map.put(EntityType.PHANTOM, 2);
        map.put(EntityType.PILLAGER, 2);
        map.put(EntityType.VINDICATOR, 2);
        map.put(EntityType.RAVAGER, 3);
        map.put(EntityType.EVOKER, 3);
        return map;
    }
}

