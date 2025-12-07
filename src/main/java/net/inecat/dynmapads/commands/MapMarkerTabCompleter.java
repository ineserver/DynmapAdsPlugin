package net.inecat.dynmapads.commands;

import net.inecat.dynmapads.DynmapAdsPlugin;
import net.inecat.dynmapads.data.MarkerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tab completer for /mapmarker command.
 */
public class MapMarkerTabCompleter implements TabCompleter {
    private static final String ADMIN_PERMISSION = "mapmarker.admin";
    private static final List<String> SUBCOMMANDS = Arrays.asList("commercial", "ads", "delete");

    private final DynmapAdsPlugin plugin;

    public MapMarkerTabCompleter(DynmapAdsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            // Subcommand completion
            return SUBCOMMANDS.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "commercial" -> {
                if (args.length == 2) {
                    // Shop name - suggest a placeholder
                    return Collections.singletonList("<店名>");
                } else if (args.length == 3) {
                    return Collections.singletonList("<説明>");
                }
            }
            case "ads" -> {
                if (args.length == 2) {
                    // Shop name - show owned shops (or all for admins)
                    return getManageableShopNames(player, args[1]);
                } else if (args.length == 3) {
                    // Duration
                    return Arrays.asList("1", "3", "7", "14", "30");
                } else if (args.length == 4) {
                    return Collections.singletonList("[宣伝文句]");
                }
            }
            case "delete" -> {
                if (args.length == 2) {
                    // Shop name - show owned shops (or all for admins)
                    return getManageableShopNames(player, args[1]);
                }
            }
        }

        return Collections.emptyList();
    }

    private List<String> getManageableShopNames(Player player, String prefix) {
        Set<String> shopNames;

        if (player.hasPermission(ADMIN_PERMISSION)) {
            // Admins can see all approved shops
            shopNames = plugin.getMarkerStorage().getApprovedShopNames();
        } else {
            // Regular players only see their own shops
            shopNames = plugin.getMarkerStorage().getByOwner(player.getUniqueId()).stream()
                    .map(MarkerData::getShopName)
                    .collect(Collectors.toSet());
        }

        return shopNames.stream()
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }
}
