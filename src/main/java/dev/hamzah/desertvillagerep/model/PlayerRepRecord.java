package dev.hamzah.desertvillagerep.model;

import java.util.UUID;

public record PlayerRepRecord(
        UUID uuid,
        String lastName,
        int builderPoints,
        int traderLivePoints,
        int legacyTraderSeed,
        int protectorPoints
) {
    public int scoreFor(RepCategory category) {
        return switch (category) {
            case BUILDER -> builderPoints;
            case TRADER -> traderLivePoints + legacyTraderSeed;
            case PROTECTOR -> protectorPoints;
        };
    }
}

