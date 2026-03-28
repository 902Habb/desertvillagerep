package dev.hamzah.desertvillagerep.listener;

import dev.hamzah.desertvillagerep.model.RepCategory;
import dev.hamzah.desertvillagerep.service.BoardService;
import dev.hamzah.desertvillagerep.service.RepService;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class ProtectorListener implements Listener {
    private final RepService repService;
    private final BoardService boardService;

    public ProtectorListener(RepService repService, BoardService boardService) {
        this.repService = repService;
        this.boardService = boardService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        repService.changeRep(killer.getUniqueId(), killer.getName(), RepCategory.PROTECTOR, 1, null, null);
        boardService.scheduleRefresh();
    }

    public static boolean countsAsProtectorKill(EntityType entityType) {
        Class<?> entityClass = entityType.getEntityClass();
        return entityClass != null && Enemy.class.isAssignableFrom(entityClass);
    }
}
