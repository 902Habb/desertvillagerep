package dev.hamzah.desertvillagerep.command;

import dev.hamzah.desertvillagerep.model.CuboidRegion;
import dev.hamzah.desertvillagerep.model.LegacyStatsRecord;
import dev.hamzah.desertvillagerep.model.LeaderboardEntry;
import dev.hamzah.desertvillagerep.model.PlayerRepRecord;
import dev.hamzah.desertvillagerep.model.ProjectDefinition;
import dev.hamzah.desertvillagerep.model.RepCategory;
import dev.hamzah.desertvillagerep.model.RegionType;
import dev.hamzah.desertvillagerep.service.BoardService;
import dev.hamzah.desertvillagerep.service.LegacyImportService;
import dev.hamzah.desertvillagerep.service.ProjectService;
import dev.hamzah.desertvillagerep.service.RegionService;
import dev.hamzah.desertvillagerep.service.RepService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class RepCommand implements CommandExecutor, TabCompleter {
    private final RegionService regionService;
    private final RepService repService;
    private final ProjectService projectService;
    private final BoardService boardService;
    private final LegacyImportService legacyImportService;

    public RepCommand(
            RegionService regionService,
            RepService repService,
            ProjectService projectService,
            BoardService boardService,
            LegacyImportService legacyImportService
    ) {
        this.regionService = regionService;
        this.repService = repService;
        this.projectService = projectService;
        this.boardService = boardService;
        this.legacyImportService = legacyImportService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                sendPlayerStats(sender, player);
            } else {
                sendUsage(sender);
            }
            return true;
        }

        String root = args[0].toLowerCase(Locale.ROOT);
        return switch (root) {
            case "top" -> handleTop(sender, args);
            case "stats" -> handleStats(sender, args);
            case "legacy" -> handleLegacy(sender, args);
            case "admin" -> handleAdmin(sender, args);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleTop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rep top <builder|trader|protector>");
            return true;
        }
        RepCategory category = RepCategory.fromInput(args[1]);
        if (category == null) {
            sender.sendMessage(ChatColor.RED + "Unknown category: " + args[1]);
            return true;
        }
        List<LeaderboardEntry> entries = repService.getTop(category, 10);
        sender.sendMessage(ChatColor.GOLD + category.displayName() + " leaderboard:");
        if (entries.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No scores yet.");
            return true;
        }
        for (int index = 0; index < entries.size(); index++) {
            LeaderboardEntry entry = entries.get(index);
            sender.sendMessage(ChatColor.YELLOW + "#" + (index + 1) + " "
                    + ChatColor.WHITE + entry.name()
                    + ChatColor.DARK_GRAY + " - "
                    + ChatColor.GOLD + entry.score());
        }
        return true;
    }

    private boolean handleStats(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) {
            target = resolvePlayer(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /rep stats <player>");
            return true;
        }
        sendPlayerStats(sender, target);
        return true;
    }

    private boolean handleLegacy(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) {
            target = resolvePlayer(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /rep legacy <player>");
            return true;
        }

        LegacyStatsRecord legacy = repService.getLegacyStats(target);
        if (legacy == null) {
            sender.sendMessage(ChatColor.GRAY + "No legacy stats imported for " + nameFor(target) + ".");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "Legacy stats for " + legacy.lastName() + ":");
        sender.sendMessage(ChatColor.YELLOW + "Villager trades: " + ChatColor.WHITE + legacy.villagerTrades());
        sender.sendMessage(ChatColor.YELLOW + "Hostile kills: " + ChatColor.WHITE + legacy.hostileKills());
        sender.sendMessage(ChatColor.YELLOW + "Deaths: " + ChatColor.WHITE + legacy.deaths());
        sender.sendMessage(ChatColor.YELLOW + "Play time ticks: " + ChatColor.WHITE + legacy.playTimeTicks());
        sender.sendMessage(ChatColor.YELLOW + "Walked centimeters: " + ChatColor.WHITE + legacy.walkedCentimeters());
        sender.sendMessage(ChatColor.DARK_GRAY + "Imported: " + legacy.importedAt());
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("desertrep.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use admin commands.");
            return true;
        }
        if (args.length < 2) {
            sendAdminUsage(sender);
            return true;
        }

        String subcommand = args[1].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "pos1" -> handlePos(sender, true);
            case "pos2" -> handlePos(sender, false);
            case "setregion" -> handleSetRegion(sender, args);
            case "createboard", "moveboard" -> handleBoardAnchor(sender);
            case "removeboard" -> handleRemoveBoard(sender);
            case "addproject" -> handleAddProject(sender, args);
            case "completeproject" -> handleCompleteProject(sender, args);
            case "adjust" -> handleAdjust(sender, args);
            case "import" -> handleImport(sender, args);
            case "refresh" -> {
                boardService.refreshNow();
                sender.sendMessage(ChatColor.GREEN + "Board refreshed.");
                yield true;
            }
            default -> {
                sendAdminUsage(sender);
                yield true;
            }
        };
    }

    private boolean handlePos(CommandSender sender, boolean first) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        var location = first ? regionService.setPos1(player) : regionService.setPos2(player);
        sender.sendMessage(ChatColor.GREEN + (first ? "pos1" : "pos2") + " set to "
                + location.getBlockX() + ", "
                + location.getBlockY() + ", "
                + location.getBlockZ() + " in " + location.getWorld().getName() + ".");
        return true;
    }

    private boolean handleSetRegion(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rep admin setregion <village|market|protector>");
            return true;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        RegionType type = RegionType.fromInput(args[2]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown region type: " + args[2]);
            return true;
        }
        try {
            CuboidRegion region = regionService.saveRegion(player, type);
            if (region == null) {
                sender.sendMessage(ChatColor.RED + "Set both pos1 and pos2 first.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + type.name() + " region saved in " + region.worldName()
                    + " from (" + region.minX() + ", " + region.minY() + ", " + region.minZ() + ")"
                    + " to (" + region.maxX() + ", " + region.maxY() + ", " + region.maxZ() + ").");
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(ChatColor.RED + exception.getMessage());
        }
        return true;
    }

    private boolean handleBoardAnchor(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        boardService.setBoardAnchor(player.getLocation());
        sender.sendMessage(ChatColor.GREEN + "Board anchor set at your current location and facing.");
        return true;
    }

    private boolean handleRemoveBoard(CommandSender sender) {
        boardService.removeBoard();
        sender.sendMessage(ChatColor.GREEN + "Board removed. Use /rep admin createboard to place it again.");
        return true;
    }

    private boolean handleAddProject(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "Usage: /rep admin addproject builder <id> <points>");
            return true;
        }
        RepCategory category = RepCategory.fromInput(args[2]);
        if (category == null || category != RepCategory.BUILDER) {
            sender.sendMessage(ChatColor.RED + "Only builder projects are available in v1.");
            return true;
        }
        int points;
        try {
            points = Integer.parseInt(args[4]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(ChatColor.RED + "Points must be a whole number.");
            return true;
        }
        String id = args[3].toLowerCase(Locale.ROOT);
        projectService.saveProject(new ProjectDefinition(id, category, Math.max(0, points)));
        sender.sendMessage(ChatColor.GREEN + "Saved builder project '" + id + "' for " + points + " points.");
        return true;
    }

    private boolean handleCompleteProject(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /rep admin completeproject <id> <player>");
            return true;
        }
        String id = args[2].toLowerCase(Locale.ROOT);
        OfflinePlayer target = resolvePlayer(args[3]);
        UUID actorUuid = sender instanceof Player player ? player.getUniqueId() : null;
        ProjectService.CompletionResult result = projectService.completeProject(id, target.getUniqueId(), nameFor(target), actorUuid);
        switch (result) {
            case PROJECT_NOT_FOUND -> sender.sendMessage(ChatColor.RED + "Project '" + id + "' does not exist.");
            case ALREADY_COMPLETED -> sender.sendMessage(ChatColor.RED + nameFor(target) + " already completed project '" + id + "'.");
            case COMPLETED -> {
                boardService.scheduleRefresh();
                sender.sendMessage(ChatColor.GREEN + "Awarded project '" + id + "' to " + nameFor(target) + ".");
            }
        }
        return true;
    }

    private boolean handleAdjust(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "Usage: /rep admin adjust <player> <category> <amount> [reason]");
            return true;
        }
        OfflinePlayer target = resolvePlayer(args[2]);
        RepCategory category = RepCategory.fromInput(args[3]);
        if (category == null) {
            sender.sendMessage(ChatColor.RED + "Unknown category: " + args[3]);
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(ChatColor.RED + "Amount must be a whole number.");
            return true;
        }
        String reason = args.length >= 6 ? String.join(" ", Arrays.copyOfRange(args, 5, args.length)) : "Manual adjustment";
        UUID actorUuid = sender instanceof Player player ? player.getUniqueId() : null;
        repService.changeRep(target.getUniqueId(), nameFor(target), category, amount, actorUuid, reason);
        boardService.scheduleRefresh();
        sender.sendMessage(ChatColor.GREEN + "Adjusted " + category.displayName() + " rep for " + nameFor(target) + " by " + amount + ".");
        return true;
    }

    private boolean handleImport(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rep admin import legacy <all|player>");
            return true;
        }
        if (!args[2].equalsIgnoreCase("legacy")) {
            sender.sendMessage(ChatColor.RED + "Only 'legacy' import is supported.");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /rep admin import legacy <all|player>");
            return true;
        }
        LegacyImportService.ImportSummary summary;
        if (args[3].equalsIgnoreCase("all")) {
            summary = legacyImportService.importAll();
        } else {
            summary = legacyImportService.importPlayer(resolvePlayer(args[3]));
        }
        boardService.scheduleRefresh();
        sender.sendMessage(ChatColor.GREEN + "Legacy import finished. Imported " + summary.imported()
                + " of " + summary.scanned() + " file(s) from " + summary.sourcePath() + ".");
        return true;
    }

    private void sendPlayerStats(CommandSender sender, OfflinePlayer target) {
        PlayerRepRecord profile = repService.getProfile(target);
        sender.sendMessage(ChatColor.GOLD + "Reputation for " + nameFor(target) + ":");
        sender.sendMessage(ChatColor.YELLOW + "Builder: " + ChatColor.WHITE + profile.builderPoints());
        sender.sendMessage(ChatColor.AQUA + "Trader: " + ChatColor.WHITE + profile.scoreFor(RepCategory.TRADER)
                + ChatColor.DARK_GRAY + " (" + profile.traderLivePoints() + " live, " + profile.legacyTraderSeed() + " legacy)");
        sender.sendMessage(ChatColor.RED + "Protector: " + ChatColor.WHITE + profile.protectorPoints());
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "/rep");
        sender.sendMessage(ChatColor.YELLOW + "/rep top <builder|trader|protector>");
        sender.sendMessage(ChatColor.YELLOW + "/rep stats [player]");
        sender.sendMessage(ChatColor.YELLOW + "/rep legacy [player]");
        if (sender.hasPermission("desertrep.admin")) {
            sendAdminUsage(sender);
        }
    }

    private void sendAdminUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Admin commands:");
        sender.sendMessage(ChatColor.YELLOW + "/rep admin pos1");
        sender.sendMessage(ChatColor.YELLOW + "/rep admin pos2");
        sender.sendMessage(ChatColor.YELLOW + "/rep admin setregion <village|market|protector>");
        sender.sendMessage(ChatColor.YELLOW + "/rep admin createboard");
        sender.sendMessage(ChatColor.YELLOW + "/rep admin moveboard");
        sender.sendMessage(ChatColor.YELLOW + "/rep admin removeboard");
        sender.sendMessage(ChatColor.YELLOW + "/rep admin addproject builder <id> <points>");
        sender.sendMessage(ChatColor.YELLOW + "/rep admin completeproject <id> <player>");
        sender.sendMessage(ChatColor.YELLOW + "/rep admin adjust <player> <category> <amount> [reason]");
        sender.sendMessage(ChatColor.YELLOW + "/rep admin import legacy <all|player>");
        sender.sendMessage(ChatColor.YELLOW + "/rep admin refresh");
    }

    private OfflinePlayer resolvePlayer(String input) {
        try {
            UUID uuid = UUID.fromString(input);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            return Bukkit.getOfflinePlayer(input);
        }
    }

    private String nameFor(OfflinePlayer player) {
        return player.getName() != null ? player.getName() : player.getUniqueId().toString().substring(0, 8);
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "This command can only be run in-game.");
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(args[0], List.of("top", "stats", "legacy", "admin"));
        }
        if (args[0].equalsIgnoreCase("top") && args.length == 2) {
            return partial(args[1], List.of("builder", "trader", "protector"));
        }
        if ((args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("legacy")) && args.length == 2) {
            return partial(args[1], playerNames());
        }
        if (!args[0].equalsIgnoreCase("admin")) {
            return List.of();
        }
        if (args.length == 2) {
            return partial(args[1], List.of("pos1", "pos2", "setregion", "createboard", "moveboard", "removeboard", "addproject", "completeproject", "adjust", "import", "refresh"));
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("setregion")) {
            return partial(args[2], List.of("village", "market", "protector"));
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("addproject")) {
            return partial(args[2], List.of("builder"));
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("completeproject")) {
            return partial(args[2], new ArrayList<>(projectService.projectIds()));
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("completeproject")) {
            return partial(args[3], playerNames());
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("adjust")) {
            return partial(args[2], playerNames());
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("adjust")) {
            return partial(args[3], List.of("builder", "trader", "protector"));
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("import")) {
            return partial(args[2], List.of("legacy"));
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("import") && args[2].equalsIgnoreCase("legacy")) {
            List<String> values = new ArrayList<>();
            values.add("all");
            values.addAll(playerNames());
            return partial(args[3], values);
        }
        return List.of();
    }

    private List<String> partial(String input, Collection<String> values) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> playerNames() {
        List<String> names = new ArrayList<>();
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player.getName() != null) {
                names.add(player.getName());
            }
        }
        return names;
    }
}
