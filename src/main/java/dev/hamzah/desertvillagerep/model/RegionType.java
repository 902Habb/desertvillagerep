package dev.hamzah.desertvillagerep.model;

import java.util.Locale;

public enum RegionType {
    VILLAGE,
    MARKET,
    PROTECTOR;

    public static RegionType fromInput(String input) {
        String normalized = input.toUpperCase(Locale.ROOT);
        for (RegionType value : values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return null;
    }
}

