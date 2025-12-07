package net.inecat.dynmapads.tasks;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReaction;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.inecat.dynmapads.DynmapAdsPlugin;
import net.inecat.dynmapads.data.MarkerData;
import net.inecat.dynmapads.data.MarkerStatus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Periodically polls Discord messages for approval/rejection reactions.
 * This is a workaround for JDA event listeners not working with DiscordSRV.
 */
public class ReactionPollingTask extends BukkitRunnable {
    private static final String APPROVE_EMOJI = "✅";
    private static final String REJECT_EMOJI = "❌";

    private final DynmapAdsPlugin plugin;

    public ReactionPollingTask(DynmapAdsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getDiscordManager().isReady()) {
            return;
        }

        String channelId = plugin.getConfigManager().getApprovalChannelId();
        TextChannel channel = DiscordUtil.getTextChannelById(channelId);

        if (channel == null) {
            return;
        }

        // Get all pending markers
        List<MarkerData> pendingMarkers = plugin.getMarkerStorage().getByStatus(MarkerStatus.PENDING);

        for (MarkerData data : pendingMarkers) {
            String messageId = data.getDiscordMessageId();
            if (messageId == null || messageId.isEmpty()) {
                continue;
            }

            try {
                checkMessageReactions(channel, messageId, data);
            } catch (Exception e) {
                plugin.getLogger()
                        .warning("[ReactionPolling] Error checking message " + messageId + ": " + e.getMessage());
            }
        }
    }

    private void checkMessageReactions(TextChannel channel, String messageId, MarkerData data) {
        // Retrieve the message
        channel.retrieveMessageById(messageId).queue(
                message -> processMessage(message, data),
                error -> {
                    // Message might have been deleted
                    plugin.getLogger().warning(
                            "[ReactionPolling] Could not retrieve message " + messageId + ": " + error.getMessage());
                });
    }

    private void processMessage(Message message, MarkerData data) {
        boolean hasApproval = false;
        boolean hasRejection = false;
        User approver = null;
        User rejector = null;

        for (MessageReaction reaction : message.getReactions()) {
            String emojiName = reaction.getReactionEmote().getName();

            // Check if non-bot users have reacted
            if (emojiName.equals(APPROVE_EMOJI) || emojiName.equals("white_check_mark")) {
                // Check reaction count (bot adds 1, so we need more than 1)
                if (reaction.getCount() > 1) {
                    hasApproval = true;
                    // Try to get the user who reacted
                    try {
                        List<User> users = reaction.retrieveUsers().complete();
                        for (User user : users) {
                            if (!user.isBot()) {
                                approver = user;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore, still process with hasApproval = true
                    }
                }
            } else if (emojiName.equals(REJECT_EMOJI) || emojiName.equals("x")) {
                if (reaction.getCount() > 1) {
                    hasRejection = true;
                    try {
                        List<User> users = reaction.retrieveUsers().complete();
                        for (User user : users) {
                            if (!user.isBot()) {
                                rejector = user;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }

        // Process approval/rejection on main thread
        if (hasApproval) {
            final User finalApprover = approver;
            final String approverName = finalApprover != null ? finalApprover.getName() : "管理者";
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("[ReactionPolling] Approving " + data.getShopName() + " by " + approverName);
                approveMarker(data, approverName);
                // Delete the Discord message after processing
                message.delete().queue(
                        v -> plugin.getLogger().info("[ReactionPolling] Approval message deleted"),
                        e -> plugin.getLogger()
                                .warning("[ReactionPolling] Failed to delete message: " + e.getMessage()));
            });
        } else if (hasRejection) {
            final User finalRejector = rejector;
            final String rejectorName = finalRejector != null ? finalRejector.getName() : "管理者";
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("[ReactionPolling] Rejecting " + data.getShopName() + " by " + rejectorName);
                rejectMarker(data, rejectorName);
                // Delete the Discord message after processing
                message.delete().queue(
                        v -> plugin.getLogger().info("[ReactionPolling] Rejection message deleted"),
                        e -> plugin.getLogger()
                                .warning("[ReactionPolling] Failed to delete message: " + e.getMessage()));
            });
        }
    }

    private void approveMarker(MarkerData data, String approverName) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(data.getOwnerUUID());
        String ownerName = owner.getName() != null ? owner.getName() : "Unknown";

        // Update status
        data.setStatus(MarkerStatus.COMMERCIAL);
        plugin.getMarkerStorage().updateMarker(data);

        // Create Dynmap marker
        boolean created = plugin.getDynmapManager().createCommercialMarker(data, ownerName);

        if (created) {
            plugin.getLogger().info("Commercial marker approved: " + data.getShopName());

            // Send history message to Discord
            plugin.getDiscordManager().sendApprovalHistory(data.getShopName(), ownerName, approverName);

            if (owner.isOnline() && owner.getPlayer() != null) {
                String message = plugin.getConfigManager().formatMessage(
                        plugin.getConfigManager().getCommercialApproved(),
                        "%shop%", data.getShopName());
                owner.getPlayer().sendMessage(message);
            }
        } else {
            plugin.getLogger().warning("Failed to create Dynmap marker: " + data.getShopName());
        }
    }

    private void rejectMarker(MarkerData data, String rejectorName) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(data.getOwnerUUID());
        String ownerName = owner.getName() != null ? owner.getName() : "Unknown";

        // Refund the commercial fee
        int refundAmount = plugin.getConfigManager().getCommercialFee();
        plugin.getEconomyManager().deposit(owner, refundAmount);

        // Remove from storage
        plugin.getMarkerStorage().removeMarker(data.getShopName());

        plugin.getLogger()
                .info("Commercial marker rejected: " + data.getShopName() + " (refunded " + refundAmount + ")");

        // Send history message to Discord
        plugin.getDiscordManager().sendRejectionHistory(data.getShopName(), ownerName, rejectorName);

        // Notify player if online
        if (owner.isOnline() && owner.getPlayer() != null) {
            String currencyName = plugin.getConfigManager().getCurrencyName();
            owner.getPlayer().sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c商業施設「§e" + data.getShopName() + "§c」の申請が却下されました。§e" +
                    refundAmount + " " + currencyName + "§cを返金しました。");
        }
    }
}
