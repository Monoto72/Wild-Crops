package me.monoto.customseeds.crops;

import me.monoto.customseeds.WildCrops;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.data.Ageable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CropGrowthScheduler {
    private final WildCrops plugin;
    private final Map<Location, BukkitTask> growthTasks = new ConcurrentHashMap<>();
    private static final double RANDOM_VARIATION = 0.15; // 15% variation

    public CropGrowthScheduler(WildCrops plugin) {
        this.plugin = plugin;
    }

    public void scheduleCropGrowth(Crop crop) {
        if (crop.getData().isFullyGrown()) return;

        Location loc = crop.getBlock().getLocation();
        if (growthTasks.containsKey(loc)) return;

        if (!(crop.getBlock().getBlockData() instanceof Ageable ageable)) {
            return;
        }

        int maxAge = ageable.getMaximumAge();
        long totalGrowTime = crop.getDefinition().getBaseGrowTime();
        long timePerStage = totalGrowTime / (maxAge + 1);

        long delay = calculateHybridDelay(timePerStage);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            growthTasks.remove(loc);

            if (!crop.getBlock().getType().isBlock() || crop.getBlock().getType().isAir()) {
                ChunkCropManager.removeCrop(crop.getBlock().getChunk(), crop.getBlock());
                return;
            }

            if (crop.meetsGrowthRequirements()) {
                if (crop.getBlock().getBlockData() instanceof Ageable currentAgeable) {
                    double progressPerStage = (double) totalGrowTime / (currentAgeable.getMaximumAge() + 1);
                    crop.updateGrowth(progressPerStage);
                }
            }

            if (!crop.getData().isFullyGrown()) {
                scheduleCropGrowth(crop);
            }
        }, delay);

        growthTasks.put(loc, task);
    }

    private long calculateHybridDelay(long averageTicks) {
        double randomFactor = 1.0 + (Math.random() * 2 - 1) * RANDOM_VARIATION; // 0.85 to 1.15
        return (long) (averageTicks * randomFactor);
    }

    public void cancelGrowth(Location loc) {
        BukkitTask task = growthTasks.remove(loc);
        if (task != null) task.cancel();
    }

    public void cancelAllInChunk(Chunk chunk) {
        growthTasks.keySet().removeIf(loc -> {
            if (loc.getChunk().equals(chunk)) {
                cancelGrowth(loc);
                return true;
            }
            return false;
        });
    }

    public WildCrops getPlugin() {
        return plugin;
    }
}
