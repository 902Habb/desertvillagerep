package dev.hamzah.desertvillagerep.model;

import java.util.UUID;

public record LegacyStatsRecord(
        UUID uuid,
        String lastName,
        int villagerTrades,
        long playTimeTicks,
        int deaths,
        int hostileKills,
        long walkedCentimeters,
        String importedAt
) {
}

