package dev.hamzah.desertvillagerep.service;

import dev.hamzah.desertvillagerep.model.BoardAnchor;
import dev.hamzah.desertvillagerep.model.CuboidRegion;
import dev.hamzah.desertvillagerep.model.LeaderboardEntry;
import dev.hamzah.desertvillagerep.model.LegacyStatsRecord;
import dev.hamzah.desertvillagerep.model.PlayerRepRecord;
import dev.hamzah.desertvillagerep.model.ProjectDefinition;
import dev.hamzah.desertvillagerep.model.RepCategory;
import dev.hamzah.desertvillagerep.model.RegionType;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.plugin.java.JavaPlugin;

public final class Database {
    private final JavaPlugin plugin;
    private Connection connection;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                throw new IllegalStateException("Failed to create plugin data folder");
            }
            File databaseFile = new File(dataFolder, "desertvillagerep.db");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode = WAL");
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS player_rep (
                            uuid TEXT PRIMARY KEY,
                            last_name TEXT NOT NULL,
                            builder_points INTEGER NOT NULL DEFAULT 0,
                            trader_live_points INTEGER NOT NULL DEFAULT 0,
                            legacy_trader_seed INTEGER NOT NULL DEFAULT 0,
                            protector_points INTEGER NOT NULL DEFAULT 0,
                            updated_at TEXT NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS legacy_snapshot (
                            uuid TEXT PRIMARY KEY,
                            last_name TEXT NOT NULL,
                            villager_trades INTEGER NOT NULL DEFAULT 0,
                            play_time_ticks INTEGER NOT NULL DEFAULT 0,
                            deaths INTEGER NOT NULL DEFAULT 0,
                            hostile_kills INTEGER NOT NULL DEFAULT 0,
                            walked_centimeters INTEGER NOT NULL DEFAULT 0,
                            imported_at TEXT NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS regions (
                            type TEXT PRIMARY KEY,
                            world_name TEXT NOT NULL,
                            min_x INTEGER NOT NULL,
                            min_y INTEGER NOT NULL,
                            min_z INTEGER NOT NULL,
                            max_x INTEGER NOT NULL,
                            max_y INTEGER NOT NULL,
                            max_z INTEGER NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS board_anchor (
                            id INTEGER PRIMARY KEY CHECK (id = 1),
                            world_name TEXT NOT NULL,
                            x REAL NOT NULL,
                            y REAL NOT NULL,
                            z REAL NOT NULL,
                            yaw REAL NOT NULL,
                            pitch REAL NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS projects (
                            id TEXT PRIMARY KEY,
                            category TEXT NOT NULL,
                            points INTEGER NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS project_completions (
                            project_id TEXT NOT NULL,
                            uuid TEXT NOT NULL,
                            completed_at TEXT NOT NULL,
                            PRIMARY KEY (project_id, uuid)
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS rep_history (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            uuid TEXT NOT NULL,
                            actor_uuid TEXT,
                            category TEXT NOT NULL,
                            amount INTEGER NOT NULL,
                            reason TEXT,
                            created_at TEXT NOT NULL
                        )
                        """);
            }
        } catch (ClassNotFoundException | SQLException exception) {
            throw new IllegalStateException("Failed to initialize SQLite database", exception);
        }
    }

    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Failed to close database cleanly: " + exception.getMessage());
        }
    }

    public synchronized void upsertPlayer(UUID uuid, String lastKnownName) {
        String resolvedName = resolveName(uuid, lastKnownName);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO player_rep (uuid, last_name, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    last_name = excluded.last_name
                """)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, resolvedName);
            statement.setString(3, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to upsert player record", exception);
        }
    }

    public synchronized PlayerRepRecord getPlayerRep(UUID uuid, String fallbackName) {
        upsertPlayer(uuid, fallbackName);
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT uuid, last_name, builder_points, trader_live_points, legacy_trader_seed, protector_points
                FROM player_rep
                WHERE uuid = ?
                """)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new PlayerRepRecord(uuid, resolveName(uuid, fallbackName), 0, 0, 0, 0);
                }
                return new PlayerRepRecord(
                        UUID.fromString(resultSet.getString("uuid")),
                        resultSet.getString("last_name"),
                        resultSet.getInt("builder_points"),
                        resultSet.getInt("trader_live_points"),
                        resultSet.getInt("legacy_trader_seed"),
                        resultSet.getInt("protector_points")
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch player reputation", exception);
        }
    }

    public synchronized void changeRep(UUID uuid, String lastKnownName, RepCategory category, int amount) {
        upsertPlayer(uuid, lastKnownName);
        String column = switch (category) {
            case BUILDER -> "builder_points";
            case TRADER -> "trader_live_points";
            case PROTECTOR -> "protector_points";
        };

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE player_rep SET " + column + " = MAX(0, " + column + " + ?), updated_at = ? WHERE uuid = ?"
        )) {
            statement.setInt(1, amount);
            statement.setString(2, Instant.now().toString());
            statement.setString(3, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to change reputation for " + category, exception);
        }
    }

    public synchronized void setLegacyTraderSeed(UUID uuid, String lastKnownName, int seedValue) {
        upsertPlayer(uuid, lastKnownName);
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE player_rep
                SET legacy_trader_seed = ?, updated_at = ?
                WHERE uuid = ?
                """)) {
            statement.setInt(1, Math.max(0, seedValue));
            statement.setString(2, Instant.now().toString());
            statement.setString(3, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to set legacy trader seed", exception);
        }
    }

    public synchronized void saveLegacySnapshot(LegacyStatsRecord record) {
        upsertPlayer(record.uuid(), record.lastName());
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO legacy_snapshot (uuid, last_name, villager_trades, play_time_ticks, deaths, hostile_kills, walked_centimeters, imported_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    last_name = excluded.last_name,
                    villager_trades = excluded.villager_trades,
                    play_time_ticks = excluded.play_time_ticks,
                    deaths = excluded.deaths,
                    hostile_kills = excluded.hostile_kills,
                    walked_centimeters = excluded.walked_centimeters,
                    imported_at = excluded.imported_at
                """)) {
            statement.setString(1, record.uuid().toString());
            statement.setString(2, record.lastName());
            statement.setInt(3, record.villagerTrades());
            statement.setLong(4, record.playTimeTicks());
            statement.setInt(5, record.deaths());
            statement.setInt(6, record.hostileKills());
            statement.setLong(7, record.walkedCentimeters());
            statement.setString(8, record.importedAt());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save legacy snapshot", exception);
        }
    }

    public synchronized LegacyStatsRecord getLegacySnapshot(UUID uuid, String fallbackName) {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT uuid, last_name, villager_trades, play_time_ticks, deaths, hostile_kills, walked_centimeters, imported_at
                FROM legacy_snapshot
                WHERE uuid = ?
                """)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new LegacyStatsRecord(
                        UUID.fromString(resultSet.getString("uuid")),
                        resultSet.getString("last_name"),
                        resultSet.getInt("villager_trades"),
                        resultSet.getLong("play_time_ticks"),
                        resultSet.getInt("deaths"),
                        resultSet.getInt("hostile_kills"),
                        resultSet.getLong("walked_centimeters"),
                        resultSet.getString("imported_at")
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch legacy snapshot", exception);
        }
    }

    public synchronized List<LeaderboardEntry> getTop(RepCategory category, int limit) {
        String query = switch (category) {
            case BUILDER -> """
                    SELECT uuid, last_name, builder_points AS score
                    FROM player_rep
                    ORDER BY builder_points DESC, last_name ASC
                    LIMIT ?
                    """;
            case TRADER -> """
                    SELECT uuid, last_name, (trader_live_points + legacy_trader_seed) AS score
                    FROM player_rep
                    ORDER BY (trader_live_points + legacy_trader_seed) DESC, last_name ASC
                    LIMIT ?
                    """;
            case PROTECTOR -> """
                    SELECT uuid, last_name, protector_points AS score
                    FROM player_rep
                    ORDER BY protector_points DESC, last_name ASC
                    LIMIT ?
                    """;
        };

        List<LeaderboardEntry> entries = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new LeaderboardEntry(
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("last_name"),
                            resultSet.getInt("score")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch leaderboard for " + category, exception);
        }
        return entries;
    }

    public synchronized void saveRegion(RegionType type, CuboidRegion region) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO regions (type, world_name, min_x, min_y, min_z, max_x, max_y, max_z)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(type) DO UPDATE SET
                    world_name = excluded.world_name,
                    min_x = excluded.min_x,
                    min_y = excluded.min_y,
                    min_z = excluded.min_z,
                    max_x = excluded.max_x,
                    max_y = excluded.max_y,
                    max_z = excluded.max_z
                """)) {
            statement.setString(1, type.name());
            statement.setString(2, region.worldName());
            statement.setInt(3, region.minX());
            statement.setInt(4, region.minY());
            statement.setInt(5, region.minZ());
            statement.setInt(6, region.maxX());
            statement.setInt(7, region.maxY());
            statement.setInt(8, region.maxZ());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save region " + type, exception);
        }
    }

    public synchronized CuboidRegion getRegion(RegionType type) {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT world_name, min_x, min_y, min_z, max_x, max_y, max_z
                FROM regions
                WHERE type = ?
                """)) {
            statement.setString(1, type.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new CuboidRegion(
                        resultSet.getString("world_name"),
                        resultSet.getInt("min_x"),
                        resultSet.getInt("min_y"),
                        resultSet.getInt("min_z"),
                        resultSet.getInt("max_x"),
                        resultSet.getInt("max_y"),
                        resultSet.getInt("max_z")
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load region " + type, exception);
        }
    }

    public synchronized void saveBoardAnchor(BoardAnchor anchor) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO board_anchor (id, world_name, x, y, z, yaw, pitch)
                VALUES (1, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    world_name = excluded.world_name,
                    x = excluded.x,
                    y = excluded.y,
                    z = excluded.z,
                    yaw = excluded.yaw,
                    pitch = excluded.pitch
                """)) {
            statement.setString(1, anchor.worldName());
            statement.setDouble(2, anchor.x());
            statement.setDouble(3, anchor.y());
            statement.setDouble(4, anchor.z());
            statement.setFloat(5, anchor.yaw());
            statement.setFloat(6, anchor.pitch());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save board anchor", exception);
        }
    }

    public synchronized BoardAnchor getBoardAnchor() {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT world_name, x, y, z, yaw, pitch
                FROM board_anchor
                WHERE id = 1
                """);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return null;
            }
            return new BoardAnchor(
                    resultSet.getString("world_name"),
                    resultSet.getDouble("x"),
                    resultSet.getDouble("y"),
                    resultSet.getDouble("z"),
                    resultSet.getFloat("yaw"),
                    resultSet.getFloat("pitch")
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load board anchor", exception);
        }
    }

    public synchronized void deleteBoardAnchor() {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM board_anchor WHERE id = 1")) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete board anchor", exception);
        }
    }

    public synchronized void saveProject(ProjectDefinition definition) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO projects (id, category, points)
                VALUES (?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    category = excluded.category,
                    points = excluded.points
                """)) {
            statement.setString(1, definition.id());
            statement.setString(2, definition.category().name());
            statement.setInt(3, definition.points());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save project " + definition.id(), exception);
        }
    }

    public synchronized ProjectDefinition getProject(String id) {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, category, points
                FROM projects
                WHERE id = ?
                """)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new ProjectDefinition(
                        resultSet.getString("id"),
                        RepCategory.valueOf(resultSet.getString("category")),
                        resultSet.getInt("points")
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load project " + id, exception);
        }
    }

    public synchronized Collection<String> getProjectIds() {
        List<String> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM projects ORDER BY id ASC");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getString("id"));
            }
            return ids;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load project ids", exception);
        }
    }

    public synchronized boolean markProjectCompleted(String projectId, UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO project_completions (project_id, uuid, completed_at)
                VALUES (?, ?, ?)
                """)) {
            statement.setString(1, projectId);
            statement.setString(2, uuid.toString());
            statement.setString(3, Instant.now().toString());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to mark project completion", exception);
        }
    }

    public synchronized void insertHistory(UUID targetUuid, UUID actorUuid, RepCategory category, int amount, String reason) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO rep_history (uuid, actor_uuid, category, amount, reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, targetUuid.toString());
            statement.setString(2, actorUuid == null ? null : actorUuid.toString());
            statement.setString(3, category.name());
            statement.setInt(4, amount);
            statement.setString(5, reason);
            statement.setString(6, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to insert reputation history", exception);
        }
    }

    private String resolveName(UUID uuid, String lastKnownName) {
        if (lastKnownName == null || lastKnownName.isBlank()) {
            return uuid.toString().substring(0, 8);
        }
        return lastKnownName;
    }
}
