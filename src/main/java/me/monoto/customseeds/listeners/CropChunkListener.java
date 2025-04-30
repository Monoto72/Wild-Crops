package me.monoto.customseeds.listeners;

import me.monoto.customseeds.crops.ChunkCropManager;
import me.monoto.customseeds.crops.Crop;
import me.monoto.customseeds.crops.CropData;
import me.monoto.customseeds.crops.CropGrowthScheduler;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Map;

public class CropChunkListener implements Listener {
    private final CropGrowthScheduler scheduler;

    public CropChunkListener(CropGrowthScheduler scheduler) {
        this.scheduler = scheduler;

        for (var world : scheduler.getPlugin().getServer().getWorlds()) {
            for (Chunk c : world.getLoadedChunks()) {
                loadChunkCrops(c);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        loadChunkCrops(e.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        scheduler.cancelAllInChunk(e.getChunk());
    }

    private void loadChunkCrops(Chunk chunk) {
        Map<String, CropData> crops = ChunkCropManager.getCropsData(chunk);
        crops.forEach((key, data) -> {
            Block block = ChunkCropManager.getBlockFromKey(chunk, key);
            Crop crop = Crop.fromBlock(block, data);
            if (crop != null && !crop.getData().isFullyGrown()) {
                scheduler.scheduleCropGrowth(crop);
            }
        });
    }
}
