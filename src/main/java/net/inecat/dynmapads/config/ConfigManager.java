package net.inecat.dynmapads.config;

import net.inecat.dynmapads.DynmapAdsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages plugin configuration.
 */
public class ConfigManager {
    private final DynmapAdsPlugin plugin;
    private FileConfiguration config;

    // Discord settings
    private String approvalChannelId;
    private String adsChannelId;

    // Economy settings
    private int commercialFee;
    private int adsFeePerDay;
    private String currencyName;

    // Dynmap settings
    private String commercialMarkerSet;
    private String adsMarkerSet;

    // Messages
    private String prefix;
    private String noPermission;
    private String playerOnly;
    private String shopExists;
    private String shopNotFound;
    private String notOwner;
    private String insufficientFunds;
    private String commercialPending;
    private String commercialApproved;
    private String adsStarted;
    private String adsExpired;
    private String shopDeleted;
    private String usageCommercial;
    private String usageAds;
    private String usageDelete;

    public ConfigManager(DynmapAdsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Discord
        approvalChannelId = config.getString("discord.approval-channel-id", "");
        adsChannelId = config.getString("discord.ads-channel-id", "786582642455478273");

        // Economy
        commercialFee = config.getInt("economy.commercial-fee", 10000);
        adsFeePerDay = config.getInt("economy.ads-fee-per-day", 30000);
        currencyName = config.getString("economy.currency-name", "ine");

        // Dynmap
        commercialMarkerSet = config.getString("dynmap.commercial-marker-set", "commercial");
        adsMarkerSet = config.getString("dynmap.ads-marker-set", "ads");

        // Messages
        prefix = colorize(config.getString("messages.prefix", "&8[&6DynmapAds&8] &r"));
        noPermission = colorize(config.getString("messages.no-permission", "&c権限がありません。"));
        playerOnly = colorize(config.getString("messages.player-only", "&cこのコマンドはプレイヤーのみ実行可能です。"));
        shopExists = colorize(config.getString("messages.shop-exists", "&cその店名は既に使用されています: &e%shop%"));
        shopNotFound = colorize(config.getString("messages.shop-not-found", "&c店舗が見つかりません: &e%shop%"));
        notOwner = colorize(config.getString("messages.not-owner", "&cあなたはこの店舗のオーナーではありません。"));
        insufficientFunds = colorize(
                config.getString("messages.insufficient-funds", "&c残高が不足しています。必要額: &e%amount% %currency%"));
        commercialPending = colorize(
                config.getString("messages.commercial-pending", "&a商業施設「&e%shop%&a」の申請を送信しました。Discord承認をお待ちください。"));
        commercialApproved = colorize(
                config.getString("messages.commercial-approved", "&a商業施設「&e%shop%&a」が承認され、Dynmapに登録されました！"));
        adsStarted = colorize(config.getString("messages.ads-started", "&a広告「&e%shop%&a」を &e%days%日間 &a掲載開始しました！"));
        adsExpired = colorize(config.getString("messages.ads-expired", "&e広告「&6%shop%&e」の掲載期間が終了しました。"));
        shopDeleted = colorize(config.getString("messages.shop-deleted", "&a店舗「&e%shop%&a」を削除しました。"));
        usageCommercial = colorize(
                config.getString("messages.usage-commercial", "&c使用法: /mapmarker commercial <店名> <説明>"));
        usageAds = colorize(config.getString("messages.usage-ads", "&c使用法: /mapmarker ads <店名> <期間(日)> [宣伝文句]"));
        usageDelete = colorize(config.getString("messages.usage-delete", "&c使用法: /mapmarker delete <店名>"));
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // Getters
    public String getApprovalChannelId() {
        return approvalChannelId;
    }

    public String getAdsChannelId() {
        return adsChannelId;
    }

    public int getCommercialFee() {
        return commercialFee;
    }

    public int getAdsFeePerDay() {
        return adsFeePerDay;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public String getCommercialMarkerSet() {
        return commercialMarkerSet;
    }

    public String getAdsMarkerSet() {
        return adsMarkerSet;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getPlayerOnly() {
        return playerOnly;
    }

    public String getShopExists() {
        return shopExists;
    }

    public String getShopNotFound() {
        return shopNotFound;
    }

    public String getNotOwner() {
        return notOwner;
    }

    public String getInsufficientFunds() {
        return insufficientFunds;
    }

    public String getCommercialPending() {
        return commercialPending;
    }

    public String getCommercialApproved() {
        return commercialApproved;
    }

    public String getAdsStarted() {
        return adsStarted;
    }

    public String getAdsExpired() {
        return adsExpired;
    }

    public String getShopDeleted() {
        return shopDeleted;
    }

    public String getUsageCommercial() {
        return usageCommercial;
    }

    public String getUsageAds() {
        return usageAds;
    }

    public String getUsageDelete() {
        return usageDelete;
    }

    // Message formatting helpers
    public String formatMessage(String message, String... replacements) {
        String result = prefix + message;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        return result;
    }
}
