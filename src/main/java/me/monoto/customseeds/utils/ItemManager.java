package me.monoto.customseeds.utils;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ItemManager {

    private static final NamespacedKey CUSTOM_CROP_KEY = new NamespacedKey(WildCrops.getInstance(), "CUSTOM_CROP");

    // Prevents instantiation.
    private ItemManager() {
    }

    /**
     * Creates a custom seed item with the provided crop type and amount.
     * It sets display name, lore, and custom NBT data from the crop configuration.
     *
     * @param type   the crop type identifier (e.g., "coal" or "Coal")
     * @param amount the number of seeds to create
     * @return the customized seed ItemStack
     */
    public static ItemStack getSeed(String type, int amount) {
        CropDefinition def = CropDefinitionRegistry.get(type);
        Material seedMat = (def != null ? def.getSeedMaterial() : Material.WHEAT_SEEDS);
        ItemStack seed = new ItemStack(seedMat, amount);

        ItemMeta meta = seed.getItemMeta();
        if (meta == null) {
            return seed;
        }

        String key = type.toLowerCase(Locale.ROOT);
        Map<String, CropConfigData> cropConfigs = WildCrops.getInstance().getFileManager().getCropDataMap();
        CropConfigData data = cropConfigs.get(key);
        if (data == null) {
            WildCrops.getInstance().getLogger().warning("No crop configuration found for type: " + type);
            return seed;
        }
        YamlConfiguration config = data.getConfig();

        String name = config.getString("display_name");
        if (name != null && !name.isEmpty()) {
            meta.displayName(Formatters.format(name));
        }

        List<String> loreList = config.getStringList("lore");
        if (!loreList.isEmpty()) {
            List<Component> seedLore = new ArrayList<>();
            for (String lore : loreList) {
                seedLore.add(Formatters.format(lore));
            }
            meta.lore(seedLore);
        }

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        if (!dataContainer.has(CUSTOM_CROP_KEY, PersistentDataType.STRING)) {
            String formattedType = key.substring(0, 1).toUpperCase(Locale.ROOT)
                    + key.substring(1).toLowerCase(Locale.ROOT);
            dataContainer.set(CUSTOM_CROP_KEY, PersistentDataType.STRING, formattedType);
        }

        seed.setItemMeta(meta);
        return seed;
    }

    /**
     * Checks if the given ItemStack is a custom crop seed by verifying its NBT data.
     *
     * @param item the item to check
     * @return true if the item contains the custom crop NBT, false otherwise
     */
    public static boolean isCustomCrop(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(CUSTOM_CROP_KEY, PersistentDataType.STRING);
    }

    /**
     * Retrieves the custom crop type stored in the item's NBT data.
     *
     * @param item the item from which to retrieve the crop type
     * @return the crop type as a String, or null if not present
     */
    public static String getCustomCropType(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(CUSTOM_CROP_KEY, PersistentDataType.STRING);
    }
}
