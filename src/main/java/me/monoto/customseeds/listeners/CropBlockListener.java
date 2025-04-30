package me.monoto.customseeds.listeners;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import me.monoto.customseeds.services.CropService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class CropBlockListener implements Listener {
    private final CropService service = new CropService();

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
        service.onBoneMealDispense(e);
    }


}

