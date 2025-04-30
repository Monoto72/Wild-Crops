package me.monoto.customseeds.utils;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import me.monoto.customseeds.WildCrops;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class DependencyManager {
    private static Economy econ = null;
    private static boolean skyblockEnabled;
    private static boolean mcMMOEnabled;

    public static void setupAll(WildCrops plugin) {
        setupEconomy(plugin);
        setupSkyblock(plugin);
        setupMcMMO(plugin);
    }

    private static void setupEconomy(WildCrops plugin) {
        try {
            RegisteredServiceProvider<Economy> rsp =
                    plugin.getServer().getServicesManager()
                            .getRegistration(Economy.class);
            if (rsp != null) {
                econ = rsp.getProvider();
                plugin.getLogger().info("Vault economy hooked: " + econ.getName());
            } else {
                plugin.getLogger().warning("Vault not found — in-game money disabled");
            }
        } catch (NoClassDefFoundError e) {
            // Vault API classes aren’t even on the classpath
            plugin.getLogger().warning("Vault API missing — skipping economy integration");
        }
    }

    private static void setupSkyblock(WildCrops plugin) {
        skyblockEnabled = plugin.getServer()
                .getPluginManager()
                .isPluginEnabled("SuperiorSkyblock2");
        plugin.getLogger().info(
                skyblockEnabled
                        ? "SuperiorSkyblock2 hooked"
                        : "SuperiorSkyblock2 not found; island perms disabled"
        );
    }

    public static void setupMcMMO(WildCrops plugin) {
        mcMMOEnabled = Bukkit.getPluginManager().isPluginEnabled("mcmmo");
        plugin.getLogger().info(
                mcMMOEnabled
                        ? "mcmmo hooked"
                        : "mcmmo not found; mcmmo features disabled"
        );
    }


    public static Economy getEcon() {
        return econ;
    }

    public static boolean isSkyblockEnabled() {
        return skyblockEnabled;
    }

    public static boolean isMcMMOEnabled() {
        return mcMMOEnabled;
    }
}
