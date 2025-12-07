package net.inecat.dynmapads.data;

import net.inecat.dynmapads.DynmapAdsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handles persistence of marker data to markers.yml.
 */
public class MarkerStorage {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DynmapAdsPlugin plugin;
    private final File dataFile;
    private final Map<String, MarkerData> markers;

    public MarkerStorage(DynmapAdsPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "markers.yml");
        this.markers = new ConcurrentHashMap<>();
    }

    /**
     * Load all markers from file.
     */
    public void load() {
        markers.clear();

        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection markersSection = config.getConfigurationSection("markers");

        if (markersSection == null) {
            return;
        }

        for (String shopName : markersSection.getKeys(false)) {
            ConfigurationSection markerSection = markersSection.getConfigurationSection(shopName);
            if (markerSection == null)
                continue;

            try {
                UUID ownerUUID = UUID.fromString(markerSection.getString("owner-uuid", ""));
                String world = markerSection.getString("world", "world");
                double x = markerSection.getDouble("x");
                double y = markerSection.getDouble("y");
                double z = markerSection.getDouble("z");
                String description = markerSection.getString("description", "");
                MarkerStatus status = MarkerStatus.valueOf(markerSection.getString("status", "PENDING"));

                LocalDateTime adsEndTime = null;
                String adsEndTimeStr = markerSection.getString("ads-end-time");
                if (adsEndTimeStr != null && !adsEndTimeStr.isEmpty()) {
                    adsEndTime = LocalDateTime.parse(adsEndTimeStr, DATE_FORMAT);
                }

                String prMessage = markerSection.getString("pr-message");
                String discordMessageId = markerSection.getString("discord-message-id");

                MarkerData data = new MarkerData(shopName, ownerUUID, world, x, y, z,
                        description, status, adsEndTime, prMessage, discordMessageId);
                markers.put(shopName, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load marker: " + shopName + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + markers.size() + " markers from storage.");
    }

    /**
     * Save all markers to file.
     */
    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection markersSection = config.createSection("markers");

        for (MarkerData data : markers.values()) {
            ConfigurationSection markerSection = markersSection.createSection(data.getShopName());
            markerSection.set("owner-uuid", data.getOwnerUUID().toString());
            markerSection.set("world", data.getWorld());
            markerSection.set("x", data.getX());
            markerSection.set("y", data.getY());
            markerSection.set("z", data.getZ());
            markerSection.set("description", data.getDescription());
            markerSection.set("status", data.getStatus().name());

            if (data.getAdsEndTime() != null) {
                markerSection.set("ads-end-time", data.getAdsEndTime().format(DATE_FORMAT));
            }
            if (data.getPrMessage() != null) {
                markerSection.set("pr-message", data.getPrMessage());
            }
            if (data.getDiscordMessageId() != null) {
                markerSection.set("discord-message-id", data.getDiscordMessageId());
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save markers: " + e.getMessage());
        }
    }

    /**
     * Add a new marker.
     */
    public void addMarker(MarkerData data) {
        markers.put(data.getShopName(), data);
        save();
    }

    /**
     * Remove a marker by shop name.
     */
    public boolean removeMarker(String shopName) {
        MarkerData removed = markers.remove(shopName);
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    /**
     * Get a marker by shop name.
     */
    public MarkerData getMarker(String shopName) {
        return markers.get(shopName);
    }

    /**
     * Check if a shop name exists.
     */
    public boolean exists(String shopName) {
        return markers.containsKey(shopName);
    }

    /**
     * Get a marker by Discord message ID.
     */
    public MarkerData getByDiscordMessageId(String messageId) {
        return markers.values().stream()
                .filter(m -> messageId.equals(m.getDiscordMessageId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all markers owned by a player.
     */
    public List<MarkerData> getByOwner(UUID ownerUUID) {
        return markers.values().stream()
                .filter(m -> m.getOwnerUUID().equals(ownerUUID))
                .collect(Collectors.toList());
    }

    /**
     * Get all markers with a specific status.
     */
    public List<MarkerData> getByStatus(MarkerStatus status) {
        return markers.values().stream()
                .filter(m -> m.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Get all expired ads.
     */
    public List<MarkerData> getExpiredAds() {
        return markers.values().stream()
                .filter(MarkerData::isAdsExpired)
                .collect(Collectors.toList());
    }

    /**
     * Get all shop names.
     */
    public Set<String> getAllShopNames() {
        return new HashSet<>(markers.keySet());
    }

    /**
     * Get all approved shop names (COMMERCIAL or ADS).
     */
    public Set<String> getApprovedShopNames() {
        return markers.values().stream()
                .filter(m -> m.getStatus() == MarkerStatus.COMMERCIAL || m.getStatus() == MarkerStatus.ADS)
                .map(MarkerData::getShopName)
                .collect(Collectors.toSet());
    }

    /**
     * Update marker and save.
     */
    public void updateMarker(MarkerData data) {
        markers.put(data.getShopName(), data);
        save();
    }
}
