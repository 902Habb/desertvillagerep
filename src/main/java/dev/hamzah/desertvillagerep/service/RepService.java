package dev.hamzah.desertvillagerep.service;

import dev.hamzah.desertvillagerep.model.LeaderboardEntry;
import dev.hamzah.desertvillagerep.model.LegacyStatsRecord;
import dev.hamzah.desertvillagerep.model.PlayerRepRecord;
import dev.hamzah.desertvillagerep.model.RepCategory;
import java.util.List;
import java.util.UUID;
import org.bukkit.OfflinePlayer;

public final class RepService {
    private final Database database;

    public RepService(Database database) {
        this.database = database;
    }

    public PlayerRepRecord getProfile(OfflinePlayer player) {
        return getProfile(player.getUniqueId(), player.getName());
    }

    public PlayerRepRecord getProfile(UUID uuid, String lastKnownName) {
        return database.getPlayerRep(uuid, lastKnownName);
    }

    public LegacyStatsRecord getLegacyStats(OfflinePlayer player) {
        return database.getLegacySnapshot(player.getUniqueId(), player.getName());
    }

    public List<LeaderboardEntry> getTop(RepCategory category, int limit) {
        return database.getTop(category, limit);
    }

    public void changeRep(UUID uuid, String lastKnownName, RepCategory category, int amount, UUID actorUuid, String reason) {
        database.changeRep(uuid, lastKnownName, category, amount);
        if (actorUuid != null || (reason != null && !reason.isBlank())) {
            database.insertHistory(uuid, actorUuid, category, amount, reason);
        }
    }

    public void saveLegacyStats(LegacyStatsRecord record) {
        database.saveLegacySnapshot(record);
    }

    public void setLegacyTraderSeed(UUID uuid, String lastKnownName, int seedValue) {
        database.setLegacyTraderSeed(uuid, lastKnownName, seedValue);
    }

    public void setLegacyBuilderSeed(UUID uuid, String lastKnownName, int seedValue) {
        database.setLegacyBuilderSeed(uuid, lastKnownName, seedValue);
    }
}
