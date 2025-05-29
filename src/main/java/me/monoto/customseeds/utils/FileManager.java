package me.monoto.customseeds.utils;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages loading, saving, creation, and migration of crop configuration files.
 * Uses an immutable UUID as the key so renaming files won't break mappings.
 */
public class FileManager {
    private final WildCrops plugin;
    private final File cropsFolder;
    // Map key: internal UUID (lowercase) -> CropConfigData
    private final Map<String, CropConfigData> cropData = new HashMap<>();
    private static final Set<String> usedUUIDs = new HashSet<>();
    // Default crop filenames to copy on first run
    private final List<String> defaultCrops = List.of("coal.yml");

    public FileManager(WildCrops plugin) {
        this.plugin = plugin;
        this.cropsFolder = new File(plugin.getDataFolder(), "crops");
        initializeCropFolder();
        migrateAllConfigs();
    }

    private void initializeCropFolder() {
        if (!cropsFolder.exists() && !cropsFolder.mkdirs()) {
            plugin.getLogger().severe("Failed to create crops folder!");
            return;
        }
        File[] files = cropsFolder.listFiles(f -> f.getName().endsWith(".yml"));
        if (files == null || files.length == 0) {
            for (String name : defaultCrops) {
                plugin.saveResource("crops/" + name, false);
            }
            files = cropsFolder.listFiles(f -> f.getName().endsWith(".yml"));
        }
        if (files != null) {
            for (File file : files) {
                try {
                    loadSingleFile(file);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error loading crop config '" + file.getName() + "': " + e.getMessage());
                }
            }
        }
    }

    private void loadSingleFile(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Migrate legacy config if needed
        migrateLegacyConfig(cfg, file);

        // ensure internal id
        String id = cfg.getString("id");
        if (id == null || id.isEmpty() || usedUUIDs.contains(id.toLowerCase())) {
            do {
                id = UUID.randomUUID().toString();
            }
            while (usedUUIDs.contains(id.toLowerCase()));
            cfg.set("id", id);
        }
        usedUUIDs.add(id.toLowerCase());

        // save any changes
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save crop config '" + file.getName() + "': " + e.getMessage());
            return;
        }

        CropConfigData data = new CropConfigData(file, cfg);
        cropData.put(id.toLowerCase(), data);

