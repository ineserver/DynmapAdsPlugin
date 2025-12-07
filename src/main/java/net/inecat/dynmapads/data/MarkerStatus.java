package net.inecat.dynmapads.data;

/**
 * Enum representing the status of a marker.
 */
public enum MarkerStatus {
    /**
     * Marker is pending approval via Discord.
     */
    PENDING,

    /**
     * Marker is an approved commercial facility.
     */
    COMMERCIAL,

    /**
     * Marker is currently an advertisement.
     */
    ADS
}
