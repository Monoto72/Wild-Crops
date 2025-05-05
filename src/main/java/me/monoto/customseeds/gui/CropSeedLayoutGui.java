package me.monoto.customseeds.gui;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.gui.items.FillerItem;
import me.monoto.customseeds.gui.items.PageNavItem;
import me.monoto.customseeds.utils.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.AbstractPagedGuiBoundItem;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.window.Window;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class CropSeedLayoutGui {

    private final int currentPage;
    private final PagedGui<Item> gui;

    public CropSeedLayoutGui(int currentPage) {
        this.currentPage = currentPage;
        this.gui = createGui();
    }

    private PagedGui<Item> createGui() {
        List<Item> seedItems = buildSeedItems();
        return buildPagedGui(seedItems);
    }

    private List<Item> buildSeedItems() {
        List<Item> seedItems = new ArrayList<>();

        List<Map.Entry<String, CropDefinition>> sorted = new ArrayList<>(CropDefinitionRegistry.getDefinitions().entrySet());
        sorted.sort(Comparator.comparing(Map.Entry::getKey));

        for (Map.Entry<String, CropDefinition> entry : sorted) {
            seedItems.add(buildSeedItem(entry.getKey(), entry.getValue()));
        }

        return seedItems;
    }

    private Item buildSeedItem(String cropId, CropDefinition definition) {
        return Item.builder()
                .setItemProvider(new ItemBuilder(ItemManager.getSeed(cropId, 1))
                        .addLoreLines(
                                Component.text(" "),
                                Component.text("Left Click to Edit").color(TextColor.color(0x00FF00)).decoration(TextDecoration.ITALIC, false),
                                Component.text("Middle Click to Rename").color(TextColor.color(0xFFFF00)).decoration(TextDecoration.ITALIC, false),
                                Component.text("Right Click to Delete").color(TextColor.color(0xFF474C)).decoration(TextDecoration.ITALIC, false)
                        ))
                .addClickHandler(buildClickHandler(cropId))
                .build();
    }

    private BiConsumer<Item, xyz.xenondevs.invui.Click> buildClickHandler(String cropId) {
        return (item, click) -> {
            Player player = click.player();
            ClickType type = click.clickType();

            if (type.isLeftClick()) {
                new CropSettingsGui(cropId).open(player);
            } else if (type.isRightClick()) {
                player.closeInventory();
                String code = generateRandomCode();
                player.sendMessage(Component.text("To confirm deletion, type: ")
                        .append(Component.text(code).color(TextColor.color(0xFF474C)).decorate(TextDecoration.BOLD)));

                Bukkit.getScheduler().runTask(WildCrops.getInstance(), () ->
                        WildCrops.getInstance().getChatInput().openChatInput(player, "Enter the confirmation code to delete:", input -> {
                            if (input.equalsIgnoreCase(code)) {
                                WildCrops.getInstance().getFileManager().deleteCropConfig(cropId);
                                player.sendMessage("Crop '" + cropId + "' deleted.");
                                new CropSeedLayoutGui(0).open(player);
                            } else {
                                player.sendMessage("Incorrect code. Seed deletion canceled.");
                            }
                        }));
            } else if (type == ClickType.MIDDLE) {
                player.closeInventory();
                Bukkit.getScheduler().runTask(WildCrops.getInstance(), () ->
                        WildCrops.getInstance().getChatInput().openChatInput(player, "Enter new crop name:", input -> {
                            String newId = input.replace(" ", "_").toLowerCase();
                            if (WildCrops.getInstance().getFileManager().getCropData(newId) != null) {
                                player.sendMessage("A crop with that name already exists.");
                                return;
                            }
                            WildCrops.getInstance().getFileManager().renameCropConfig(cropId, newId);
                            player.sendMessage("Crop renamed to '" + newId + "'.");
                            new CropSeedLayoutGui(0).open(player);
                        }));
            }
        };
    }

    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private PagedGui<Item> buildPagedGui(List<Item> content) {
        return PagedGui.itemsBuilder()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# # # < . > # # #"
                )
                .addIngredient('#', new FillerItem(Material.GRAY_STAINED_GLASS_PANE))
                .addIngredient('<', new PageNavItem(-1, Material.RED_DYE, "Previous Page", TextColor.color(0xFF474C)))
                .addIngredient('>', new PageNavItem(1, Material.LIME_DYE, "Next Page", TextColor.color(0x00FF00)))
                .addIngredient('.', buildCreateSeedItem())
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(content)
                .build();
    }

    private Item buildCreateSeedItem() {
        return Item.builder()
                .setItemProvider(new ItemBuilder(Material.CRAFTING_TABLE)
                        .setName(Component.text("Create Seed")
                                .color(TextColor.color(0x00FF00))
                                .decoration(TextDecoration.ITALIC, false)))
                .addClickHandler((item, click) -> {
                    Player player = click.player();
                    player.closeInventory();
                    Bukkit.getScheduler().runTask(WildCrops.getInstance(), () ->
                            WildCrops.getInstance().getChatInput().openChatInput(player, "Enter Crop Name (e.g. Coal, Gold):", input -> {
                                String id = input.replace(" ", "_").toLowerCase();
                                if (id.isBlank() || !id.matches("^[a-z0-9_]+$")) {
                                    player.sendMessage("Invalid crop name.");
                                    return;
                                }
                                if (WildCrops.getInstance().getFileManager().getCropData(id) != null) {
                                    player.sendMessage("Crop already exists.");
                                    new CropSettingsGui(id).open(player);
                                    return;
                                }
                                WildCrops.getInstance().getFileManager().createCropConfig(id, null);
                                new CropSettingsGui(id).open(player);
                            }));
                })
                .build();
    }

    public void open(Player player) {
        Window.builder()
                .setViewer(player)
                .setUpperGui(gui)
                .setTitle("Custom Crops")
                .open(player);
    }
}
