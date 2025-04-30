package me.monoto.customseeds.crops;

import me.monoto.customseeds.WildCrops;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import java.util.Iterator;

public class ChunkCropManager {

    public static final NamespacedKey CHUNK_CROPS_KEY = new NamespacedKey(WildCrops.getInstance(), "CHUNK_CROPS");

    public static void addCrop(Chunk chunk, Block block, String cropType) {
        int relX = block.getX() - (chunk.getX() * 16);
        int relY = block.getY();
        int relZ = block.getZ() - (chunk.getZ() * 16);
        String key = relX + "," + relY + "," + relZ;

        PersistentDataContainer container = chunk.getPersistentDataContainer();
        String dataStr = container.get(CHUNK_CROPS_KEY, PersistentDataType.STRING);
        Map<String, CropData> map = deserializeCropMap(dataStr);
        // Add new crop with initial progress 0.
        String normalizedCropType = cropType.toLowerCase();
        map.put(key, new CropData(0.0, 0, false, normalizedCropType));
        String serialized = serializeCropMap(map);
        container.set(CHUNK_CROPS_KEY, PersistentDataType.STRING, serialized);
    }

    public static void removeCrop(Chunk chunk, Block block) {
        int relX = block.getX() - (chunk.getX() * 16);
        int relY = block.getY();
        int relZ = block.getZ() - (chunk.getZ() * 16);
        String key = relX + "," + relY + "," + relZ;

        PersistentDataContainer container = chunk.getPersistentDataContainer();
        String dataStr = container.get(CHUNK_CROPS_KEY, PersistentDataType.STRING);
        Map<String, CropData> map = deserializeCropMap(dataStr);

        if (map.containsKey(key)) {

            map.remove(key);
            String serialized = serializeCropMap(map);
            container.set(CHUNK_CROPS_KEY, PersistentDataType.STRING, serialized);
        }
    }

    /**
     * Retrieves the crop mapping for a chunk.
     * Before returning, we prune any crop entry that is no longer a custom crop
     * (i.e. whose definition no longer exists in CropDefinitionRegistry).
     *
     * @param chunk The chunk to read from.
     * @return A map of relative coordinate keys to CropData.
     */
    public static Map<String, CropData> getCropsData(Chunk chunk) {
        // Prune crops that have become invalid before returning the data.
        pruneInvalidCrops(chunk);
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        String dataStr = container.get(CHUNK_CROPS_KEY, PersistentDataType.STRING);
        return deserializeCropMap(dataStr);
    }

