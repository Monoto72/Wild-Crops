package me.monoto.customseeds.utils;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockCache {
    /*
     * This class is used to cache all the blocks that can be placed in the world.
     * Might need to be updated if new blocks are added in the future.
     * Currently a hacky solution, but it works. Though holds the main thread for a bit.
     */

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

    private static List<Material> placeableItems = List.of();

    public static void load() {
        Set<Material> result = new HashSet<>();

        List<Tag<Material>> allowedTags = List.of(
                Tag.CROPS, Tag.SAPLINGS, Tag.LOGS, Tag.LEAVES,
                Tag.DIRT, Tag.FLOWERS, Tag.WOOL
        );
        allowedTags.forEach(tag -> result.addAll(tag.getValues()));

        for (Material mat : Material.values()) {
            if (!mat.isBlock() || mat.name().startsWith("LEGACY_") || DISALLOWED_BLOCKS.contains(mat)) continue;
            Material safeItem = resolveItemEquivalent(mat);
            if (safeItem != null) result.add(safeItem);
        }

        placeableItems = result.stream()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
    }

    private static Material resolveItemEquivalent(Material mat) {
        return switch (mat) {
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case WHEAT -> Material.WHEAT_SEEDS;
            case COCOA -> Material.COCOA_BEANS;
            case NETHER_WART -> Material.NETHER_WART;
            default -> {
                try {
                    yield ItemStack.of(mat).getType();
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }

    public static List<Material> getPlaceableItems() {
        return placeableItems;
    }

    public static List<Material> getCrops() {
        return ALLOWED_CROPS;
    }
}
