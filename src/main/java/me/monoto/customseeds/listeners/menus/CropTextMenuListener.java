package me.monoto.customseeds.listeners.menus;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.gui.CropTextMenuHolder;
import me.monoto.customseeds.gui.CropSettingsGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public class CropTextMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof CropTextMenuHolder)) return;

        event.setCancelled(true);

        CropTextMenuHolder menu = (CropTextMenuHolder) holder;
        Player player = (Player) event.getWhoClicked();
        String cropType = ((CropTextMenuHolder) holder).getCropType();
        int slot = event.getSlot();

        if (slot == 10) {
            // Edit Name
            WildCrops.getInstance().getChatInput().openChatInput(player,
                    "Enter new crop name:", input -> {
                        CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);

                        data.getConfig().set("item.display_name", input);
                        WildCrops.getInstance().getFileManager().saveCropConfig(cropType);

                        CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), data.getConfig());
                        CropDefinitionRegistry.update(cropType, newDef);
                        new CropTextMenuHolder(menu.getCropType()).open(player);
                    });
        } else if (slot >= 12 && slot <= 16) {
            // Edit Lore lines (slots 12-16)

            int loreIndex = slot - 12;
            WildCrops.getInstance().getChatInput().openChatInput(player,
                    "Enter new lore for line " + (loreIndex + 1) + ":", input -> {
                        CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
                        if (data == null) {
                            player.sendMessage("Crop configuration not found!");
                            return;
                        }
                        List<String> loreList = data.getConfig().getStringList("item.lore");
                        while (loreList.size() <= loreIndex) {
                            loreList.add("");
                        }

                        loreList.set(loreIndex, input);

                        data.getConfig().set("item.lore", loreList);
                        WildCrops.getInstance().getFileManager().saveCropConfig(cropType);

                        CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), data.getConfig());
                        CropDefinitionRegistry.update(cropType, newDef);

                        new CropTextMenuHolder(cropType).open(player);
                    });
        } else if (slot == 26) {
            // Back button: open the CropSettings menu.
            new CropSettingsGui(menu.getCropType()).open(player);
        }
    }
}
