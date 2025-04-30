package me.monoto.customseeds.listeners.menus;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.gui.CropDropsMenuHolder;
import me.monoto.customseeds.gui.CropSeedLayoutMenuHolder;
import me.monoto.customseeds.gui.CropTextMenuHolder;
import me.monoto.customseeds.gui.CropSettingsGui;
import org.apache.commons.lang3.Range;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class CropSettingsMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof CropSettingsGui)) return;

        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int slot = event.getSlot();
        Player player = (Player) event.getWhoClicked();
        String cropType = ((CropSettingsGui) holder).getCropType();

        switch (slot) {
            case 10:
                // Edit Name clicked.
                new CropTextMenuHolder(cropType).open(player);
                break;
            case 11:
                // Edit Time clicked.
                WildCrops.getInstance().getChatInput()
                        .openChatInput(player, "Enter Time in seconds (e.g. 60):", input -> {
                            CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
                            if (data != null) {
                                int time;

                                try {
                                    time = Integer.parseInt(input);
                                    if (time < 0) {
                                        player.sendMessage("Invalid time! Must be a positive integer.");
                                        return;
                                    }
                                } catch (NumberFormatException e) {
                                    player.sendMessage("Invalid input! Please enter a valid number.");
                                    return;
                                }
                                data.getConfig().set("grow_time", time);
                                WildCrops.getInstance().getFileManager().saveCropConfig(cropType);

                                CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), data.getConfig());
                                CropDefinitionRegistry.update(cropType, newDef);
                            }

                            new CropSettingsGui(cropType).open(player);
                        });
                break;
            case 13:
                // Crop Seed clicked.
                break;
            case 15:
                // Crop Drops clicked.
                new CropDropsMenuHolder(cropType).open(player);
                break;
            case 16:
                // Edit Seed Drops clicked.
                final Range<Integer> defaultRange = Range.between(1, 3);

                if (event.getClick().isRightClick()) {
                    CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
                    if (data != null) {
                        data.getConfig().set("drops.seed.amount", null);
                        WildCrops.getInstance().getFileManager().saveCropConfig(cropType);
                        CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), data.getConfig());
                        CropDefinitionRegistry.update(cropType, newDef);
                    }
                    player.sendMessage("Seed drop removed.");
                    new CropSettingsGui(cropType).open(player);
                } else {
                    WildCrops.getInstance().getChatInput()
                            .openChatInput(player, "Enter Seed Amount (e.g. 1 or 1-5):", input -> {
                                Range<Integer> newRange = parseRange(input, defaultRange);
                                CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
                                if (data != null) {
                                    // If input is 0 (or "0" or "0-0"), remove the seed drop.
                                    if (newRange.getMinimum() == 0 && newRange.getMaximum() == 0) {
                                        data.getConfig().set("drops.seed.amount", null);
                                        player.sendMessage("Seed drop removed (set to 0).");
                                    } else {
                                        data.getConfig().set("drops.seed.amount", input);
                                        player.sendMessage("Seed drop updated to: " + input);
                                    }
                                    WildCrops.getInstance().getFileManager().saveCropConfig(cropType);
                                    CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), data.getConfig());
                                    CropDefinitionRegistry.update(cropType, newDef);
                                }
                                new CropSettingsGui(cropType).open(player);
                            });
                }
                break;
            case 26:
                // Back button clicked.
                new CropSeedLayoutMenuHolder().open(player);
                break;
            default:
                break;
        }
    }

    /**
     * Parses a player's input into a Range<Integer>. If parsing fails, returns the provided default range.
     */
    private Range<Integer> parseRange(String input, Range<Integer> defaultRange) {
        try {
            if (input.contains("-")) {
                String[] parts = input.split("-");
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return Range.between(min, max);
            } else {
                int value = Integer.parseInt(input.trim());
                return Range.between(value, value);
            }
        } catch (Exception e) {
            return defaultRange;
        }
    }
}