package net.inecat.dynmapads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import net.inecat.dynmapads.commands.MapMarkerCommand;
import net.inecat.dynmapads.commands.MapMarkerTabCompleter;
import net.inecat.dynmapads.config.ConfigManager;
import net.inecat.dynmapads.data.MarkerStorage;
import net.inecat.dynmapads.discord.DiscordManager;
import net.inecat.dynmapads.dynmap.DynmapManager;
import net.inecat.dynmapads.economy.EconomyManager;
import net.inecat.dynmapads.tasks.AdExpirationTask;
import net.inecat.dynmapads.tasks.ReactionPollingTask;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for DynmapAdsPlugin.
 * Enables players to create commercial facility and advertisement markers on
 * Dynmap
 * with Discord approval and Vault economy integration.
 */
public class DynmapAdsPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private MarkerStorage markerStorage;
    private EconomyManager economyManager;
    private DynmapManager dynmapManager;
    private DiscordManager discordManager;
    private AdExpirationTask expirationTask;
    private ReactionPollingTask pollingTask;

    @Override
    public void onEnable() {
        // Initialize config
        configManager = new ConfigManager(this);
        configManager.load();

        // Initialize marker storage
        markerStorage = new MarkerStorage(this);
        markerStorage.load();

        // Setup economy
        economyManager = new EconomyManager(this);
        if (!economyManager.setup()) {
            getLogger().severe("Failed to setup Vault economy! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Setup Dynmap
        dynmapManager = new DynmapManager(this);
        if (!dynmapManager.setup()) {
            getLogger().severe("Failed to setup Dynmap! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Setup Discord (via DiscordSRV)
        discordManager = new DiscordManager(this);
        DiscordSRV.api.subscribe(this);

        // Check if DiscordSRV is already ready (plugin might have loaded after
        // DiscordSRV connected)
        if (github.scarsz.discordsrv.util.DiscordUtil.getJda() != null) {
            getLogger().info("DiscordSRV is already connected, initializing Discord integration immediately...");
            discordManager.initialize();
        } else {
            getLogger().info("Waiting for DiscordSRV to connect (DiscordReadyEvent)...");
        }

        // Register commands
        MapMarkerCommand commandExecutor = new MapMarkerCommand(this);
        MapMarkerTabCompleter tabCompleter = new MapMarkerTabCompleter(this);

        if (getCommand("mapmarker") != null) {
            getCommand("mapmarker").setExecutor(commandExecutor);
            getCommand("mapmarker").setTabCompleter(tabCompleter);
        }

        // Start expiration task (runs every minute = 1200 ticks)
        expirationTask = new AdExpirationTask(this);
        expirationTask.runTaskTimer(this, 1200L, 1200L);

        // Start reaction polling task (runs every 5 seconds = 100 ticks)
        pollingTask = new ReactionPollingTask(this);
        pollingTask.runTaskTimerAsynchronously(this, 100L, 100L);
        getLogger().info("Reaction polling task started (every 5 seconds).");

        getLogger().info("DynmapAdsPlugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Cancel scheduled tasks
        if (expirationTask != null) {
            expirationTask.cancel();
        }
        if (pollingTask != null) {
            pollingTask.cancel();
        }

        // Shutdown Discord
        if (discordManager != null) {
            discordManager.shutdown();
        }

        // Unsubscribe from DiscordSRV
        DiscordSRV.api.unsubscribe(this);

        // Save data
        if (markerStorage != null) {
            markerStorage.save();
        }

        getLogger().info("DynmapAdsPlugin disabled.");
    }

    /**
     * Called when DiscordSRV's JDA is ready.
     */
    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        getLogger().info("DiscordSRV is ready, initializing Discord integration...");
        discordManager.initialize();
    }

    // Getters for managers
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MarkerStorage getMarkerStorage() {
        return markerStorage;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public DynmapManager getDynmapManager() {
        return dynmapManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }
}
