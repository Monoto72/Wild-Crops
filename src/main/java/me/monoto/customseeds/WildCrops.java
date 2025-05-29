package me.monoto.customseeds;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.crops.CropGrowthScheduler;
import me.monoto.customseeds.listeners.CropBlockListener;
import me.monoto.customseeds.listeners.CropChunkListener;
import me.monoto.customseeds.listeners.CropPlaceListener;
import me.monoto.customseeds.utils.BlockCache;
import me.monoto.customseeds.utils.ChatInput.ChatInput;
import me.monoto.customseeds.utils.DependencyManager;
import me.monoto.customseeds.utils.FileManager;
import me.monoto.customseeds.utils.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.invui.InvUI;

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

    @Override
    public void onEnable() {
        System.out.println("🕵️ CraftLegacy warning likely coming up next — check stack trace if it does.");
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

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(WildCropsCommand.commandRoot.build());
        });

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

}
