package net.inecat.dynmapads.tasks;

import net.inecat.dynmapads.DynmapAdsPlugin;
import net.inecat.dynmapads.data.MarkerData;
import net.inecat.dynmapads.data.MarkerStatus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Scheduled task to check for expired advertisements and revert them to
 * commercial.
 */
public class AdExpirationTask extends BukkitRunnable {
    private final DynmapAdsPlugin plugin;

    public AdExpirationTask(DynmapAdsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        List<MarkerData> expiredAds = plugin.getMarkerStorage().getExpiredAds();

        for (MarkerData data : expiredAds) {
            expireAd(data);
        }
    }

    private void expireAd(MarkerData data) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(data.getOwnerUUID());
        String ownerName = owner.getName() != null ? owner.getName() : "Unknown";

        // Clear ads-specific data
        data.setStatus(MarkerStatus.COMMERCIAL);
        data.setAdsEndTime(null);
        data.setPrMessage(null);
        plugin.getMarkerStorage().updateMarker(data);

        // Move marker from ads to commercial
        plugin.getDynmapManager().moveToCommercial(data, ownerName);

        plugin.getLogger().info("Advertisement expired: " + data.getShopName());

        // Notify player if online
        if (owner.isOnline() && owner.getPlayer() != null) {
            String message = plugin.getConfigManager().formatMessage(
                    plugin.getConfigManager().getAdsExpired(),
                    "%shop%", data.getShopName());
            owner.getPlayer().sendMessage(message);
        }
    }
}
