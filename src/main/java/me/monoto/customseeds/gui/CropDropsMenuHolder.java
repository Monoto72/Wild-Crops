package me.monoto.customseeds.gui;

import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.Range;
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

/**
 * GUI for editing the item-type rewards (crop drops) of a custom crop.
 */
public class CropDropsMenuHolder implements InventoryHolder {
    private static final int SIZE = 27;
    private static final int MIN_SLOT = 10;
    private static final int MAX_SLOT = 16;

    private final Inventory inventory;
    private final String cropType;
    private final CropDefinition cropDefinition;

    public CropDropsMenuHolder(String cropType) {
        this.cropType = cropType;
        this.cropDefinition = CropDefinitionRegistry.get(cropType);
        String title = "Edit Crop Drops - " + StringUtils.capitalize(cropDefinition.getType());
        this.inventory = Bukkit.createInventory(this, SIZE, Component.text(title));
        initializeItems();
    }

    /**
     * Populate the GUI slots
     */
    private void initializeItems() {
        // outer filler
        for (int i = 0; i < SIZE; i++) {
            if (i < MIN_SLOT || i > MAX_SLOT) {
                inventory.setItem(i, createFillerItem());
            } else {
                inventory.setItem(i, new ItemStack(Material.AIR));
            }
        }
        inventory.setItem(26, createBackButton());

        List<CropDefinition.Reward> itemRewards = cropDefinition.getRewards().stream()
                .filter(r -> r.type().equalsIgnoreCase("item"))
                .toList();

        int slot = MIN_SLOT;
        for (CropDefinition.Reward r : itemRewards) {
            if (slot > MAX_SLOT) break;
            Material mat = r.material();
            Range<Integer> amount = r.amount();
            inventory.setItem(slot, createRewardItem(mat, amount));
            slot++;
        }
    }

    /**
     * Creates a black glass pane filler
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
     * Creates the visual representation of one item reward
     */
    public ItemStack createRewardItem(Material material, Range<Integer> range) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = StringUtils.capitalize(material.name().toLowerCase().replace('_', ' '));
            meta.displayName(Component.text(name)
                    .color(TextColor.color(0x00FF00))
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            String rangeStr = (Objects.equals(range.getMinimum(), range.getMaximum()))
                    ? String.valueOf(range.getMinimum())
                    : range.getMinimum() + " to " + range.getMaximum();
            lore.add(Component.text("Amount: " + rangeStr)
                    .color(TextColor.color(0xAAAAAA))
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Back button
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

    public String getCropType() {
        return this.cropType;
    }

    public void open(Player player) {
        player.openInventory(this.inventory);
    }
}
