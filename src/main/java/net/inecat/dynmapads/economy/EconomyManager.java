package net.inecat.dynmapads.economy;

import net.inecat.dynmapads.DynmapAdsPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Manages Vault economy integration.
 */
public class EconomyManager {
    private final DynmapAdsPlugin plugin;
    private Economy economy;

    public EconomyManager(DynmapAdsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Setup the economy provider.
     * 
     * @return true if setup was successful
     */
    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault not found!");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe("No economy provider found!");
            return false;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("Hooked into economy: " + economy.getName());
        return true;
    }

    /**
     * Check if player has enough balance.
     */
    public boolean hasBalance(OfflinePlayer player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    /**
     * Get player's balance.
     */
    public double getBalance(OfflinePlayer player) {
        return economy != null ? economy.getBalance(player) : 0;
    }

    /**
     * Withdraw amount from player.
     * 
     * @return true if withdrawal was successful
     */
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (economy == null)
            return false;

        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * Deposit amount to player.
     * 
     * @return true if deposit was successful
     */
    public boolean deposit(OfflinePlayer player, double amount) {
        if (economy == null)
            return false;

        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * Format currency amount for display.
     */
    public String format(double amount) {
        return economy != null ? economy.format(amount) : String.valueOf(amount);
    }
}
