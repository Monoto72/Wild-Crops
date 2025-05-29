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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.Iterator;

public class CropService implements Listener {
    private final boolean hasSkyblock = DependencyManager.isSkyblockEnabled();
    private final CropGrowthScheduler scheduler;

    public CropService(CropGrowthScheduler scheduler) {
        this.scheduler = scheduler;
    }

    private boolean canBreak(Player player, Block block) {
        if (!hasSkyblock) return true;
        SuperiorPlayer sp = SuperiorSkyblockAPI.getPlayer(player);
        Island island = SuperiorSkyblockAPI.getIslandAt(block.getLocation());
        return island.hasPermission(sp, IslandPrivilege.getByName("Break"));
    }

    /**
     * Core break handler, accepts an optional player context.
     */
    public void handleBreak(Block cropBlock, boolean forced, Player player) {
        CropData data = CropUtils.getCropData(cropBlock);
        if (data == null) return;

        CropDefinition def = CropDefinitionRegistry.get(data.getCropType());

        boolean autoReplantAllowed = def.isAutoReplantAllowed();
        boolean autoReplantPermissible = (player != null && player.hasPermission("wildcrops.autoreplant"));
        boolean sneakBreak = (player != null && player.isSneaking());
        boolean willAutoReplant = autoReplantAllowed && autoReplantPermissible && !forced && !sneakBreak;

        CropUtils.removeCrop(cropBlock);
        CropUtils.processCropDrops(cropBlock, data, forced, player, willAutoReplant);

        if (willAutoReplant) {
            Block below = cropBlock.getRelative(BlockFace.DOWN);

            if (below.getType() == Material.FARMLAND) {
                Chunk chunk = below.getChunk();
                Bukkit.getScheduler().runTaskLater(WildCrops.getInstance(), () -> {
                    cropBlock.setType(BlockCache.resolve(def.getSeedMaterial()));

                    ChunkCropManager.addCrop(chunk, cropBlock, data.getCropType());

                    CropData nData = ChunkCropManager.getCropsData(chunk)
                            .get(ChunkCropManager.getRelativeKey(cropBlock));
                    if (nData == null) return;

                    Crop crop = Crop.fromBlock(cropBlock, nData);
                    if (crop != null) {
                        scheduler.scheduleCropGrowth(crop);
                    }
                }, 5);
            }
        }
    }

    /**
     * Overload for non-player breaks (pistons, water, explosions, etc.)
     */
    public void handleBreak(Block cropBlock, boolean forced) {
        handleBreak(cropBlock, forced, null);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player player = e.getPlayer();

        // Breaking farmland under a crop
        if (b.getType() == Material.FARMLAND) {
            Block crop = b.getRelative(BlockFace.UP);
            if (CropUtils.isCustomCrop(crop) && canBreak(player, crop)) {
                e.setDropItems(false);
                handleBreak(crop, false, player);
            }
            return;
        }

        if (CropUtils.isCustomCrop(b) && canBreak(player, b)) {
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
            if (CropUtils.isCustomCrop(crop)) {
                e.getDrops().clear();
                handleBreak(crop, true);
            }
            return;
        }

        if (CropUtils.isCustomCrop(b)) {
            e.getDrops().clear();
            handleBreak(b, true);
        }
    }

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent e) {
        Block b = e.getBlock();
        if (!CropUtils.isCustomCrop(b)) return;

        e.getItems().clear();
        e.setCancelled(true);
        handleBreak(b, true);
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent e) {
        Iterator<Block> it = e.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            if (CropUtils.isCustomCrop(b)) {
                it.remove();
                handleBreak(b, true);
            }
        }
    }

    private void processPistonBlocks(Iterable<Block> blocks) {
        for (Block b : blocks) {
            if (b.getType() != Material.FARMLAND) continue;
            Block crop = b.getRelative(BlockFace.UP);
            if (!CropUtils.isCustomCrop(crop)) continue;
            handleBreak(crop, true);
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
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK
                && e.getClickedBlock() != null
                && CropUtils.isCustomCrop(e.getClickedBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBoneMealDispense(BlockDispenseEvent e) {
        if (e.getItem().getType() != Material.BONE_MEAL) return;
        Directional d = (Directional) e.getBlock().getBlockData();
        Block target = e.getBlock().getRelative(d.getFacing());
        if (CropUtils.isCustomCrop(target)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onGrow(BlockGrowEvent e) {
        if (CropUtils.isCustomCrop(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent e) {
        if (CropUtils.isCustomCrop(e.getLocation().getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent e) {
        if (CropUtils.isCustomCrop(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    public void onBlockFade(BlockFadeEvent event) {
        if (CropUtils.isCustomCrop(event.getBlock())) {
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
        if (!CropUtils.isCustomCrop(crop) || !canBreak(player, crop)) return;

        crop.getDrops().clear();
        crop.setType(Material.AIR);

        boolean forced = !below.getRelative(BlockFace.DOWN).getType().equals(Material.FARMLAND);
        handleBreak(crop, forced, player);
    }
}