        // register definition under UUID key
        CropDefinition def = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), cfg);
        CropDefinitionRegistry.update(id, def);
    }

    /**
     * Detects legacy keys (item, drops) and rewrites to the new flattened schema.
     * Skips migration if required legacy fields are missing.
     */
    private void migrateLegacyConfig(YamlConfiguration cfg, File file) {
        if (!cfg.contains("item") || !cfg.contains("drops")) {
            return;
        }
        // ensure seed material exists
        if (!cfg.isString("item.material")) {
            plugin.getLogger().warning("Legacy config '" + file.getName() + "' missing 'item.material'; skipping migration.");
            return;
        }
        String seedMat = cfg.getString("item.material");
        cfg.set("material", seedMat);
        cfg.set("display_name", cfg.getString("item.display_name"));
        cfg.set("lore", cfg.getStringList("item.lore"));

        // options
        cfg.set("settings.bone_meal", cfg.getBoolean("bone_meal", false));

        // growth
        // String cropType = seedMat.replaceAll("_SEEDS?$", "").toUpperCase();
        cfg.set("settings.final_block", "FERN");

        // rewards
        List<Map<String, Object>> rewards = new ArrayList<>();
        Map<String, Object> seedReward = new HashMap<>();
        seedReward.put("type", "seed");
        seedReward.put("chance", "100%");
        seedReward.put("amount", cfg.getString("drops.seed.amount", "1"));
        rewards.add(seedReward);
        for (Map<?, ?> entry : cfg.getMapList("drops.item")) {
            Map<String, Object> r = new HashMap<>();
            r.put("type", "item");
            r.put("chance", "100%");
            r.put("material", entry.get("material"));
            r.put("amount", entry.get("amount"));
            rewards.add(r);
        }
        cfg.set("rewards", rewards);

        // cleanup
        cfg.set("item", null);
        cfg.set("drops", null);
    }

    /**
     * Migrates all existing crop config files on disk to the new schema.
     * This rewrites each YAML and reloads definitions.
     */
    public void migrateAllConfigs() {
        for (CropConfigData data : cropData.values()) {
            YamlConfiguration cfg = data.getConfig();
            migrateLegacyConfig(cfg, data.getFile());
            try {
                cfg.save(data.getFile());
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to migrate config '" + data.getFile().getName() + "': " + e.getMessage());
            }
        }
        reloadCropConfigs();
        plugin.getLogger().info("All crop configs migrated to new schema.");
    }

    /**
     * Reloads all crop configs from disk, preserving internal UUID keys.
     */
    public void reloadCropConfigs() {
        usedUUIDs.clear();
        cropData.clear();
        File[] files = cropsFolder.listFiles(f -> f.getName().endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    loadSingleFile(file);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error reloading crop config '" + file.getName() + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("Reloaded " + cropData.size() + " crop configurations.");
    }

    /**
     * Renames the YAML file for the given internal UUID (doesn't change the ID).
     */
    public void renameCropConfig(String id, String newFileName) {
        CropConfigData data = getCropData(id);
        if (data == null) {
            plugin.getLogger().warning("No crop config for id: " + id);
            return;
        }
        File oldFile = data.getFile();
        File newFile = new File(cropsFolder, newFileName + ".yml");
        if (oldFile.renameTo(newFile)) {
            data.setFile(newFile);
            plugin.getLogger().info("Renamed crop file for id " + id + " to " + newFile.getName());
        } else {
            plugin.getLogger().severe("Failed to rename crop file for id: " + id);
        }
    }

    /**
     * Retrieves CropConfigData by its internal UUID.
     */
    public CropConfigData getCropData(String id) {
        return cropData.get(id.toLowerCase());
    }

    /**
     * Saves the config file for the given internal UUID.
     */
    public void saveCropConfig(String id) {
        CropConfigData data = getCropData(id);
        if (data == null) return;
        try {
            data.getConfig().save(data.getFile());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crop config for id: " + id);
        }
    }

    /**
     * Creates a new crop config file with defaults, preserving an immutable UUID.
     */
    public void createCropConfig(String baseName, YamlConfiguration defaultCfg) {
        File file = new File(cropsFolder, baseName.toLowerCase() + ".yml");
        if (defaultCfg == null) {
            defaultCfg = new YamlConfiguration();
            populateDefaultConfig(defaultCfg, baseName);
        }

        // now assign and save the id as before
        String id = UUID.randomUUID().toString();
        defaultCfg.set("id", id);
        try {
            defaultCfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create crop config file: " + file.getName());
            return;
        }
        CropConfigData data = new CropConfigData(file, defaultCfg);
        usedUUIDs.add(id.toLowerCase());
        cropData.put(id.toLowerCase(), data);
        CropDefinition def = CropDefinition.fromConfig(baseName, defaultCfg);
        CropDefinitionRegistry.update(id, def);
    }

    /**
     * Deletes the crop config file and unregisters its definition.
     */
    public void deleteCropConfig(String id) {
        CropConfigData data = getCropData(id);
        if (data == null) return;
        File file = data.getFile();
        if (file.delete()) {
            cropData.remove(id.toLowerCase());
            usedUUIDs.remove(id.toLowerCase());
            CropDefinitionRegistry.remove(id);
            plugin.getLogger().info("Deleted crop config and definition for id: " + id);
        } else {
            plugin.getLogger().severe("Failed to delete file for id: " + id);
        }
    }

    /**
     * Fills in all of the new-schema keys with sane defaults.
     */
    private void populateDefaultConfig(YamlConfiguration cfg, String baseName) {
        // Display & planting
        cfg.set("material", "WHEAT_SEEDS");
        cfg.set("display_name", "<yellow>" + baseName + " <gray>seeds");
        cfg.set("lore", Arrays.asList(
                "<gray>Place it on",
                "<dark_gray>farmland <gray>to plant it."
        ));
        cfg.set("settings.bone_meal", false);
        cfg.set("settings.final_block", "OAK_SAPLING");
        cfg.set("settings.auto_replant", false);
        cfg.set("settings.min_light_level", 8);
        cfg.set("settings.grow_time", 15);

        // Rewards
        List<Map<String, Object>> rewards = new ArrayList<>();

        Map<String, Object> seedR = new HashMap<>();
        seedR.put("type", "seed");
        seedR.put("chance", "100%");
        seedR.put("amount", "1-3");
        rewards.add(seedR);

        Map<String, Object> itemR = new HashMap<>();
        itemR.put("type", "item");
        itemR.put("chance", "100%");
        itemR.put("material", "APPLE");
        itemR.put("amount", "1-3");
        rewards.add(itemR);

        cfg.set("rewards", rewards);
    }

    /**
     * Gets an unmodifiable map of all crop configs keyed by internal UUID.
     */
    public Map<String, CropConfigData> getCropDataMap() {
        return Collections.unmodifiableMap(cropData);
    }
}
