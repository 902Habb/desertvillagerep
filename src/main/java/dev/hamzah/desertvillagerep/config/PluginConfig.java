package dev.hamzah.desertvillagerep.config;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginConfig {
    private final String boardTitle;
    private final long boardRefreshTicks;
    private final float boardVisibilityRange;
    private final Set<Material> standardBuilderMaterials;
    private final Set<Material> premiumBuilderMaterials;

    public PluginConfig(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        this.boardTitle = ChatColor.translateAlternateColorCodes('&', config.getString("board.title", "&6&lDESERT VILLAGE REPUTATION"));
        this.boardRefreshTicks = Math.max(20L, config.getLong("board.refresh-seconds", 10L) * 20L);
        this.boardVisibilityRange = (float) Math.max(1.0d, config.getDouble("board.visibility-radius", 5.0d));
        this.standardBuilderMaterials = parseMaterials(config.getStringList("builder.standard-materials"), plugin);
        this.premiumBuilderMaterials = parseMaterials(config.getStringList("builder.premium-materials"), plugin);
    }

    private Set<Material> parseMaterials(Collection<String> configuredValues, JavaPlugin plugin) {
        EnumSet<Material> materials = EnumSet.noneOf(Material.class);
        for (String value : configuredValues) {
            Material material = Material.matchMaterial(value);
            if (material == null) {
                plugin.getLogger().warning("Unknown material in config: " + value);
                continue;
            }
            materials.add(material);
        }
        return Collections.unmodifiableSet(materials);
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
        if (premiumBuilderMaterials.contains(material)) {
            return 2;
        }
        if (isPremiumPattern(material)) {
            return 2;
        }
        if (standardBuilderMaterials.contains(material)) {
            return 1;
        }
        return 0;
    }

    private boolean isPremiumPattern(Material material) {
        String name = material.name();
        if (Tag.BANNERS.isTagged(material)) {
            return true;
        }
        if (Tag.FLOWERS.isTagged(material)) {
            return true;
        }
        if (Tag.FLOWER_POTS.isTagged(material)) {
            return true;
        }
        if (name.endsWith("_STAINED_GLASS") || name.endsWith("_STAINED_GLASS_PANE")) {
            return true;
        }
        return name.endsWith("_TERRACOTTA") && material != Material.TERRACOTTA;
    }
}
