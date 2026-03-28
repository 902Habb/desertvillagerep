package dev.hamzah.desertvillagerep.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hamzah.desertvillagerep.model.LegacyStatsRecord;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class LegacyImportService {
    public record ImportSummary(int scanned, int imported, String sourcePath) {
    }

    private static final Set<String> HOSTILE_KILL_KEYS = Set.of(
            "minecraft:zombie",
            "minecraft:husk",
            "minecraft:skeleton",
            "minecraft:spider",
            "minecraft:cave_spider",
            "minecraft:creeper",
            "minecraft:witch",
            "minecraft:stray",
            "minecraft:phantom",
            "minecraft:pillager",
            "minecraft:vindicator",
            "minecraft:ravager",
            "minecraft:evoker",
            "minecraft:slime"
    );

    private final JavaPlugin plugin;
    private final Database database;
    private final RepService repService;

    public LegacyImportService(JavaPlugin plugin, Database database, RepService repService) {
        this.plugin = plugin;
        this.database = database;
        this.repService = repService;
    }

    public ImportSummary importAll() {
        File statsDirectory = resolveStatsDirectory();
        if (statsDirectory == null) {
            return new ImportSummary(0, 0, "stats directory not found");
        }

        File[] statFiles = statsDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (statFiles == null) {
            return new ImportSummary(0, 0, statsDirectory.getAbsolutePath());
        }

        int imported = 0;
        for (File statFile : statFiles) {
            if (importFile(statFile)) {
                imported++;
            }
        }
        return new ImportSummary(statFiles.length, imported, statsDirectory.getAbsolutePath());
    }

    public ImportSummary importPlayer(OfflinePlayer player) {
        File statsDirectory = resolveStatsDirectory();
        if (statsDirectory == null) {
            return new ImportSummary(0, 0, "stats directory not found");
        }
        File statFile = new File(statsDirectory, player.getUniqueId() + ".json");
        if (!statFile.exists()) {
            return new ImportSummary(1, 0, statFile.getAbsolutePath());
        }
        boolean imported = importFile(statFile);
        return new ImportSummary(1, imported ? 1 : 0, statFile.getAbsolutePath());
    }

    private boolean importFile(File statFile) {
        String fileName = statFile.getName();
        String uuidSection = fileName.substring(0, fileName.length() - 5);
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidSection);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Skipping malformed stats file: " + fileName);
            return false;
        }

        try (FileReader fileReader = new FileReader(statFile)) {
            JsonObject root = JsonParser.parseReader(fileReader).getAsJsonObject();
            JsonObject stats = object(root, "stats");
            JsonObject custom = object(stats, "minecraft:custom");
            JsonObject killed = object(stats, "minecraft:killed");
            JsonObject used = object(stats, "minecraft:used");

            int estimatedBlocksPlaced = sumUsedBlockItems(used);
            int villagerTrades = integer(custom, "minecraft:traded_with_villager");
            long playTime = longValue(custom, "minecraft:play_time");
            if (playTime == 0L) {
                playTime = longValue(custom, "minecraft:play_one_minute");
            }
            int deaths = integer(custom, "minecraft:deaths");
            long walkedCentimeters = longValue(custom, "minecraft:walk_one_cm");
            int hostileKills = sumHostileKills(killed);

            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String playerName = player.getName() != null ? player.getName() : uuid.toString().substring(0, 8);
            LegacyStatsRecord record = new LegacyStatsRecord(
                    uuid,
                    playerName,
                    estimatedBlocksPlaced,
                    villagerTrades,
                    playTime,
                    deaths,
                    hostileKills,
                    walkedCentimeters,
                    Instant.now().toString()
            );

            repService.saveLegacyStats(record);
            repService.setLegacyBuilderSeed(uuid, playerName, estimatedBlocksPlaced);
            repService.setLegacyTraderSeed(uuid, playerName, villagerTrades);
            return true;
        } catch (IOException | IllegalStateException exception) {
            plugin.getLogger().warning("Failed to import legacy stats from " + statFile.getName() + ": " + exception.getMessage());
            return false;
        }
    }

    private File resolveStatsDirectory() {
        if (Bukkit.getWorlds().isEmpty()) {
            return null;
        }
        Set<File> candidates = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            candidates.add(new File(world.getWorldFolder(), "stats"));
        }
        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isDirectory()) {
                return candidate;
            }
        }
        return null;
    }

    private JsonObject object(JsonObject source, String key) {
        JsonElement element = source.get(key);
        if (element == null || !element.isJsonObject()) {
            return new JsonObject();
        }
        return element.getAsJsonObject();
    }

    private int integer(JsonObject source, String key) {
        JsonElement element = source.get(key);
        return element == null ? 0 : element.getAsInt();
    }

    private long longValue(JsonObject source, String key) {
        JsonElement element = source.get(key);
        return element == null ? 0L : element.getAsLong();
    }

    private int sumHostileKills(JsonObject killed) {
        int total = 0;
        for (String key : HOSTILE_KILL_KEYS) {
            total += integer(killed, key);
        }
        return total;
    }

    private int sumUsedBlockItems(JsonObject used) {
        int total = 0;
        for (var entry : used.entrySet()) {
            Material material = materialFromStatKey(entry.getKey());
            if (material == null || !material.isBlock()) {
                continue;
            }
            total += entry.getValue().getAsInt();
        }
        return total;
    }

    private Material materialFromStatKey(String statKey) {
        if (!statKey.startsWith("minecraft:")) {
            return null;
        }
        String normalized = statKey.substring("minecraft:".length()).toUpperCase();
        return Material.matchMaterial(normalized);
    }
}
