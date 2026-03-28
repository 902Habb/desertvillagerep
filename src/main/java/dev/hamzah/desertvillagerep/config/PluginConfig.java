package dev.hamzah.desertvillagerep.config;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginConfig {
    private final String boardTitle;
    private final long boardRefreshTicks;
    private final float boardVisibilityRange;

    public PluginConfig(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        this.boardTitle = ChatColor.translateAlternateColorCodes('&', config.getString("board.title", "&6&lDESERT VILLAGE REPUTATION"));
        this.boardRefreshTicks = Math.max(20L, config.getLong("board.refresh-seconds", 10L) * 20L);
        this.boardVisibilityRange = (float) Math.max(1.0d, config.getDouble("board.visibility-radius", 5.0d));
    }

    public String boardTitle() {
        return boardTitle;
    }

    public long boardRefreshTicks() {
        return boardRefreshTicks;
    }

    public float boardVisibilityRange() {
        return boardVisibilityRange;
    }

    public int builderPointsFor(Material material) {
        return material.isBlock() ? 1 : 0;
    }
}
