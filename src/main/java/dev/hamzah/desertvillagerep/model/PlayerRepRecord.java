package dev.hamzah.desertvillagerep.model;

import java.util.UUID;

public record PlayerRepRecord(
        UUID uuid,
        String lastName,
        int builderPoints,
        int legacyBuilderSeed,
        int traderLivePoints,
        int legacyTraderSeed,
        int protectorPoints,
        int legacyProtectorSeed
) {
    public int scoreFor(RepCategory category) {
        return switch (category) {
            case BUILDER -> builderPoints + legacyBuilderSeed;
            case TRADER -> traderLivePoints + legacyTraderSeed;
            case PROTECTOR -> protectorPoints + legacyProtectorSeed;
        };
    }
}
