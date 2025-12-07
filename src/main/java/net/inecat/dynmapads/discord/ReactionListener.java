package net.inecat.dynmapads.discord;

import github.scarsz.discordsrv.dependencies.jda.api.events.message.react.MessageReactionAddEvent;
import net.inecat.dynmapads.DynmapAdsPlugin;
import net.inecat.dynmapads.data.MarkerData;
import net.inecat.dynmapads.data.MarkerStatus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * Listens for Discord reactions to approve/reject commercial facilities.
 * Note: This listener may not receive events with DiscordSRV.
 * ReactionPollingTask is used as a fallback.
 */
public class ReactionListener {
    private static final String APPROVE_EMOJI = "✅";
    private static final String APPROVE_EMOJI_NAME = "white_check_mark";
    private static final String REJECT_EMOJI = "❌";
    private static final String REJECT_EMOJI_NAME = "x";

    private final DynmapAdsPlugin plugin;
    private final JdaReactionListener jdaListener;

    public ReactionListener(DynmapAdsPlugin plugin) {
        this.plugin = plugin;
        this.jdaListener = new JdaReactionListener();
    }

    public JdaReactionListener getJdaListener() {
        return jdaListener;
    }

    /**
     * Inner class that extends JDA ListenerAdapter
     */
    public class JdaReactionListener extends github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter {

        @Override
        public void onMessageReactionAdd(MessageReactionAddEvent event) {
            handleReaction(event);
        }
    }

    private void handleReaction(MessageReactionAddEvent event) {
        String emojiName;
        try {
            emojiName = event.getReactionEmote().getName();
        } catch (Exception e) {
            return;
        }

        // Ignore bot reactions
        if (event.getUser() != null && event.getUser().isBot()) {
            return;
        }

        // Check if it's the approval or rejection emoji
        boolean isApproveEmoji = emojiName.equals(APPROVE_EMOJI) || emojiName.equals(APPROVE_EMOJI_NAME);
        boolean isRejectEmoji = emojiName.equals(REJECT_EMOJI) || emojiName.equals(REJECT_EMOJI_NAME);

        if (!isApproveEmoji && !isRejectEmoji) {
            return;
        }

        // Check if it's in the approval channel
        String approvalChannelId = plugin.getConfigManager().getApprovalChannelId();
        if (!event.getChannel().getId().equals(approvalChannelId)) {
            return;
        }

        String messageId = event.getMessageId();
        final boolean approve = isApproveEmoji;

        // Run on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            MarkerData data = plugin.getMarkerStorage().getByDiscordMessageId(messageId);

            if (data == null || data.getStatus() != MarkerStatus.PENDING) {
                return;
            }

            if (approve) {
                approveMarker(data);
            } else {
                rejectMarker(data);
            }
        });
    }

    private void approveMarker(MarkerData data) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(data.getOwnerUUID());
        String ownerName = owner.getName() != null ? owner.getName() : "Unknown";

        // Update status
        data.setStatus(MarkerStatus.COMMERCIAL);
        plugin.getMarkerStorage().updateMarker(data);

        // Create Dynmap marker
        boolean created = plugin.getDynmapManager().createCommercialMarker(data, ownerName);

        if (created) {
            plugin.getLogger().info("Commercial marker approved: " + data.getShopName());

            if (owner.isOnline() && owner.getPlayer() != null) {
                String message = plugin.getConfigManager().formatMessage(
                        plugin.getConfigManager().getCommercialApproved(),
                        "%shop%", data.getShopName());
                owner.getPlayer().sendMessage(message);
            }
        } else {
            plugin.getLogger().warning("Failed to create marker: " + data.getShopName());
        }
    }

    private void rejectMarker(MarkerData data) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(data.getOwnerUUID());

        // Refund the commercial fee
        int refundAmount = plugin.getConfigManager().getCommercialFee();
        plugin.getEconomyManager().deposit(owner, refundAmount);

        // Remove from storage
        plugin.getMarkerStorage().removeMarker(data.getShopName());

        plugin.getLogger().info("Commercial marker rejected: " + data.getShopName());

        // Notify player if online
        if (owner.isOnline() && owner.getPlayer() != null) {
            String currencyName = plugin.getConfigManager().getCurrencyName();
            owner.getPlayer().sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c商業施設「§e" + data.getShopName() + "§c」の申請が却下されました。§e" +
                    refundAmount + " " + currencyName + "§cを返金しました。");
        }
    }
}
