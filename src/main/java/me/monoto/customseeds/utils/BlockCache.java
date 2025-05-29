package me.monoto.customseeds.utils;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockCache {
    /*
     * This class is used to cache all the blocks that can be placed in the world.
     * Might need to be updated if new blocks are added in the future.
     * Currently a hacky solution, but it works. Though holds the main thread for a bit.
     */

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> new Thread(r, "BlockCache"));

    private static final Set<Material> DISALLOWED_BLOCKS = Set.of(
            Material.BEDROCK, Material.BARRIER, Material.COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK, Material.STRUCTURE_VOID, Material.JIGSAW,
            Material.DEBUG_STICK, Material.AIR, Material.VOID_AIR, Material.CAVE_AIR
    );

    private static final List<Material> ALLOWED_CROPS = List.of(
            Material.BEETROOT_SEEDS, Material.CARROT, Material.POTATO,
            Material.WHEAT_SEEDS, Material.NETHER_WART, Material.COCOA_BEANS
    );


    private static final CompletableFuture<Cache> CACHE_FUTURE =
            CompletableFuture.supplyAsync(BlockCache::computeAll, EXEC);

    // simple data holder
    public record Cache(
            List<Material> placeables,
            List<Material> drops,
            List<Material> crops
    ) {
    }

    public static CompletableFuture<Cache> initAsync() {
        return CACHE_FUTURE;
    }

    public static Cache getCache() {
        return CACHE_FUTURE.join();
    }

    public static List<Material> getPlaceableItems() {
        return getCache().placeables();
    }

    public static List<Material> getAllDrops() {
        return getCache().drops();
    }

    public static List<Material> getCrops() {
        return getCache().crops();
    }

    private static Cache computeAll() {
        Set<Material> placeSet = new HashSet<>();
        Set<Material> dropSet = new HashSet<>();

        Set<Material> tagged = new HashSet<>();
        Tag<Material>[] tags = new Tag[]{
                Tag.CROPS, Tag.SAPLINGS, Tag.LOGS,
                Tag.LEAVES, Tag.DIRT, Tag.FLOWERS, Tag.WOOL
        };
        for (Tag<Material> tag : tags) {
            for (Material m : tag.getValues()) {
                tagged.add(m);
            }
        }

        for (Material m : Material.values()) {
            if (m.isItem() && m != Material.AIR && m != Material.VOID_AIR) {
                dropSet.add(m);
            }

            if (tagged.contains(m) || (m.isBlock() && !DISALLOWED_BLOCKS.contains(m))) {
                Material safe = resolve(m);
                if (safe != null) {
                    placeSet.add(safe);
                }
            }
        }

        Comparator<Material> byName = Comparator.comparing(Enum::name);
        List<Material> placeables = new ArrayList<>(placeSet);
        placeables.sort(byName);

        List<Material> drops = new ArrayList<>(dropSet);
        drops.sort(byName);

        List<Material> crops = new ArrayList<>(ALLOWED_CROPS);
        crops.sort(byName);

        return new Cache(placeables, drops, crops);
    }


    private static final Map<Material, Material> BI_MAP;

    static {
        Map<Material, Material> m = new EnumMap<>(Material.class);
        m.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        m.put(Material.BEETROOT_SEEDS, Material.BEETROOTS);
        m.put(Material.CARROTS, Material.CARROT);
        m.put(Material.CARROT, Material.CARROTS);
        m.put(Material.POTATOES, Material.POTATO);
        m.put(Material.POTATO, Material.POTATOES);
        m.put(Material.WHEAT, Material.WHEAT_SEEDS);
        m.put(Material.WHEAT_SEEDS, Material.WHEAT);
        m.put(Material.COCOA, Material.COCOA_BEANS);
        m.put(Material.COCOA_BEANS, Material.COCOA);
        m.put(Material.NETHER_WART, Material.NETHER_WART);

        BI_MAP = Collections.unmodifiableMap(m);
    }

    public static Material resolve(Material mat) {
        return BI_MAP.getOrDefault(mat, mat);
    }


    /**
     * Call this from onDisable() so the executor cleanly shuts down.
     */
    public static void shutdown() {
        EXEC.shutdownNow();
    }
}