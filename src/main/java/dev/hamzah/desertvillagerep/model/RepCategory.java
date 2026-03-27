package dev.hamzah.desertvillagerep.model;

import java.util.Locale;

public enum RepCategory {
    BUILDER("Builder", "Master Builder"),
    TRADER("Trader", "Dune Trader"),
    PROTECTOR("Protector", "Desert Warden");

    private final String displayName;
    private final String winnerTitle;

    RepCategory(String displayName, String winnerTitle) {
        this.displayName = displayName;
        this.winnerTitle = winnerTitle;
    }

    public String displayName() {
        return displayName;
    }

    public String winnerTitle() {
        return winnerTitle;
    }

    public static RepCategory fromInput(String input) {
        String normalized = input.toUpperCase(Locale.ROOT);
        for (RepCategory value : values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return null;
    }
}

