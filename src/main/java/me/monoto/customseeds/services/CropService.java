package me.monoto.customseeds.services;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.*;
import me.monoto.customseeds.utils.BlockCache;
import me.monoto.customseeds.utils.DependencyManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class CropService implements Listener {
    private final boolean hasSkyblock = DependencyManager.isSkyblockEnabled();
    private final CropGrowthScheduler scheduler;
    private final Random random = new Random();

    public CropService(CropGrowthScheduler scheduler) {
        this.scheduler = scheduler;
    }

    private boolean canBreak(Player player, Block block) {
        if (player == null) {
            return true;
        }
        if (!hasSkyblock) {
            return true;
        }

        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player);
        Island island = SuperiorSkyblockAPI.getIslandAt(block.getLocation());

        if (island == null) {
            return true;
        }

        return island.hasPermission(sp, IslandPrivilege.getByName("Break"));
    }

    private boolean isTrackedCrop(Block b) {
        return CropUtils.getCropData(b) != null;
    }

    /**
     * Core break handler, accepts an optional player context.
     */
    public void handleBreak(Block cropBlock, boolean forced, Player player) {
        scheduler.cancelGrowth(cropBlock.getLocation());

        CropData data = CropUtils.getCropData(cropBlock);
        if (data == null) return;

        CropDefinition def = CropDefinitionRegistry.get(data.getCropType());
        Optional<CropDefinition.Reward> seedReward = def.getReward("seed");
        boolean hasSeedReward = false;

        if (seedReward.isPresent()) {
            int min = seedReward.get().amount().getMinimum();
            int max = seedReward.get().amount().getMaximum();
            if (max > 0) {
                hasSeedReward = true;
            }
        }

        boolean autoReplantAllowed = def.isAutoReplantAllowed();
        boolean autoReplantPermissible = (player != null && player.hasPermission("wildcrops.autoreplant"));
        boolean sneakBreak = (player != null && player.isSneaking());
        boolean willAutoReplant = autoReplantAllowed
                && autoReplantPermissible
                && !forced
                && !sneakBreak
                && hasSeedReward;

        CropUtils.removeCrop(cropBlock);
        CropUtils.processCropDrops(cropBlock, data, forced, player, willAutoReplant);

        if (willAutoReplant) {
            Chunk originalChunk = cropBlock.getRelative(BlockFace.DOWN).getChunk();
            Location targetLoc = cropBlock.getLocation();
            Bukkit.getScheduler().runTaskLater(WildCrops.getInstance(), () -> {
                Block belowNow = targetLoc.getBlock().getRelative(BlockFace.DOWN);
                if (belowNow.getType() == Material.FARMLAND) {
                    targetLoc.getBlock().setType(BlockCache.resolve(def.getSeedMaterial()));
                    ChunkCropManager.addCrop(originalChunk, targetLoc.getBlock(), data.getCropType());

                    CropData nData = ChunkCropManager.getCropsData(originalChunk)
                            .get(ChunkCropManager.getRelativeKey(targetLoc.getBlock()));
                    if (nData == null) return;

                    Crop crop = Crop.fromBlock(targetLoc.getBlock(), nData);
                    if (crop != null) {
                        scheduler.scheduleCropGrowth(crop);
                    }
                }
            }, 5);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player player = e.getPlayer();

        // Breaking farmland under a crop
        if (b.getType() == Material.FARMLAND) {
            Block crop = b.getRelative(BlockFace.UP);
            if (isTrackedCrop(crop) && canBreak(player, crop)) {
                e.setDropItems(false);
                handleBreak(crop, false, player);
            }
            return;
        }

        if (isTrackedCrop(b) && canBreak(player, b)) {
            e.setDropItems(false);
            handleBreak(b, false, player);
        }
    }

    @EventHandler
    public void onBlockForcedBreak(BlockBreakBlockEvent e) {
        Block b = e.getBlock();

        // Piston or block breaking farmland
        if (b.getType() == Material.FARMLAND) {
            Block crop = b.getRelative(BlockFace.UP);
            if (isTrackedCrop(crop)) {
                CropUtils.processCropDrops(crop, CropUtils.getCropData(crop), true);
                CropUtils.removeCrop(crop);
            }
            return;
        }

        if (isTrackedCrop(b)) {
            e.getDrops().clear();
            CropUtils.processCropDrops(b, CropUtils.getCropData(b), true);
            CropUtils.removeCrop(b);
        }
    }

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent e) {
        Block b = e.getBlock();
        if (!isTrackedCrop(b)) return;

        e.getItems().clear();
        e.setCancelled(true);
        handleBreak(b, true, null);
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent e) {
        Iterator<Block> it = e.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            if (isTrackedCrop(b)) {
                it.remove();
                handleBreak(b, true, null);
            }
        }
    }

    private void processPistonBlocks(Iterable<Block> blocks) {
        for (Block b : blocks) {
            if (b.getType() != Material.FARMLAND) continue;
            Block crop = b.getRelative(BlockFace.UP);
            if (!isTrackedCrop(crop)) continue;
            handleBreak(crop, true, null);
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        processPistonBlocks(e.getBlocks());
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        processPistonBlocks(e.getBlocks());
    }

    @EventHandler
    public void onBoneMealInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = e.getClickedBlock();
        Player player = e.getPlayer();

        if (clicked == null || !isTrackedCrop(clicked)) return;
        e.setCancelled(true);

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.BONE_MEAL && handItem.getAmount() > 0) {
            handItem.setAmount(handItem.getAmount() - 1);
            player.getInventory().setItemInMainHand(handItem);
        } else {
            return;
        }

        applyBoneMealEffect(clicked);
    }

    @EventHandler
    public void onBoneMealDispense(BlockDispenseEvent e) {
        Block source = e.getBlock();
        BlockData bd = source.getBlockData();
        if (!(bd instanceof Directional dir)) return;

        Block target = source.getRelative(dir.getFacing());
        if (!isTrackedCrop(target)) return;

        e.setCancelled(true);

        BlockState state = source.getState();
        if (state instanceof Dispenser dispenser) {
            Map<Integer, ItemStack> leftovers = dispenser.getInventory()
                    .removeItem(new ItemStack(Material.BONE_MEAL, 1));

            if (!leftovers.isEmpty()) {
                dispenser.update();
                return;
            }

            dispenser.update();
            applyBoneMealEffect(target);
        }
    }

    private void applyBoneMealEffect(Block cropBlock) {
        CropData data = CropUtils.getCropData(cropBlock);
        if (data == null || data.isFullyGrown()) return;

        Crop crop = Crop.fromBlock(cropBlock, data);
        if (crop == null) return;

        if (!crop.meetsGrowthRequirements()) return;

        Location loc = cropBlock.getLocation();
        scheduler.cancelGrowth(loc);

        BlockData bd = cropBlock.getBlockData();
        if (!(bd instanceof Ageable ageable)) return;

        int oldAge = ageable.getAge();
        int maxAge = ageable.getMaximumAge();
        if (oldAge >= maxAge) return;

        int bump = 1 + random.nextInt(2);

        data.setProgress(0.0);

        double progressPerStage = (double) crop.definition().getBaseGrowTime() / (maxAge + 1);
        double totalTicksToAdd = bump * progressPerStage;

        crop.updateGrowth(totalTicksToAdd);

        if (!data.isFullyGrown()) {
            scheduler.scheduleCropGrowth(crop);
        }
    }

    @EventHandler
    public void onLiquidBucketDispense(BlockDispenseEvent e) {
        Block source = e.getBlock();
        BlockData bd = source.getBlockData();
        if (!(bd instanceof Directional dir)) return;

        Block target = source.getRelative(dir.getFacing());
        if (!isTrackedCrop(target)) return;
        
        handleBreak(target, true, null);
    }

    @EventHandler
    public void onGrow(BlockGrowEvent e) {
        if (isTrackedCrop(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent e) {
        if (isTrackedCrop(e.getLocation().getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent e) {
        if (isTrackedCrop(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (isTrackedCrop(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPhysicalInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.PHYSICAL) return;

        Block below = e.getClickedBlock();
        if (below == null || below.getType() != Material.FARMLAND) return;

        Block crop = below.getRelative(BlockFace.UP);
        Player player = e.getPlayer();
        if (!isTrackedCrop(crop) || !canBreak(player, crop)) return;

        crop.getDrops().clear();
        crop.setType(Material.AIR);

        boolean forced = !below.getRelative(BlockFace.DOWN).getType().equals(Material.FARMLAND);
        handleBreak(crop, forced, player);
    }
}
