package me.monoto.customseeds.crops;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;

public record Crop(CropDefinition definition, Block block, CropData data) {
    public Crop(CropDefinition definition, Block block, CropData data) {
        this.definition = definition;
        this.block = block;
        this.data = data;
        if (block.getBlockData() instanceof Ageable) {
            data.setAge(((Ageable) block.getBlockData()).getAge());
        } else {
            data.setAge(0);
        }
    }

    /**
     * Factory method to create a Crop instance from a block using chunk data.
     * Returns null if the CropData is missing or no CropDefinition is found.
     */
    public static Crop fromBlock(Block block, CropData data) {
        if (data == null || data.getCropType() == null) {
            return null;
        }
        CropDefinition def = CropDefinitionRegistry.get(data.getCropType());
        if (def == null) {
            return null;
        }
        return new Crop(def, block, data);
    }

    /**
     * Updates the crop's growth state if all growth conditions are met.
     * The crop's dynamic state (progress, age, fully grown flag) is updated accordingly.
     *
     * @param tickMultiplier The multiplier for tick progress.
     */
    public void updateGrowth(double tickMultiplier) {
        if (!block.getChunk().isLoaded() || !(block.getBlockData() instanceof Ageable ageable)) {
            return;
        }

        if (data.isFullyGrown()) {
            return;
        }

        int maxAge = ageable.getMaximumAge();
        double progressPerStage = (double) definition.getBaseGrowTime() / (maxAge + 1);

        // Increase progress
        data.setProgress(data.getProgress() + tickMultiplier);

        while (data.getProgress() >= progressPerStage && !data.isFullyGrown()) {
            data.setProgress(data.getProgress() - progressPerStage);
            int nextAge = data.getAge() + 1;

            if (nextAge < maxAge) {
                data.setAge(nextAge);
                ageable.setAge(nextAge);
                block.setBlockData(ageable);
                ChunkCropManager.updateCropData(block, data);
            } else {
                block.setType(definition.getFinalBlock());

                BlockData newData = block.getBlockData();
                if (newData instanceof Ageable finalAgeable) {
                    finalAgeable.setAge(finalAgeable.getMaximumAge());
                    block.setBlockData(finalAgeable, false);
                }

                data.setAge(maxAge);
                data.setFullyGrown(true);
                ChunkCropManager.updateCropData(block, data);
            }
        }
    }

    public boolean meetsGrowthRequirements() {
        if (!(block.getBlockData() instanceof Ageable)) return false;

        Material below = block.getRelative(BlockFace.DOWN).getType();
        Material cropType = block.getType();
        boolean soilOk;

        if (cropType == Material.NETHER_WART) {
            soilOk = (below == Material.SOUL_SAND);

        } else if (cropType == Material.COCOA) {
            if (!(block.getBlockData() instanceof Directional dir)) return false;
            BlockFace attachFace = dir.getFacing();
            soilOk = (block.getRelative(attachFace).getType() == Material.JUNGLE_LOG);

        } else {
            soilOk = (below == Material.FARMLAND);
        }

        if (!soilOk) return false;

        return block.getLightLevel() >= definition.getMinLight();
    }
}