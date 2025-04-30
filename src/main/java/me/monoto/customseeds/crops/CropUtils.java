package me.monoto.customseeds.crops;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
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
     * @param block        The block that was broken.
     * @param data         The dynamic state of the crop.
     * @param forcedBreak  Whether the break was forced (e.g. support lost).
     * @param player       The player who broke the block, or null if none.
     */
    public static void processCropDrops(Block block, CropData data, boolean forcedBreak, Player player) {
        CropDefinition def = CropDefinitionRegistry.get(data.getCropType());
        if (def == null) return;

        Location loc = block.getLocation();

        // If forced or not fully grown, drop exactly one seed from definition
        if (forcedBreak || !data.isFullyGrown()) {
            ItemStack seedDrop = new ItemStack(def.getSeedMaterial(), 1);
            block.getWorld().dropItemNaturally(loc, seedDrop);
        } else {
            // Iterate all configured rewards
            for (CropDefinition.Reward reward : def.getRewards()) {
                // roll chance
                if (ThreadLocalRandom.current().nextDouble() > reward.chance()) {
                    continue;
                }

                String type = reward.type();
                Range<Integer> range = reward.amount();
                int amount = range != null
                        ? ThreadLocalRandom.current().nextInt(range.getMinimum(), range.getMaximum() + 1)
                        : 0;

                switch (type) {
                    case "seed": {
                        ItemStack seeds = ItemManager.getSeed(type, amount);
                        block.getWorld().dropItemNaturally(loc, seeds);
                        break;
                    }
                    case "item": {
                        Material mat = reward.material();
                        ItemStack drop = new ItemStack(mat, amount);
                        block.getWorld().dropItemNaturally(loc, drop);
                        break;
                    }
                    case "exp": {
                        ExperienceOrb orb = block.getWorld().spawn(loc, ExperienceOrb.class);
                        orb.setExperience(amount);
                        break;
                    }
                    case "mcmmo_exp": {
                        if (player != null) {
                            if (DependencyManager.isMcMMOEnabled()) {
                                try {
                                    ExperienceAPI.addLevel(player, "Herbalism", amount);
                                } catch (NoClassDefFoundError e) {
                                    WildCrops.getInstance().getLogger().warning("McMMO API missing — skipping mcmmo_exp reward");
                                }
                            } else {
                                WildCrops.getInstance().getLogger().warning("McMMO not available — skipping mcmmo_exp reward");
                            }
                        }
                        break;
                    }
                    case "money": {
                        if (player != null) {
                            try {
                                if (DependencyManager.getEcon() != null) {
                                    DependencyManager.getEcon().depositPlayer(player, amount);
                                } else {
                                    WildCrops.getInstance().getLogger().warning("Vault economy not available — skipping money reward");
                                }
                            } catch (NoClassDefFoundError e) {
                                WildCrops.getInstance().getLogger().warning("Vault API missing — skipping money reward");
                            }
                        }
                        break;
                    }
                    case "command": {
                        if (player != null && reward.commands() != null) {
                            for (String cmd : reward.commands()) {
                                String toRun = cmd.replace("{player}", player.getName());
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toRun);
                            }
                        }
                        break;
                    }
                    default:
                        Bukkit.getLogger().warning("Unknown reward type: " + type);
                }
            }
        }

        block.setType(Material.AIR);
    }

    /**
     * Overload for non-player breaks (e.g. piston, water)
     */
    public static void processCropDrops(Block block, CropData data, boolean forcedBreak) {
        processCropDrops(block, data, forcedBreak, null);
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
