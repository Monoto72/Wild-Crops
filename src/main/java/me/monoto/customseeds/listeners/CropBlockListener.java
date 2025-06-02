package me.monoto.customseeds.listeners;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropGrowthScheduler;
import me.monoto.customseeds.services.CropService;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class CropBlockListener implements Listener {
    private final CropService service = new CropService(new CropGrowthScheduler(WildCrops.getInstance()));

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        service.onBlockBreak(e);
    }

    @EventHandler
    public void onForcedBreak(BlockBreakBlockEvent e) {
        service.onBlockForcedBreak(e);
    }

    @EventHandler
    public void onDrop(BlockDropItemEvent e) {
        service.onBlockDrop(e);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        service.onExplosion(e);
    }

    @EventHandler
    public void onGrow(BlockGrowEvent e) {
        service.onGrow(e);
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent e) {
        service.onStructureGrow(e);
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent e) {
        service.onLeavesDecay(e);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent e) {
        service.onBlockFade(e);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        service.onPistonExtend(e);
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        service.onPistonRetract(e);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Action action = e.getAction();

        if (action == Action.PHYSICAL) {
            service.onPhysicalInteract(e);

        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            service.onBoneMealInteract(e);
        }
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent e) {
        Material mat = e.getItem().getType();

        if (mat == Material.BONE_MEAL) {
            service.onBoneMealDispense(e);
        } else if (mat == Material.WATER_BUCKET || mat == Material.LAVA_BUCKET) {
            service.onLiquidBucketDispense(e);
        }
    }
}

