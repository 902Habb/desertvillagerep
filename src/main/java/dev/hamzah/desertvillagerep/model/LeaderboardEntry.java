package dev.hamzah.desertvillagerep.model;

import java.util.UUID;

public record LeaderboardEntry(UUID uuid, String name, int score) {
}

