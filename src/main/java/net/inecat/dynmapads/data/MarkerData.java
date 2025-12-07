package net.inecat.dynmapads.data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data class representing a shop marker.
 */
public class MarkerData {
    private final String shopName;
    private final UUID ownerUUID;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final String description;
    private MarkerStatus status;
    private LocalDateTime adsEndTime;
    private String prMessage;
    private String discordMessageId;

    public MarkerData(String shopName, UUID ownerUUID, String world, double x, double y, double z, String description) {
        this.shopName = shopName;
        this.ownerUUID = ownerUUID;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.description = description;
        this.status = MarkerStatus.PENDING;
        this.adsEndTime = null;
        this.prMessage = null;
        this.discordMessageId = null;
    }

    // Full constructor for loading from storage
    public MarkerData(String shopName, UUID ownerUUID, String world, double x, double y, double z,
            String description, MarkerStatus status, LocalDateTime adsEndTime, String prMessage,
            String discordMessageId) {
        this.shopName = shopName;
        this.ownerUUID = ownerUUID;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.description = description;
        this.status = status;
        this.adsEndTime = adsEndTime;
        this.prMessage = prMessage;
        this.discordMessageId = discordMessageId;
    }

    public String getShopName() {
        return shopName;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getDescription() {
        return description;
    }

    public MarkerStatus getStatus() {
        return status;
    }

    public void setStatus(MarkerStatus status) {
        this.status = status;
    }

    public LocalDateTime getAdsEndTime() {
        return adsEndTime;
    }

    public void setAdsEndTime(LocalDateTime adsEndTime) {
        this.adsEndTime = adsEndTime;
    }

    public String getPrMessage() {
        return prMessage;
    }

    public void setPrMessage(String prMessage) {
        this.prMessage = prMessage;
    }

    public String getDiscordMessageId() {
        return discordMessageId;
    }

    public void setDiscordMessageId(String discordMessageId) {
        this.discordMessageId = discordMessageId;
    }

    public boolean isOwner(UUID uuid) {
        return ownerUUID.equals(uuid);
    }

    public boolean isAdsExpired() {
        if (status != MarkerStatus.ADS || adsEndTime == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(adsEndTime);
    }
}
