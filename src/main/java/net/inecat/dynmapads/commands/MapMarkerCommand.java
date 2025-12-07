package net.inecat.dynmapads.commands;

import net.inecat.dynmapads.DynmapAdsPlugin;
import net.inecat.dynmapads.config.ConfigManager;
import net.inecat.dynmapads.data.MarkerData;
import net.inecat.dynmapads.data.MarkerStatus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Main command executor for /mapmarker command.
 */
public class MapMarkerCommand implements CommandExecutor {
    private static final String ADMIN_PERMISSION = "mapmarker.admin";

    private final DynmapAdsPlugin plugin;

    public MapMarkerCommand(DynmapAdsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getPlayerOnly());
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "commercial" -> handleCommercial(player, args);
            case "ads" -> handleAds(player, args);
            case "delete" -> handleDelete(player, args);
            default -> sendUsage(player);
        }

        return true;
    }

    private void handleCommercial(Player player, String[] args) {
        ConfigManager config = plugin.getConfigManager();

        // /mapmarker commercial <店名> <説明>
        if (args.length < 3) {
            player.sendMessage(config.getPrefix() + config.getUsageCommercial());
            return;
        }

        String shopName = args[1];
        String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        // Check if shop name already exists
        if (plugin.getMarkerStorage().exists(shopName)) {
            player.sendMessage(config.formatMessage(config.getShopExists(), "%shop%", shopName));
            return;
        }

        // Check balance
        int fee = config.getCommercialFee();
        if (!plugin.getEconomyManager().hasBalance(player, fee)) {
            player.sendMessage(config.formatMessage(config.getInsufficientFunds(),
                    "%amount%", String.valueOf(fee),
                    "%currency%", config.getCurrencyName()));
            return;
        }

        // Withdraw fee
        if (!plugin.getEconomyManager().withdraw(player, fee)) {
            player.sendMessage(config.formatMessage(config.getInsufficientFunds(),
                    "%amount%", String.valueOf(fee),
                    "%currency%", config.getCurrencyName()));
            return;
        }

        // Create marker data
        MarkerData data = new MarkerData(
                shopName,
                player.getUniqueId(),
                player.getWorld().getName(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                description);

        // Notify player about the payment
        final int chargedFee = fee;
        player.sendMessage(config.getPrefix() + "§e" + chargedFee + " " + config.getCurrencyName() + "§aを支払いました。");

        // Send Discord approval request
        plugin.getDiscordManager().sendApprovalRequest(data, player.getName())
                .thenAccept(messageId -> {
                    data.setDiscordMessageId(messageId);
                    plugin.getMarkerStorage().addMarker(data);

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(config.formatMessage(config.getCommercialPending(), "%shop%", shopName));
                    });
                })
                .exceptionally(error -> {
                    // Refund on error
                    plugin.getEconomyManager().deposit(player, chargedFee);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(config.getPrefix() + "§cDiscord通知の送信に失敗しました。§e" + chargedFee + " "
                                + config.getCurrencyName() + "§cを返金しました。");
                    });
                    return null;
                });
    }

    private void handleAds(Player player, String[] args) {
        ConfigManager config = plugin.getConfigManager();

        // /mapmarker ads <店名> <期間(日)> [宣伝文句]
        if (args.length < 3) {
            player.sendMessage(config.getPrefix() + config.getUsageAds());
            return;
        }

        String shopName = args[1];
        int days;

        try {
            days = Integer.parseInt(args[2]);
            if (days <= 0) {
                player.sendMessage(config.getPrefix() + "§c期間は1日以上を指定してください。");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(config.getPrefix() + config.getUsageAds());
            return;
        }

        String prMessage = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;

        // Check if shop exists
        MarkerData data = plugin.getMarkerStorage().getMarker(shopName);
        if (data == null) {
            player.sendMessage(config.formatMessage(config.getShopNotFound(), "%shop%", shopName));
            return;
        }

        // Check ownership
        if (!canManage(player, data)) {
            player.sendMessage(config.getPrefix() + config.getNotOwner());
            return;
        }

        // Check if already ads or pending
        if (data.getStatus() == MarkerStatus.PENDING) {
            player.sendMessage(config.getPrefix() + "§cこの店舗はまだ承認されていません。");
            return;
        }

        if (data.getStatus() == MarkerStatus.ADS) {
            player.sendMessage(config.getPrefix() + "§cこの店舗は既に広告中です。");
            return;
        }

        // Calculate fee
        int totalFee = config.getAdsFeePerDay() * days;
        if (!plugin.getEconomyManager().hasBalance(player, totalFee)) {
            player.sendMessage(config.formatMessage(config.getInsufficientFunds(),
                    "%amount%", String.valueOf(totalFee),
                    "%currency%", config.getCurrencyName()));
            return;
        }

        // Withdraw fee
        if (!plugin.getEconomyManager().withdraw(player, totalFee)) {
            player.sendMessage(config.formatMessage(config.getInsufficientFunds(),
                    "%amount%", String.valueOf(totalFee),
                    "%currency%", config.getCurrencyName()));
            return;
        }

        // Notify player about the payment
        player.sendMessage(
                config.getPrefix() + "§e" + totalFee + " " + config.getCurrencyName() + "§aを支払いました。（" + days + "日間）");

        // Update marker data
        data.setStatus(MarkerStatus.ADS);
        data.setAdsEndTime(LocalDateTime.now().plusDays(days));
        data.setPrMessage(prMessage);
        plugin.getMarkerStorage().updateMarker(data);

        // Move marker to ads set
        String ownerName = player.getName();
        plugin.getDynmapManager().moveToAds(data, ownerName);

        // Send Discord notification
        plugin.getDiscordManager().sendAdsNotification(data, ownerName, days);

        player.sendMessage(config.formatMessage(config.getAdsStarted(),
                "%shop%", shopName,
                "%days%", String.valueOf(days)));
    }

    private void handleDelete(Player player, String[] args) {
        ConfigManager config = plugin.getConfigManager();

        // /mapmarker delete <店名>
        if (args.length < 2) {
            player.sendMessage(config.getPrefix() + config.getUsageDelete());
            return;
        }

        String shopName = args[1];

        // Check if shop exists
        MarkerData data = plugin.getMarkerStorage().getMarker(shopName);
        if (data == null) {
            player.sendMessage(config.formatMessage(config.getShopNotFound(), "%shop%", shopName));
            return;
        }

        // Check ownership
        if (!canManage(player, data)) {
            player.sendMessage(config.getPrefix() + config.getNotOwner());
            return;
        }

        // Refund and delete Discord message if PENDING
        if (data.getStatus() == MarkerStatus.PENDING) {
            int refundAmount = config.getCommercialFee();
            plugin.getEconomyManager().deposit(player, refundAmount);
            player.sendMessage(config.getPrefix() + "§a承認待ち店舗のため、§e" + refundAmount + " " + config.getCurrencyName()
                    + "§aを返金しました。");

            // Delete Discord message
            String messageId = data.getDiscordMessageId();
            if (messageId != null && !messageId.isEmpty()) {
                plugin.getDiscordManager().deleteApprovalMessage(messageId, shopName, player.getName() + "が申請を取り消しました");
            }
        }

        // Delete from Dynmap (only if not PENDING)
        if (data.getStatus() != MarkerStatus.PENDING) {
            plugin.getDynmapManager().deleteMarker(data);
        }

        // Delete from storage
        plugin.getMarkerStorage().removeMarker(shopName);

        player.sendMessage(config.formatMessage(config.getShopDeleted(), "%shop%", shopName));
    }

    private boolean canManage(Player player, MarkerData data) {
        return data.isOwner(player.getUniqueId()) || player.hasPermission(ADMIN_PERMISSION);
    }

    private void sendUsage(Player player) {
        ConfigManager config = plugin.getConfigManager();
        player.sendMessage(config.getPrefix() + "§e使用法:");
        player.sendMessage("§7  /mapmarker commercial <店名> <説明>");
        player.sendMessage("§7  /mapmarker ads <店名> <期間(日)> [宣伝文句]");
        player.sendMessage("§7  /mapmarker delete <店名>");
    }
}
