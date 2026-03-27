package dev.hamzah.desertvillagerep;

import dev.hamzah.desertvillagerep.command.RepCommand;
import dev.hamzah.desertvillagerep.config.PluginConfig;
import dev.hamzah.desertvillagerep.listener.BuilderListener;
import dev.hamzah.desertvillagerep.listener.ProtectorListener;
import dev.hamzah.desertvillagerep.listener.TraderListener;
import dev.hamzah.desertvillagerep.service.BoardService;
import dev.hamzah.desertvillagerep.service.Database;
import dev.hamzah.desertvillagerep.service.LegacyImportService;
import dev.hamzah.desertvillagerep.service.ProjectService;
import dev.hamzah.desertvillagerep.service.RegionService;
import dev.hamzah.desertvillagerep.service.RepService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DesertVillageRepPlugin extends JavaPlugin {
    private PluginConfig pluginConfig;
    private Database database;
    private RegionService regionService;
    private RepService repService;
    private ProjectService projectService;
    private BoardService boardService;
    private LegacyImportService legacyImportService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(this);
        this.database = new Database(this);
        this.database.initialize();
        this.regionService = new RegionService(database);
        this.repService = new RepService(database);
        this.projectService = new ProjectService(database, repService);
        this.boardService = new BoardService(this, pluginConfig, database, repService);
        this.legacyImportService = new LegacyImportService(this, database, repService);

        PluginCommand repCommand = getCommand("rep");
        if (repCommand == null) {
            throw new IllegalStateException("Command /rep is not defined in plugin.yml");
        }

        RepCommand executor = new RepCommand(regionService, repService, projectService, boardService, legacyImportService);
        repCommand.setExecutor(executor);
        repCommand.setTabCompleter(executor);

        getServer().getPluginManager().registerEvents(new BuilderListener(pluginConfig, regionService, repService, boardService), this);
        getServer().getPluginManager().registerEvents(new TraderListener(regionService, repService, boardService), this);
        getServer().getPluginManager().registerEvents(new ProtectorListener(regionService, repService, boardService), this);

        boardService.start();
        getLogger().info("DesertVillageRep enabled.");
    }

    @Override
    public void onDisable() {
        if (boardService != null) {
            boardService.shutdown();
        }
        if (database != null) {
            database.close();
        }
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }
}
