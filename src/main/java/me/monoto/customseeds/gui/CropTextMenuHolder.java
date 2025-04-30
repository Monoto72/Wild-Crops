package me.monoto.customseeds.gui;

import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.utils.Formatters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CropTextMenuHolder implements InventoryHolder {

    private final Inventory inventory;
    private final String cropType;
    private final CropDefinition cropDefinition;

    // Define the slots for our editable items:
    // Slot 10: Edit Name (map)
    // Slots 12-16: Edit Lore lines (paper items)
    // Slot 26: Back button
    public CropTextMenuHolder(String cropType) {
        this.cropType = cropType;
        this.cropDefinition = CropDefinitionRegistry.get(cropType);

        this.inventory = Bukkit.createInventory(this, 27, Component.text("Edit Crop - " + StringUtils.capitalize((CropDefinitionRegistry.get(cropType)).getType())));
        initializeItems();
    }

    private void initializeItems() {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, createFillerItem());
        }
        inventory.setItem(10, createEditNameItem());
        List<String> currentLore = cropDefinition != null ?
                cropDefinition.getLore() != null ?
                        cropDefinition.getLore() : new ArrayList<>()
                : new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            String loreLine = (i < currentLore.size()) ? currentLore.get(i) : "Empty";
            inventory.setItem(12 + i, createEditLoreItem(i + 1, loreLine));
        }

        inventory.setItem(26, createBackButton());
    }

    /**
     * Creates a filler item (black stained glass pane) with no display name.
     */
    private ItemStack createFillerItem() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /**
     * Creates the "Edit Name" item using a map.
     * Its lore shows the current name.
     */
    private ItemStack createEditNameItem() {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Edit Name")
                    .color(TextColor.color(0x00FF00))
                    .decoration(TextDecoration.ITALIC, false));

            String currentName = (cropDefinition != null && cropDefinition.getDisplayName() != null)
                    ? cropDefinition.getDisplayName()
                    : "None";

            List<Component> lore = new ArrayList<>(addLoreList(currentName, item, meta));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates an "Edit Lore" item for the given lore line number.
     * The display name will be "Edit Lore X" and the lore will show the current lore.
     *
     * @param lineNumber The lore line number (1-indexed).
     * @param currentLore The current lore text for that line.
     */
    private ItemStack createEditLoreItem(int lineNumber, String currentLore) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Edit Lore " + lineNumber)
                    .color(TextColor.color(0x00FF00))
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>(addLoreList(currentLore, item, meta));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<Component> addLoreList(String currentLore, ItemStack item, ItemMeta meta) {
        List<Component> lore = new ArrayList<>();

        if (Objects.equals(currentLore, "Empty")) {
            lore.add(Component.text("No Lore Set")
                    .color(TextColor.color(0xAAAAAA))
                    .decoration(TextDecoration.ITALIC, false));

            return lore;
        }

        lore.add(Component.text("Raw: " + currentLore)
                .color(TextColor.color(0xAAAAAA))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(net.kyori.adventure.text.Component.text(" "));

        lore.add(Component.text("Preview: ")
                .color(TextColor.color(0xAAAAAA))
                .decoration(TextDecoration.ITALIC, false)
                .append(Formatters.format(currentLore)));

        return lore;
    }

    /**
     * Creates a "Back" button item.
     */
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Back")
                    .color(TextColor.color(0xFF474C))
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    /**
     * Returns the crop type associated with this menu.
     */
    public String getCropType() {
        return cropType;
    }

    /**
     * Opens this inventory for the given player.
     *
     * @param player the player to open the menu for.
     */
    public void open(Player player) {
        player.openInventory(this.inventory);
    }
}
