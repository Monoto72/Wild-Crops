package me.monoto.customseeds.crops;

import com.gmail.nossr50.api.ExperienceAPI;
import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.utils.DependencyManager;
import me.monoto.customseeds.utils.ItemManager;
import org.apache.commons.lang3.Range;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility for handling crop harvesting and rewards.
 */
public class CropUtils {

    /**
     * Processes custom crop drops for the given block when broken by a player.
     *
     * @param block       The block that was broken.
     * @param data        The dynamic state of the crop.
     * @param forcedBreak Whether the break was forced (e.g. support lost).
     * @param player      The player who broke the block, or null if none.
     */
    public static void processCropDrops(Block block, CropData data, boolean forcedBreak, Player player, boolean subtractReplant) {
        CropDefinition def = CropDefinitionRegistry.get(data.getCropType());
        if (def == null) return;
        Location loc = block.getLocation().toCenterLocation();

        if (forcedBreak || !data.isFullyGrown()) {
            if (!subtractReplant) {
                ItemStack seedR = ItemManager.getSeed(data.getCropType(), 1);
                block.getWorld().dropItemNaturally(loc, seedR);
            }
        } else {
            for (CropDefinition.Reward reward : def.getRewards()) {
                double roll = ThreadLocalRandom.current().nextDouble();
                if (roll > reward.chance()) continue;

                String type = reward.type();
                Range<Integer> range = reward.amount();
                int amount = range != null
                        ? ThreadLocalRandom.current().nextInt(range.getMinimum(), range.getMaximum() + 1)
                        : 0;

                if (amount == 0) continue;

                switch (type) {
                    case "seed": {
                        int dropCount = subtractReplant ? Math.max(amount - 1, 0) : amount;
                        if (dropCount > 0) {
                            ItemStack seedR = ItemManager.getSeed(data.getCropType(), dropCount);
                            block.getWorld().dropItemNaturally(loc, seedR);
                        }
                    }
                    case "item": {
                        ItemStack itemR = new ItemStack(reward.material(), amount);
                        block.getWorld().dropItemNaturally(loc, itemR);
                        break;
                    }
                    case "exp": {
                        block.getWorld().spawn(loc, ExperienceOrb.class, experienceOrb -> experienceOrb.setExperience(amount));
                        break;
                    }
                    case "mcmmo_exp": {
                        if (DependencyManager.isMcMMOEnabled()) {
                            try {
                                ExperienceAPI.addLevel(player, "Herbalism", amount);
                            } catch (NoClassDefFoundError e) {
                                WildCrops.getInstance().getLogger().warning("McMMO API missing — skipping mcmmo_exp reward");
                            }
                        } else {
                            WildCrops.getInstance().getLogger().warning("McMMO not available — skipping mcmmo_exp reward");
                        }
                        break;
                    }
                    case "money": {
                        try {
                            if (DependencyManager.getEcon() != null) {
                                DependencyManager.getEcon().depositPlayer(player, amount);
                            } else {
                                WildCrops.getInstance().getLogger().warning("Vault economy not available — skipping money reward");
                            }
                        } catch (NoClassDefFoundError e) {
                            WildCrops.getInstance().getLogger().warning("Vault API missing — skipping money reward");
                        }
                        break;
                    }
                    case "command": {
                        if (reward.commands() != null) {
                            for (String cmd : reward.commands()) {
                                String toRun = cmd.replace("{player}", player.getName());
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toRun);
                            }
                        }
                        break;
                    }
                    default:
                        WildCrops.getInstance().getLogger().warning("Unknown crop type: " + type);
                }
            }
        }

        block.setType(Material.AIR);
    }

    /**
     * Overload for non-player breaks (e.g. piston, water)
     */
    public static void processCropDrops(Block block, CropData data, boolean forcedBreak) {
        processCropDrops(block, data, forcedBreak, null, false);
    }

    /**
     * Returns the CropData for the given block if it is a custom crop.
     *
     * @param block the block to check.
     * @return the CropData or null if not present.
     */
    public static CropData getCropData(Block block) {
        String key = getRelativeKey(block, block.getChunk());
        return ChunkCropManager.getCropsData(block.getChunk()).get(key);
    }

    /**
     * Removes the crop entry for the given block from the chunk data.
     *
     * @param block the block representing the crop.
     */
    public static void removeCrop(Block block) {
        ChunkCropManager.removeCrop(block.getChunk(), block);
    }

    /**
     * Returns the relative key for a block within its chunk in the format "x,y,z".
     */
    public static String getRelativeKey(Block block, org.bukkit.Chunk chunk) {
        int relX = block.getX() - (chunk.getX() * 16);
        int relY = block.getY();
        int relZ = block.getZ() - (chunk.getZ() * 16);
        return relX + "," + relY + "," + relZ;
    }

    /**
     * Checks if a block is a custom crop by verifying that CropData exists for it.
     */
    public static boolean isCustomCrop(Block block) {
        return getCropData(block) != null;
    }
}
