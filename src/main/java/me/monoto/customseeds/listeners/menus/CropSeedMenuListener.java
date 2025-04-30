package me.monoto.customseeds.listeners.menus;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.gui.CropSeedLayoutMenuHolder;
import me.monoto.customseeds.gui.CropSettingsGui;
import me.monoto.customseeds.utils.ItemManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class CropSeedMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof CropSeedLayoutMenuHolder)) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int clickedSlot = event.getSlot();
        Player player = (Player) event.getWhoClicked();

        if (clickedSlot == CropSeedLayoutMenuHolder.PAGE_LEFT_SLOT) {
            CropSeedLayoutMenuHolder menu = (CropSeedLayoutMenuHolder) holder;
            if (menu.getCurrentPage() > 0) {
                menu.previousPage();
            }

            return;
        } else if (clickedSlot == CropSeedLayoutMenuHolder.PAGE_RIGHT_SLOT) {
            CropSeedLayoutMenuHolder menu = (CropSeedLayoutMenuHolder) holder;
            int totalItems = CropDefinitionRegistry.getDefinitions().size();
            int maxPage = (totalItems - 1) / CropSeedLayoutMenuHolder.ITEMS_PER_PAGE;
            if (menu.getCurrentPage() < maxPage) {
                menu.nextPage();
            }
            return;
        } else if (clickedSlot == CropSeedLayoutMenuHolder.PAGE_SLOT) {
            return;
        } else if (clickedSlot == CropSeedLayoutMenuHolder.CREATE_SEED_SLOT) {

            WildCrops.getInstance().getChatInput()
                    .openChatInput(player, "Enter Crop Name (e.g. Coal, Gold, Butter):", input -> {
                        String cropType = input.replace(" ", "_").toLowerCase();

                        if (cropType.isBlank() || !cropType.matches("^[a-z0-9_]+$")) {
                            player.sendMessage("Invalid crop name.");
                            return;
                        }

                        CropConfigData cropData = WildCrops.getInstance().getFileManager().getCropData(cropType);
                        if (cropData == null) {
                            // It doesn't exist, so create it
                            WildCrops.getInstance().getFileManager().createCropConfig(cropType, null);

                            cropData = WildCrops.getInstance().getFileManager().getCropData(cropType);
                            if (cropData == null) {
                                player.sendMessage("Failed to create crop config for '" + cropType + "'.");
                                new CropSeedLayoutMenuHolder().open(player);
                                return;
                            }
                            // Log the new creation, and register it
                            YamlConfiguration config = cropData.getConfig();
                            CropDefinition definition = CropDefinition.fromConfig(cropData.getFileNameWithoutExtension(), config);
                            CropDefinitionRegistry.update(definition.getId(), definition);
                            new CropSettingsGui(definition.getId()).open(player);
                        } else {
                            // If the crop already exists, inform the player and/or open its settings
                            player.sendMessage("Crop '" + cropType + "' already exists.");
                            YamlConfiguration config = cropData.getConfig();
                            CropDefinition definition = CropDefinition.fromConfig(cropData.getFileNameWithoutExtension(), config);
                            CropDefinitionRegistry.update(definition.getId(), definition);
                            new CropSettingsGui(definition.getId()).open(player);
                        }
                    });
            return;
        }

        String cropType = ItemManager.getCustomCropType(clickedItem);
        if (cropType == null) return;

        ClickType clickType = event.getClick();
        if (clickType.isLeftClick()) {
            new CropSettingsGui(cropType).open(player);
        } else if (clickType.isRightClick()) {
            WildCrops.getInstance().getChatInput().openChatInput(player,
                    "Type 'confirm' to delete this seed.", input -> {
                        if (input.equalsIgnoreCase("confirm")) {
                            WildCrops.getInstance().getFileManager().deleteCropConfig(cropType);
                            player.sendMessage("Crop '" + cropType + "' deleted.");
                            new CropSeedLayoutMenuHolder().open(player);


                        } else {
                            player.sendMessage("Seed deletion canceled.");
                        }
                    });
        } else if (clickType.equals(ClickType.MIDDLE)) {
        // Middle click: rename the crop configuration.
        WildCrops.getInstance().getChatInput().openChatInput(player,
                "Enter new crop name (e.g. coal, gold, butter):", input -> {
                    String newCropType = input.replace(" ", "_").toLowerCase();

                    if (WildCrops.getInstance().getFileManager().getCropData(newCropType) != null) {
                        player.sendMessage("A crop with that name already exists.");
                        return;
                    }

                    WildCrops.getInstance().getFileManager().renameCropConfig(cropType, newCropType);
                    player.sendMessage("Crop renamed from '" + cropType + "' to '" + newCropType + "'.");
                    new CropSeedLayoutMenuHolder().open(player);
                });
        }
    }
}