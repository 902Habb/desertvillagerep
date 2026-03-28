package dev.hamzah.desertvillagerep.service;

import dev.hamzah.desertvillagerep.config.PluginConfig;
import dev.hamzah.desertvillagerep.model.BoardAnchor;
import dev.hamzah.desertvillagerep.model.LeaderboardEntry;
import dev.hamzah.desertvillagerep.model.RepCategory;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class BoardService {
    private static final String BOARD_TAG = "desertrep_board";
    private static final int ROWS_PER_COLUMN = 4;

    private final JavaPlugin plugin;
    private final PluginConfig pluginConfig;
    private final Database database;
    private final RepService repService;
    private final Map<String, UUID> displayIds = new HashMap<>();
    private BukkitTask repeatingRefreshTask;
    private BukkitTask queuedRefreshTask;

    public BoardService(JavaPlugin plugin, PluginConfig pluginConfig, Database database, RepService repService) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.database = database;
        this.repService = repService;
    }

    public void start() {
        cleanupExistingBoardEntities();
        refreshNow();
        repeatingRefreshTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::refreshNow,
                pluginConfig.boardRefreshTicks(),
                pluginConfig.boardRefreshTicks()
        );
    }

    public void shutdown() {
        if (queuedRefreshTask != null) {
            queuedRefreshTask.cancel();
            queuedRefreshTask = null;
        }
        if (repeatingRefreshTask != null) {
            repeatingRefreshTask.cancel();
            repeatingRefreshTask = null;
        }
        cleanupExistingBoardEntities();
        displayIds.clear();
    }

    public void setBoardAnchor(Location location) {
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Board location must be in a world");
        }
        database.saveBoardAnchor(new BoardAnchor(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                0.0f
        ));
        recreateBoard();
    }

    public void removeBoard() {
        database.deleteBoardAnchor();
        cleanupExistingBoardEntities();
        displayIds.clear();
    }

    public void scheduleRefresh() {
        if (queuedRefreshTask != null) {
            return;
        }
        queuedRefreshTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            queuedRefreshTask = null;
            refreshNow();
        }, 10L);
    }

    public void refreshNow() {
        BoardAnchor anchor = database.getBoardAnchor();
        if (anchor == null) {
            return;
        }
        if (!hasAllDisplays()) {
            recreateBoard();
            anchor = database.getBoardAnchor();
            if (anchor == null) {
                return;
            }
        }
        updateTexts(anchor);
    }

    private void recreateBoard() {
        BoardAnchor anchor = database.getBoardAnchor();
        if (anchor == null) {
            return;
        }
        cleanupExistingBoardEntities();
        displayIds.clear();
        World world = Bukkit.getWorld(anchor.worldName());
        if (world == null) {
            return;
        }

        spawn(anchor, "title", 0.0, 0.0, 0.0, pluginConfig.boardTitle());
        double[] columns = {-3.6, 0.0, 3.6};
        RepCategory[] categories = RepCategory.values();
        for (int index = 0; index < categories.length; index++) {
            RepCategory category = categories[index];
            spawn(anchor, "header:" + category.name(), columns[index], -0.55, 0.0, colorFor(category) + category.displayName());
            for (int row = 0; row < ROWS_PER_COLUMN; row++) {
                spawn(anchor, key(category, row), columns[index], -0.95 - (row * 0.33), 0.0, ChatColor.GRAY + "-");
            }
        }
        spawn(anchor, "footer", 0.0, -2.45, 0.0, ChatColor.DARK_GRAY + "No rankings yet");
        updateTexts(anchor);
    }

    private boolean hasAllDisplays() {
        int expected = 1 + RepCategory.values().length + (RepCategory.values().length * ROWS_PER_COLUMN) + 1;
        if (displayIds.size() != expected) {
            return false;
        }
        for (UUID id : displayIds.values()) {
            Entity entity = Bukkit.getEntity(id);
            if (!(entity instanceof TextDisplay textDisplay) || !textDisplay.isValid()) {
                return false;
            }
        }
        return true;
    }

    private void cleanupExistingBoardEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
                if (display.getScoreboardTags().contains(BOARD_TAG)) {
                    display.remove();
                }
            }
        }
    }

    private void updateTexts(BoardAnchor anchor) {
        Map<RepCategory, List<LeaderboardEntry>> tops = new EnumMap<>(RepCategory.class);
        for (RepCategory category : RepCategory.values()) {
            tops.put(category, repService.getTop(category, ROWS_PER_COLUMN));
        }

        for (RepCategory category : RepCategory.values()) {
            List<LeaderboardEntry> entries = tops.get(category);
            for (int row = 0; row < ROWS_PER_COLUMN; row++) {
                TextDisplay display = display(key(category, row));
                if (display != null) {
                    display.setText(formatEntry(entries, row));
                }
            }
        }

        TextDisplay footer = display("footer");
        if (footer != null) {
            footer.setText(formatFooter(tops));
        }
    }

    private String formatEntry(List<LeaderboardEntry> entries, int row) {
        if (entries == null || row >= entries.size()) {
            return ChatColor.DARK_GRAY + "#" + (row + 1) + " -";
        }
        LeaderboardEntry entry = entries.get(row);
        return ChatColor.WHITE + "#" + (row + 1) + " " + shortName(entry.name()) + " " + ChatColor.GOLD + entry.score();
    }

    private String formatFooter(Map<RepCategory, List<LeaderboardEntry>> tops) {
        StringBuilder builder = new StringBuilder(ChatColor.GOLD.toString());
        appendFooterSegment(builder, tops, RepCategory.BUILDER);
        builder.append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.GOLD);
        appendFooterSegment(builder, tops, RepCategory.TRADER);
        builder.append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.GOLD);
        appendFooterSegment(builder, tops, RepCategory.PROTECTOR);
        return builder.toString();
    }

    private void appendFooterSegment(StringBuilder builder, Map<RepCategory, List<LeaderboardEntry>> tops, RepCategory category) {
        List<LeaderboardEntry> entries = tops.get(category);
        String winner = (entries == null || entries.isEmpty()) ? "-" : shortName(entries.get(0).name());
        builder.append(category.winnerTitle()).append(": ").append(winner);
    }

    private void spawn(BoardAnchor anchor, String key, double rightOffset, double verticalOffset, double forwardOffset, String text) {
        World world = Bukkit.getWorld(anchor.worldName());
        if (world == null) {
            return;
        }
        Location location = orientedOffset(anchor, rightOffset, verticalOffset, forwardOffset);
        TextDisplay display = world.spawn(location, TextDisplay.class, entity -> {
            entity.setText(text);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setSeeThrough(false);
            entity.setShadowed(true);
            entity.setViewRange(pluginConfig.boardVisibilityRange());
            entity.addScoreboardTag(BOARD_TAG);
            entity.setRotation(anchor.yaw(), anchor.pitch());
        });
        displayIds.put(key, display.getUniqueId());
    }

    private Location orientedOffset(BoardAnchor anchor, double rightOffset, double verticalOffset, double forwardOffset) {
        World world = Bukkit.getWorld(anchor.worldName());
        Location base = new Location(world, anchor.x(), anchor.y(), anchor.z(), anchor.yaw(), anchor.pitch());
        double yawRadians = Math.toRadians(anchor.yaw());
        Vector forward = new Vector(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians)).normalize();
        Vector right = new Vector(forward.getZ(), 0.0, -forward.getX()).normalize();
        return base.clone()
                .add(right.multiply(rightOffset))
                .add(0.0, verticalOffset, 0.0)
                .add(forward.multiply(forwardOffset));
    }

    private TextDisplay display(String key) {
        UUID entityId = displayIds.get(key);
        if (entityId == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(entityId);
        if (entity instanceof TextDisplay textDisplay) {
            return textDisplay;
        }
        return null;
    }

    private String key(RepCategory category, int row) {
        return "row:" + category.name() + ":" + row;
    }

    private String shortName(String name) {
        if (name == null || name.isBlank()) {
            return "-";
        }
        return name.length() <= 10 ? name : name.substring(0, 10);
    }

    private ChatColor colorFor(RepCategory category) {
        return switch (category) {
            case BUILDER -> ChatColor.YELLOW;
            case TRADER -> ChatColor.AQUA;
            case PROTECTOR -> ChatColor.RED;
        };
    }
}
