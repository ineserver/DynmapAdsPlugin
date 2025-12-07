package net.inecat.dynmapads.discord;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.inecat.dynmapads.DynmapAdsPlugin;
import net.inecat.dynmapads.data.MarkerData;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

/**
 * Manages Discord integration via DiscordSRV.
 */
public class DiscordManager {
    private static final String MAP_URL_FORMAT = "https://map.1necat.net/?worldname=%s&mapname=flat&zoom=5&x=%.0f&y=%.0f&z=%.0f";

    private final DynmapAdsPlugin plugin;
    private ReactionListener reactionListener;
    private boolean ready = false;

    public DiscordManager(DynmapAdsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize Discord integration.
     * Should be called after DiscordSRV is ready.
     */
    public void initialize() {
        JDA jda = getJda();
        if (jda == null) {
            plugin.getLogger().warning("JDA is null - DiscordSRV may not be fully initialized");
            return;
        }

        // Register reaction listener
        reactionListener = new ReactionListener(plugin);
        jda.addEventListener(reactionListener.getJdaListener());
        ready = true;

        plugin.getLogger().info("Discord integration initialized.");
    }

    /**
     * Shutdown Discord integration.
     */
    public void shutdown() {
        if (reactionListener != null) {
            JDA jda = getJda();
            if (jda != null) {
                jda.removeEventListener(reactionListener.getJdaListener());
            }
            reactionListener = null;
        }
        ready = false;
    }

    /**
     * Check if Discord is ready.
     */
    public boolean isReady() {
        return ready && getJda() != null;
    }

    /**
     * Get JDA instance from DiscordSRV.
     */
    private JDA getJda() {
        try {
            return DiscordUtil.getJda();
        } catch (Exception e) {
            plugin.getLogger().severe("Exception in getJda(): " + e.getMessage());
            return null;
        }
    }

    /**
     * Send approval request to Discord.
     */
    public CompletableFuture<String> sendApprovalRequest(MarkerData data, String playerName) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (!isReady()) {
            future.completeExceptionally(new IllegalStateException("Discord not ready"));
            return future;
        }

        String channelId = plugin.getConfigManager().getApprovalChannelId();
        if (channelId == null || channelId.isEmpty() || channelId.equals("YOUR_APPROVAL_CHANNEL_ID")) {
            plugin.getLogger().severe("Approval channel ID is not configured!");
            future.completeExceptionally(new IllegalStateException("Channel ID not configured"));
            return future;
        }

        TextChannel channel = DiscordUtil.getTextChannelById(channelId);

        if (channel == null) {
            plugin.getLogger().severe("Channel not found for ID: " + channelId);
            future.completeExceptionally(new IllegalStateException("Channel not found: " + channelId));
            return future;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏪 商業施設申請")
                .setColor(Color.ORANGE)
                .addField("店名", data.getShopName(), true)
                .addField("申請者", playerName, true)
                .addField("説明", data.getDescription(), false)
                .addField("座標", String.format("%s: %.0f, %.0f, %.0f",
                        data.getWorld(), data.getX(), data.getY(), data.getZ()), false)
                .setFooter("✅ 承認 / ❌ 却下");

        channel.sendMessageEmbeds(embed.build()).queue(
                message -> {
                    // Add approval reaction
                    message.addReaction("✅").queue();
                    // Add rejection reaction
                    message.addReaction("❌").queue();
                    future.complete(message.getId());
                },
                error -> {
                    plugin.getLogger().severe("Failed to send message: " + error.getMessage());
                    future.completeExceptionally(error);
                });

        return future;
    }

    /**
     * Send advertisement notification to Discord.
     */
    public void sendAdsNotification(MarkerData data, String playerName, int days) {
        if (!isReady()) {
            plugin.getLogger().warning("Discord not ready for ads notification");
            return;
        }

        String channelId = plugin.getConfigManager().getAdsChannelId();
        TextChannel channel = DiscordUtil.getTextChannelById(channelId);

        if (channel == null) {
            plugin.getLogger().warning("Ads channel not found: " + channelId);
            return;
        }

        // Generate map URL
        String mapUrl = String.format(MAP_URL_FORMAT,
                data.getWorld(), data.getX(), data.getY(), data.getZ());

        // Build plain text message
        StringBuilder message = new StringBuilder();
        message.append("📢 **広告掲載開始**\n\n");
        message.append("**店名:** ").append(data.getShopName()).append("\n");
        message.append("**オーナー:** ").append(playerName).append("\n");
        message.append("**掲載期間:** ").append(days).append("日間\n");
        message.append("**説明:** ").append(data.getDescription()).append("\n");

        if (data.getPrMessage() != null && !data.getPrMessage().isEmpty()) {
            message.append("**PR:** ").append(data.getPrMessage()).append("\n");
        }

        message.append("**座標:** ").append(String.format("%s: %.0f, %.0f, %.0f",
                data.getWorld(), data.getX(), data.getY(), data.getZ())).append("\n");
        message.append("\n🗺️ **店舗周辺地図を見る:** ").append(mapUrl);

        channel.sendMessage(message.toString()).queue(
                success -> plugin.getLogger().info("Ads notification sent: " + data.getShopName()),
                error -> plugin.getLogger().severe("Failed to send ads notification: " + error.getMessage()));
    }

    /**
     * Delete an approval message and send a history log.
     */
    public void deleteApprovalMessage(String messageId, String shopName, String reason) {
        if (!isReady()) {
            return;
        }

        String channelId = plugin.getConfigManager().getApprovalChannelId();
        TextChannel channel = DiscordUtil.getTextChannelById(channelId);

        if (channel == null) {
            return;
        }

        // Delete the message
        channel.deleteMessageById(messageId).queue();

        // Send history log
        sendHistoryMessage(shopName, "取消", reason, Color.GRAY);
    }

    /**
     * Send approval history message to the approval channel.
     */
    public void sendApprovalHistory(String shopName, String ownerName, String approverName) {
        sendHistoryMessage(shopName, "承認",
                "店主: " + ownerName + "\n承認者: " + approverName,
                Color.GREEN);
    }

    /**
     * Send rejection history message to the approval channel.
     */
    public void sendRejectionHistory(String shopName, String ownerName, String rejectorName) {
        sendHistoryMessage(shopName, "却下",
                "店主: " + ownerName + "\n却下者: " + rejectorName + "\n※料金は返金されました",
                Color.RED);
    }

    /**
     * Send a history message to the approval channel.
     */
    private void sendHistoryMessage(String shopName, String action, String details, Color color) {
        if (!isReady()) {
            return;
        }

        String channelId = plugin.getConfigManager().getApprovalChannelId();
        TextChannel channel = DiscordUtil.getTextChannelById(channelId);

        if (channel == null) {
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 " + action + ": " + shopName)
                .setColor(color)
                .setDescription(details)
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue();
    }
}
