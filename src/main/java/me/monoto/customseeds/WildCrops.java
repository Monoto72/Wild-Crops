package me.monoto.customseeds;

import me.monoto.customseeds.commands.CommandBase;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.crops.CropGrowthScheduler;
import me.monoto.customseeds.listeners.CropBlockListener;
import me.monoto.customseeds.listeners.CropChunkListener;
import me.monoto.customseeds.listeners.CropPlaceListener;
import me.monoto.customseeds.utils.*;
import me.monoto.customseeds.utils.ChatInput.ChatInput;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.invui.InvUI;

import java.util.List;
import java.util.Objects;

public final class WildCrops extends JavaPlugin {

    private static WildCrops instance;
    private FileManager fileManager;
    private ChatInput chatInput;
    private String newVersion;
    private Metrics metric;

    public static WildCrops getInstance() {
        return instance;
    }

    public ChatInput getChatInput() {
        return chatInput;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public String getVersion() {
        return this.newVersion;
    }

    public void setVersion(String value) {
        this.newVersion = value;
    }

    private static final List<PermDefinition> PERMISSIONS = List.of(
            new PermDefinition("wildcrops.admin", "Allows access to admin commands and features", PermissionDefault.OP),
            new PermDefinition("wildcrops.settings", "Allows interaction with crops", PermissionDefault.OP),
            new PermDefinition("wildcrops.reload", "Allows placing crops", PermissionDefault.OP),
            new PermDefinition("wildcrops.give", "Allows harvesting crops", PermissionDefault.OP),
            new PermDefinition("wildcrops.version", "Shows the current version", PermissionDefault.FALSE)
    );

    @Override
    public void onEnable() {
        BlockCache.initAsync()
                .thenAccept(cache -> getLogger().info("Cache ready: "
                        + cache.placeables().size() + " placeables, "
                        + cache.drops().size() + " drops"))
                .exceptionally(err -> {
                    getLogger().severe("Failed to build BlockCache: " + err.getMessage());
                    return null;
                });
        InvUI.getInstance().setPlugin(this);

        int bstatID = 25412;
        int spigotID = 123916;

        this.metric = new Metrics(this, bstatID);

        this.fileManager = new FileManager(this);
        this.chatInput = new ChatInput(this);
        instance = this;

        DependencyManager.setupAll(this);

        CropDefinitionRegistry.init(this);
        fileManager.reloadCropConfigs();

        CropDefinitionRegistry.load(fileManager.getCropDataMap());

        new CommandBase(this);
        registerPermissions();

        CropGrowthScheduler scheduler = new CropGrowthScheduler(this);
        CropChunkListener chunkListener = new CropChunkListener(scheduler);
        CropPlaceListener placeListener = new CropPlaceListener(scheduler);

        getServer().getPluginManager().registerEvents(chunkListener, this);
        getServer().getPluginManager().registerEvents(placeListener, this);
        getServer().getPluginManager().registerEvents(new CropBlockListener(), this);

        (new UpdateChecker(this, spigotID)).getLatestVersion(version -> {
            if (!Objects.equals(getDescription().getVersion(), version)) {
                getLogger().info("Wild Crops v" + version + " is out! Download it at: https://www.spigotmc.org/resources/wild-crops.123916/");
                setVersion(version);
            } else {
                getLogger().info("No new version available");
            }
        });
    }

    @Override
    public void onDisable() {
        BlockCache.shutdown();
    }

    private void registerPermissions() {
        PluginManager pm = Bukkit.getPluginManager();

        for (PermDefinition pd : PERMISSIONS) {
            if (pm.getPermission(pd.getNode()) == null) {
                Permission perm = new Permission(pd.getNode(), pd.getDescription(), pd.getDefaultValue());
                pm.addPermission(perm);
            }
        }
    }
}
