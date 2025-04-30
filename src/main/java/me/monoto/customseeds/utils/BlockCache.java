package me.monoto.customseeds.utils;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockCache {

    private static final List<ItemStack> PLACEABLE_BLOCKS = new ArrayList<>();

    private static final Set<Material> DISALLOWED = Set.of(
            Material.BEDROCK,
            Material.BARRIER,
            Material.COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.JIGSAW,
            Material.DEBUG_STICK,
            Material.AIR,
            Material.VOID_AIR,
            Material.CAVE_AIR
    );

    private static final List<Tag<Material>> ALLOWED_TAGS = List.of(
            Tag.CROPS,
            Tag.SAPLINGS,
            Tag.LOGS,
            Tag.LEAVES,
            Tag.DIRT,
            Tag.FLOWERS,
            Tag.WOOL
    );

    // Optional mapping for block-only materials -> item equivalents
    private static final Map<Material, Material> BLOCK_TO_ITEM_OVERRIDES = Map.of(
            Material.BEETROOTS, Material.BEETROOT_SEEDS,
            Material.CARROTS, Material.CARROT,
            Material.POTATOES, Material.POTATO,
            Material.WHEAT, Material.WHEAT_SEEDS,
            Material.NETHER_WART, Material.NETHER_WART,
            Material.COCOA, Material.COCOA_BEANS
    );

    public static void load() {
        PLACEABLE_BLOCKS.clear();

        // Gather all from tags
        Set<Material> tagged = ALLOWED_TAGS.stream()
                .flatMap(tag -> tag.getValues().stream())
                .collect(Collectors.toSet());

        // Add all solid block items
        Stream<Material> baseBlocks = Arrays.stream(Material.values())
                .filter(Material::isBlock)
                .filter(mat -> !mat.name().startsWith("LEGACY_"))
                .filter(mat -> !DISALLOWED.contains(mat));

        // Combine both
        Stream<Material> all = Stream.concat(baseBlocks, tagged.stream()).distinct();

        // Convert to valid ItemStacks
        all.map(BlockCache::resolveItemMaterial)
                .filter(Objects::nonNull)
                .map(ItemStack::new)
                .sorted(Comparator.comparing(item -> item.getType().name()))
                .forEach(PLACEABLE_BLOCKS::add);
    }

    private static Material resolveItemMaterial(Material blockMat) {
        if (blockMat.isItem()) return blockMat;
        return BLOCK_TO_ITEM_OVERRIDES.getOrDefault(blockMat, null);
    }

    public static List<ItemStack> getPlaceableBlocks() {
        return PLACEABLE_BLOCKS;
    }
}
