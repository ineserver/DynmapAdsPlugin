package net.inecat.dynmapads.dynmap;

import net.inecat.dynmapads.DynmapAdsPlugin;
import net.inecat.dynmapads.data.MarkerData;
import net.inecat.dynmapads.data.MarkerStatus;
import org.bukkit.Bukkit;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

/**
 * Manages Dynmap marker operations.
 */
public class DynmapManager {
    private final DynmapAdsPlugin plugin;
    private DynmapAPI dynmapAPI;
    private MarkerAPI markerAPI;
    private MarkerSet commercialSet;
    private MarkerSet adsSet;

    public DynmapManager(DynmapAdsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Setup Dynmap integration.
     * 
     * @return true if setup was successful
     */
    public boolean setup() {
        org.bukkit.plugin.Plugin dynmapPlugin = Bukkit.getPluginManager().getPlugin("dynmap");
        if (dynmapPlugin == null) {
            plugin.getLogger().severe("Dynmap not found!");
            return false;
        }

        dynmapAPI = (DynmapAPI) dynmapPlugin;
        markerAPI = dynmapAPI.getMarkerAPI();

        if (markerAPI == null) {
            plugin.getLogger().severe("Dynmap Marker API not available!");
            return false;
        }

        // Get or create marker sets
        String commercialSetId = plugin.getConfigManager().getCommercialMarkerSet();
        String adsSetId = plugin.getConfigManager().getAdsMarkerSet();

        commercialSet = markerAPI.getMarkerSet(commercialSetId);
        if (commercialSet == null) {
            commercialSet = markerAPI.createMarkerSet(commercialSetId, "商業施設", null, false);
            if (commercialSet != null) {
                commercialSet.setHideByDefault(false);
                commercialSet.setLayerPriority(10);
            }
        }

        adsSet = markerAPI.getMarkerSet(adsSetId);
        if (adsSet == null) {
            adsSet = markerAPI.createMarkerSet(adsSetId, "広告", null, false);
            if (adsSet != null) {
                adsSet.setHideByDefault(false);
                adsSet.setLayerPriority(20);
            }
        }

        if (commercialSet == null || adsSet == null) {
            plugin.getLogger().severe("Failed to create marker sets!");
            return false;
        }

        plugin.getLogger().info("Dynmap marker sets initialized.");
        return true;
    }

    /**
     * Create a marker for a commercial facility.
     */
    public boolean createCommercialMarker(MarkerData data, String ownerName) {
        if (commercialSet == null)
            return false;

        String markerId = sanitizeMarkerId(data.getShopName());
        String html = buildHtml(data, ownerName, false);

        Marker marker = commercialSet.createMarker(
                markerId,
                data.getShopName(),
                data.getWorld(),
                data.getX(),
                data.getY(),
                data.getZ(),
                markerAPI.getMarkerIcon("default"),
                false);

        if (marker != null) {
            marker.setDescription(html);
            return true;
        }
        return false;
    }

    /**
     * Create a marker for an advertisement.
     */
    public boolean createAdsMarker(MarkerData data, String ownerName) {
        if (adsSet == null)
            return false;

        String markerId = sanitizeMarkerId(data.getShopName());
        String html = buildHtml(data, ownerName, true);

        Marker marker = adsSet.createMarker(
                markerId,
                data.getShopName(),
                data.getWorld(),
                data.getX(),
                data.getY(),
                data.getZ(),
                markerAPI.getMarkerIcon("default"),
                false);

        if (marker != null) {
            marker.setDescription(html);
            return true;
        }
        return false;
    }

    /**
     * Delete a marker from commercial set.
     */
    public boolean deleteCommercialMarker(String shopName) {
        if (commercialSet == null)
            return false;

        String markerId = sanitizeMarkerId(shopName);
        Marker marker = commercialSet.findMarker(markerId);
        if (marker != null) {
            marker.deleteMarker();
            return true;
        }
        return false;
    }

    /**
     * Delete a marker from ads set.
     */
    public boolean deleteAdsMarker(String shopName) {
        if (adsSet == null)
            return false;

        String markerId = sanitizeMarkerId(shopName);
        Marker marker = adsSet.findMarker(markerId);
        if (marker != null) {
            marker.deleteMarker();
            return true;
        }
        return false;
    }

    /**
     * Delete a marker from any set.
     */
    public void deleteMarker(MarkerData data) {
        if (data.getStatus() == MarkerStatus.ADS) {
            deleteAdsMarker(data.getShopName());
        } else {
            deleteCommercialMarker(data.getShopName());
        }
    }

    /**
     * Move marker from commercial to ads set.
     */
    public boolean moveToAds(MarkerData data, String ownerName) {
        deleteCommercialMarker(data.getShopName());
        return createAdsMarker(data, ownerName);
    }

    /**
     * Move marker from ads to commercial set.
     */
    public boolean moveToCommercial(MarkerData data, String ownerName) {
        deleteAdsMarker(data.getShopName());
        return createCommercialMarker(data, ownerName);
    }

    /**
     * Build HTML description for marker popup.
     */
    private String buildHtml(MarkerData data, String ownerName, boolean includeAds) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"shop-entry");
        if (includeAds && data.getStatus() == MarkerStatus.ADS) {
            html.append(" shop-ads");
        }
        html.append("\">");

        html.append("<div class=\"shop-name\">").append(escapeHtml(data.getShopName())).append("</div>");
        html.append("<div class=\"shop-description\">").append(escapeHtml(data.getDescription())).append("</div>");
        html.append("<div class=\"shop-owner\">").append(escapeHtml(ownerName)).append("</div>");

        if (includeAds && data.getPrMessage() != null && !data.getPrMessage().isEmpty()) {
            html.append("<div class=\"shop-pr\">").append(escapeHtml(data.getPrMessage())).append("</div>");
        }

        html.append("</div>");
        return html.toString();
    }

    /**
     * Escape HTML special characters.
     */
    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Sanitize shop name for use as marker ID.
     */
    private String sanitizeMarkerId(String shopName) {
        return shopName.toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_")
                .replaceAll("_+", "_");
    }
}
