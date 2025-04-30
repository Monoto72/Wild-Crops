package me.monoto.customseeds.listeners;

import me.monoto.customseeds.crops.*;
import me.monoto.customseeds.utils.ItemManager;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class CropPlaceListener implements Listener {
    private final CropGrowthScheduler scheduler;

    public CropPlaceListener(CropGrowthScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!ItemManager.isCustomCrop(item)) return;

        String type = ItemManager.getCustomCropType(item);
        if (type == null) return;

        Block block = event.getBlock();
        if (CropUtils.isCustomCrop(block)) {
            event.setCancelled(true);
            return;
        }

        Chunk chunk = block.getChunk();
        ChunkCropManager.addCrop(chunk, block, type);

        CropData data = ChunkCropManager.getCropsData(chunk)
                .get(ChunkCropManager.getRelativeKey(block));
        if (data == null) return;

        Crop crop = Crop.fromBlock(block, data);
        if (crop != null) {
            scheduler.scheduleCropGrowth(crop);
        }
    }
}

