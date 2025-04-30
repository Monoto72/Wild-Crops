package me.monoto.customseeds.gui;

import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.utils.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import java.util.Map;

public class CropSeedLayoutMenuHolder implements InventoryHolder {

    private static final int INVENTORY_SIZE = 45;
    private static final Component MENU_TITLE = Component.text("Custom Crops");

    public static final int PAGE_LEFT_SLOT = 39;
    public static final int PAGE_SLOT = 40;
    public static final int PAGE_RIGHT_SLOT = 41;
    public static int CREATE_SEED_SLOT = 10;
    public static final int ITEMS_PER_PAGE = 21;

    private int currentPage = 0;

    private final Inventory inventory;

    public CropSeedLayoutMenuHolder() {
        this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE, MENU_TITLE);
        fillBorders();
        clearSeedArea();
        addPaginationButtons();
        populateSeedItems();
    }

    /**
     * Fills the border of the menu with black stained glass panes.
     * Border includes the top row, bottom row, and the left/right edges of the middle rows.
     */
    private void fillBorders() {
        ItemStack borderItem = createGlassPane();
        // Top row (0-8) and bottom row (36-44)
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderItem);
            inventory.setItem(INVENTORY_SIZE - 9 + i, borderItem);
        }
        // For rows 1 to 3, set first and last columns
        for (int row = 1; row <= 3; row++) {
            int leftSlot = row * 9;
            int rightSlot = row * 9 + 8;
            inventory.setItem(leftSlot, borderItem);
            inventory.setItem(rightSlot, borderItem);
        }
    }

    /**
     * Clears the seed area (non-border slots in rows 1-3) by setting them to air.
     */
    private void clearSeedArea() {
        // Each row is 9 slots. Seed area for rows 1 to 3 (indexes 9 to 35)
        for (int row = 1; row <= 3; row++) {
            int start = row * 9;
            for (int col = 0; col < 9; col++) {
                int slot = start + col;
                // Skip border slots (first and last column)
                if (col == 0 || col == 8) {
                    continue;
                }
                inventory.setItem(slot, new ItemStack(Material.AIR));
            }
        }
    }

    /**
     * Adds pagination buttons to the bottom row.
     */
    private void addPaginationButtons() {
        ItemStack pageLeft = createButton(Material.ARROW, Component.text("Previous Page").color(TextColor.color(0x00FF00)));
        ItemStack pageRight = createButton(Material.ARROW, Component.text("Next Page").color(TextColor.color(0xFF474C)));

        inventory.setItem(PAGE_LEFT_SLOT, pageLeft);
        inventory.setItem(PAGE_RIGHT_SLOT, pageRight);

        updatePageIndicator(); // Place the dynamic "Page X" paper
    }

    /**
     * Populates the seed items into the defined seed area.
     */
    private void populateSeedItems() {
        clearSeedArea();

        Map<String, CropDefinition> definitions = CropDefinitionRegistry.getDefinitions();
        List<Map.Entry<String, CropDefinition>> sortedList = new ArrayList<>(definitions.entrySet());

        // Determine available slots based on page
        List<Integer> seedSlots = getSeedSlots().stream()
                .filter(slot -> !(currentPage == 0 && slot == CREATE_SEED_SLOT))
                .toList();

        int itemsPerPage = seedSlots.size(); // 20 on page 0, 21 on others
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, sortedList.size());

        int seedIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (seedIndex >= seedSlots.size()) break;

            int slot = seedSlots.get(seedIndex++);
            Map.Entry<String, CropDefinition> entry = sortedList.get(i);
            ItemStack seedItem = createSeedItem(entry.getKey(), entry.getValue());
            inventory.setItem(slot, seedItem);
        }

        if (currentPage == 0) {
            inventory.setItem(CREATE_SEED_SLOT, createSeed());
        }

        updatePageIndicator();
    }

    private ItemStack createSeed() {
        ItemStack seed = new ItemStack(Material.WHEAT_SEEDS);
        ItemMeta meta = seed.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Create Seed")
                    .color(TextColor.color(0x00FF00))
                    .decoration(TextDecoration.ITALIC, false));
            seed.setItemMeta(meta);
        }
        return seed;
    }

    private void updatePageIndicator() {
        ItemStack page = createButton(Material.PAPER, Component.text("Page " + (currentPage + 1))
                .color(TextColor.color(0xFFFF00))
                .decoration(TextDecoration.ITALIC, false));
        inventory.setItem(PAGE_SLOT, page);
    }

    public void nextPage() {
        int totalItems = CropDefinitionRegistry.getDefinitions().size();
        if ((currentPage + 1) * ITEMS_PER_PAGE < totalItems) {
            currentPage++;
            populateSeedItems();
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            populateSeedItems();
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    private static List<Integer> getSeedSlots() {
        List<Integer> seedSlots = new ArrayList<>();
        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 7; col++) { // skip border columns (0 and 8)
                int slot = row * 9 + col;
                seedSlots.add(slot);
            }
        }
        return seedSlots;
    }


    /**
     * Returns the first seed slot in the defined area.
     */
    private int getFirstSeedSlot() {
        return 10;
    }

    /**
     * Returns the last seed slot in the defined area.
     */
    private int getLastSeedSlot() {
        return 34;
    }

    /**
     * Returns the next available seed slot given the current slot.
     * Wraps to the next row if necessary.
     */
    private int getNextSeedSlot(int currentSlot) {
        if ((currentSlot % 9) == 16) {
            return currentSlot + 3;
        }
        return currentSlot + 1;
    }

    /**
     * Creates an ItemStack seed with custom lore and details.
     */
    private ItemStack createSeedItem(String cropType, CropDefinition cropDefinition) {
        ItemStack seed = ItemManager.getSeed(cropType, 1);
        seed.editMeta(meta -> {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(" "));

            lore.add(Component.text("Left Click to Edit")
                    .color(TextColor.color(0x00FF00))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Middle Click to Rename")
                    .color(TextColor.color(0xFFFF00))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Right Click to Delete")
                    .color(TextColor.color(0xFF474C))
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
        });
        return seed;
    }

    /**
     * Helper method to create a black stained glass pane.
     */
    private ItemStack createGlassPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        return pane;
    }

    /**
     * Helper method to create a button with a specified material and display name.
     */
    private ItemStack createButton(Material material, Component displayName) {
        ItemStack button = new ItemStack(material);
        button.editMeta(meta -> meta.displayName(displayName));
        return button;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    /**
     * Opens this inventory for a player.
     */
    public void open(Player player) {
        player.openInventory(this.inventory);
    }
}