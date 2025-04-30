package me.monoto.customseeds.crops;

import me.monoto.customseeds.WildCrops;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all CropDefinitions, keyed by internal UUID.
 * Must be initialized in plugin onEnable to avoid NPEs.
 */
public class CropDefinitionRegistry {
    private static final Map<String, CropDefinition> definitions = new HashMap<>();
    private static WildCrops plugin;

    /**
     * Must be called once in WildCrops.onEnable(), passing the plugin instance.
     */
    public static void init(WildCrops instance) {
        plugin = instance;
    }

    /**
     * Loads crop definitions from a map of CropConfigData.
     * @param cropData Map of internal UUID to CropConfigData.
     */
    public static void load(Map<String, CropConfigData> cropData) {
        definitions.clear();
        for (Map.Entry<String, CropConfigData> entry : cropData.entrySet()) {
            String id = entry.getKey();
            YamlConfiguration cfg = entry.getValue().getConfig();
            CropDefinition def = CropDefinition.fromConfig(entry.getValue().getFileNameWithoutExtension(), cfg);
            definitions.put(id.toLowerCase(), def);
        }
        if (plugin != null) {
            plugin.getLogger().info("Loaded " + definitions.size() + " crop definitions");
        }
    }

    /**
     * Register or update a single CropDefinition under the given id.
     */
    public static void update(String id, CropDefinition def) {
        definitions.put(id.toLowerCase(), def);
        if (plugin != null) {
            plugin.getLogger().info("Updated crop definition: " + id);
        }
    }

    /**
     * Remove a definition when deleting a crop.
     */
    public static void remove(String id) {
        definitions.remove(id.toLowerCase());
        if (plugin != null) {
            plugin.getLogger().info("Removed crop definition: " + id);
        }
    }

    /**
     * Retrieve a definition by internal UUID.
     */
    public static CropDefinition get(String id) {
        return definitions.get(id.toLowerCase());
    }

    /**
     * Get all definitions (unmodifiable).
     */
    public static Map<String, CropDefinition> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }
}
