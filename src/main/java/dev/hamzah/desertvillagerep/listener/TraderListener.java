package dev.hamzah.desertvillagerep.listener;

import dev.hamzah.desertvillagerep.model.RepCategory;
import dev.hamzah.desertvillagerep.model.RegionType;
import dev.hamzah.desertvillagerep.service.BoardService;
import dev.hamzah.desertvillagerep.service.RegionService;
import dev.hamzah.desertvillagerep.service.RepService;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.EquipmentSlot;

public final class TraderListener implements Listener {
    private static final long FEED_WINDOW_MILLIS = 60_000L;
    private static final long CURE_WINDOW_MILLIS = 15 * 60_000L;
    private static final long CARETAKER_COOLDOWN_MILLIS = 30_000L;
    private static final Set<Material> BREEDING_FOODS = Set.of(Material.BREAD, Material.CARROT, Material.POTATO, Material.BEETROOT);
    private static final Set<Material> WORKSTATIONS = EnumSet.of(
            Material.BLAST_FURNACE,
            Material.SMOKER,
            Material.CARTOGRAPHY_TABLE,
            Material.FLETCHING_TABLE,
            Material.GRINDSTONE,
            Material.LECTERN,
            Material.LOOM,
            Material.SMITHING_TABLE,
            Material.STONECUTTER,
            Material.COMPOSTER,
            Material.BARREL,
            Material.BREWING_STAND,
            Material.CAULDRON
    );

    private record InteractionRecord(UUID playerUuid, String playerName, long timestamp) {
    }

    private final RegionService regionService;
    private final RepService repService;
    private final BoardService boardService;
    private final Map<UUID, InteractionRecord> villagerFeedRecords = new HashMap<>();
    private final Map<UUID, InteractionRecord> zombieCureRecords = new HashMap<>();
    private final Map<UUID, Long> workstationCooldowns = new HashMap<>();
    private final Map<UUID, Long> bedCooldowns = new HashMap<>();

    public TraderListener(RegionService regionService, RepService repService, BoardService boardService) {
        this.regionService = regionService;
        this.repService = repService;
        this.boardService = boardService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMerchantTrade(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory() instanceof MerchantInventory)) {
            return;
        }
        if (event.getRawSlot() != 2 || event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
            return;
        }
        repService.changeRep(player.getUniqueId(), player.getName(), RepCategory.TRADER, 1, null, null);
        boardService.scheduleRefresh();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return;
        }

        if (event.getRightClicked() instanceof Villager villager && BREEDING_FOODS.contains(item.getType())) {
            villagerFeedRecords.put(villager.getUniqueId(), new InteractionRecord(player.getUniqueId(), player.getName(), System.currentTimeMillis()));
            return;
        }

        if (event.getRightClicked() instanceof ZombieVillager zombieVillager && item.getType() == Material.GOLDEN_APPLE) {
            zombieCureRecords.put(zombieVillager.getUniqueId(), new InteractionRecord(player.getUniqueId(), player.getName(), System.currentTimeMillis()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerBreed(EntityBreedEvent event) {
        if (!(event.getEntity() instanceof Villager)) {
            return;
        }

        UUID playerUuid = null;
        String playerName = null;

        if (event.getBreeder() instanceof Player player) {
            playerUuid = player.getUniqueId();
            playerName = player.getName();
        } else {
            InteractionRecord candidate = selectFeedRecord(event.getMother(), event.getFather());
            if (candidate != null) {
                playerUuid = candidate.playerUuid();
                playerName = candidate.playerName();
            }
        }

        if (playerUuid == null) {
            return;
        }

        repService.changeRep(playerUuid, playerName, RepCategory.TRADER, 3, null, null);
        boardService.scheduleRefresh();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onZombieVillagerCured(EntityTransformEvent event) {
        if (!(event.getEntity() instanceof ZombieVillager zombieVillager)) {
            return;
        }
        if (!(event.getTransformedEntity() instanceof Villager)) {
            return;
        }
        if (event.getTransformReason() != EntityTransformEvent.TransformReason.CURED) {
            return;
        }

        InteractionRecord record = zombieCureRecords.remove(zombieVillager.getUniqueId());
        if (record == null) {
            return;
        }
        if (System.currentTimeMillis() - record.timestamp() > CURE_WINDOW_MILLIS) {
            return;
        }
        repService.changeRep(record.playerUuid(), record.playerName(), RepCategory.TRADER, 100, null, null);
        boardService.scheduleRefresh();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillageSupportBlockPlaced(BlockPlaceEvent event) {
        if (!regionService.isInRegion(event.getBlockPlaced().getLocation(), RegionType.VILLAGE)) {
            return;
        }

        Material material = event.getBlockPlaced().getType();
        Player player = event.getPlayer();
        if (WORKSTATIONS.contains(material)) {
            if (cooldownReady(workstationCooldowns, player.getUniqueId()) && hasNearbyUnemployedVillager(event.getBlockPlaced().getLocation(), 6.0)) {
                workstationCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                repService.changeRep(player.getUniqueId(), player.getName(), RepCategory.TRADER, 1, null, null);
                boardService.scheduleRefresh();
            }
            return;
        }

        if (Tag.BEDS.isTagged(material) && cooldownReady(bedCooldowns, player.getUniqueId()) && hasNearbyVillager(event.getBlockPlaced().getLocation(), 8.0)) {
            bedCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            repService.changeRep(player.getUniqueId(), player.getName(), RepCategory.TRADER, 1, null, null);
            boardService.scheduleRefresh();
        }
    }

    private InteractionRecord selectFeedRecord(Entity mother, Entity father) {
        InteractionRecord first = mother == null ? null : villagerFeedRecords.get(mother.getUniqueId());
        InteractionRecord second = father == null ? null : villagerFeedRecords.get(father.getUniqueId());

        first = freshEnough(first, FEED_WINDOW_MILLIS);
        second = freshEnough(second, FEED_WINDOW_MILLIS);

        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.timestamp() >= second.timestamp() ? first : second;
    }

    private InteractionRecord freshEnough(InteractionRecord record, long windowMillis) {
        if (record == null) {
            return null;
        }
        if (System.currentTimeMillis() - record.timestamp() > windowMillis) {
            return null;
        }
        return record;
    }

    private boolean cooldownReady(Map<UUID, Long> cooldowns, UUID playerUuid) {
        long lastUsedAt = cooldowns.getOrDefault(playerUuid, 0L);
        return System.currentTimeMillis() - lastUsedAt >= CARETAKER_COOLDOWN_MILLIS;
    }

    private boolean hasNearbyUnemployedVillager(org.bukkit.Location location, double radius) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
            if (entity instanceof Villager villager && villager.getProfession() == Villager.Profession.NONE) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNearbyVillager(org.bukkit.Location location, double radius) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
            if (entity instanceof Villager) {
                return true;
            }
        }
        return false;
    }
}