    public static Block getBlockFromKey(Chunk chunk, String key) {
        String[] parts = key.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid crop key: " + key);
        }
        int relX = Integer.parseInt(parts[0].trim());
        int relY = Integer.parseInt(parts[1].trim());
        int relZ = Integer.parseInt(parts[2].trim());
        int absX = chunk.getX() * 16 + relX;
        int absZ = chunk.getZ() * 16 + relZ;
        return chunk.getWorld().getBlockAt(absX, relY, absZ);
    }

    public static String getRelativeKey(Block block) {
        int relX = block.getX() - (block.getChunk().getX() * 16);
        int relY = block.getY();
        int relZ = block.getZ() - (block.getChunk().getZ() * 16);
        return relX + "," + relY + "," + relZ;
    }

    /**
     * Iterates through the stored crop data for the given chunk and removes any entry
     * whose crop type is no longer a valid custom crop.
     *
     * @param chunk The chunk whose crop data should be validated.
     */
    public static void pruneInvalidCrops(Chunk chunk) {
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        String dataStr = container.get(CHUNK_CROPS_KEY, PersistentDataType.STRING);
        Map<String, CropData> map = deserializeCropMap(dataStr);
        boolean modified = false;

        Iterator<Map.Entry<String, CropData>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CropData> entry = iterator.next();

            String storedType = entry.getValue().getCropType().trim().toLowerCase();
            if (CropDefinitionRegistry.get(storedType) == null) {
                WildCrops.getInstance().getLogger().info("Pruning non-custom crop: " + storedType + " at key: " + entry.getKey());
                iterator.remove();
                modified = true;
            }
        }

        if (modified) {
            String serialized = serializeCropMap(map);
            container.set(CHUNK_CROPS_KEY, PersistentDataType.STRING, serialized);

            WildCrops.getInstance().getLogger().info("Updated serialized crop map: " + serialized);
        }
    }

    /**
     * Serializes a map of crop data into a string.
     * Format for each entry: key;cropType;progress;age;fullyGrown
     * Entries are separated by a pipe (|) character.
     *
     * @param map The map to serialize.
     * @return The serialized string.
     */
    public static String serializeCropMap(Map<String, CropData> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, CropData> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            CropData data = entry.getValue();
            sb.append(entry.getKey()).append(";")
                    .append(data.getCropType()).append(";")
                    .append(data.getProgress()).append(";")
                    .append(data.getAge()).append(";")
                    .append(data.isFullyGrown());
        }
        return sb.toString();
    }

    /**
     * Deserializes a string into a map of crop data.
     *
     * @param dataStr The serialized string.
     * @return A map of crop data.
     */
    public static Map<String, CropData> deserializeCropMap(String dataStr) {
        Map<String, CropData> map = new HashMap<>();
        if (dataStr == null || dataStr.isEmpty()) return map;

        String[] entries = dataStr.split("\\|");
        for (String entry : entries) {
            String[] parts = entry.split(";");
            if (parts.length != 5) {
                WildCrops.getInstance().getLogger().warning("Malformed crop entry: " + entry);
                continue;
            }

            String key = parts[0].trim();
            String cropType = parts[1].trim();
            double progress;
            int age;
            boolean fullyGrown;
            try {
                progress = Double.parseDouble(parts[2].trim());
                age = Integer.parseInt(parts[3].trim());
                fullyGrown = Boolean.parseBoolean(parts[4].trim());
            } catch (NumberFormatException e) {
                WildCrops.getInstance().getLogger().warning("Invalid number format in entry: " + entry);
                continue;
            }
            if (key.chars().filter(ch -> ch == ',').count() < 2) {
                WildCrops.getInstance().getLogger().warning("Ignoring invalid key: " + key);
                continue;
            }
            map.put(key, new CropData(progress, age, fullyGrown, cropType));
        }
        return map;
    }

    /**
     * Dumps all chunk crop data to a file for debugging purposes.
     * Temporarily used for debugging.
     */
    public static void dumpAllChunkCropsToFile() {
        File dumpFile = new File(WildCrops.getInstance().getDataFolder(), "chunk_crop_data_dump.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(dumpFile))) {
            writer.println("=== Chunk Crop Data Dump ===\n");

            for (World world : WildCrops.getInstance().getServer().getWorlds()) {
                writer.println("World: " + world.getName());

                for (Chunk chunk : world.getLoadedChunks()) {
                    PersistentDataContainer container = chunk.getPersistentDataContainer();
                    String dataStr = container.get(CHUNK_CROPS_KEY, PersistentDataType.STRING);

                    // Only process if there is crop data
                    if (dataStr != null && !dataStr.isEmpty()) {
                        writer.println("Chunk at (" + chunk.getX() + ", " + chunk.getZ() + ")");
                        writer.println("Serialized Data: " + dataStr);

                        Map<String, CropData> cropMap = deserializeCropMap(dataStr);
                        if (!cropMap.isEmpty()) {
                            writer.println("Deserialized Entries:");
                            for (Map.Entry<String, CropData> entry : cropMap.entrySet()) {
                                writer.println("   Key: " + entry.getKey() + "  =>  " + entry.getValue().toString());
                            }
                        }
                        writer.println("----------------------------------------");
                    }
                }
                writer.println();
            }

            writer.flush();
            WildCrops.getInstance().getLogger().info("Chunk crop data dump written to " + dumpFile.getAbsolutePath());
        } catch (IOException ex) {
            WildCrops.getInstance().getLogger().severe("Error writing chunk crop data dump: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
