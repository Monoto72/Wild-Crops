package me.monoto.customseeds.gui;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.gui.items.ChatInputItem;
import me.monoto.customseeds.gui.items.FillerItem;
import me.monoto.customseeds.gui.items.PageNavItem;
import me.monoto.customseeds.utils.ChatInput.ChatInputType;
import me.monoto.customseeds.utils.ClickAction;
import me.monoto.customseeds.utils.ItemManager;
import me.monoto.customseeds.utils.Utilities;
import me.monoto.customseeds.utils.WindowHistory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
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
        List<Map.Entry<String, CropDefinition>> sorted = new ArrayList<>(
                CropDefinitionRegistry.getDefinitions().entrySet());
        sorted.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, CropDefinition> entry : sorted) {
            seedItems.add(buildSeedItem(entry.getKey(), entry.getValue()));
        }
        return seedItems;
    }

    private Item buildSeedItem(String cropId, CropDefinition definition) {
        List<ClickAction> actions = List.of(
                new ClickAction(ClickType.LEFT, "Edit Settings"),
                new ClickAction(ClickType.RIGHT, "Delete Crop"),
                new ClickAction(ClickType.SHIFT_LEFT, "Rename Crop")
        );

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(" "));
        for (ClickAction act : actions) {
            String human = ClickAction.humanize(act.clickType);
            Component line = Component.text(human + ": ")
                    .append(act.description)
                    .color(TextColor.color(0xAAAAAA))
                    .decoration(TextDecoration.ITALIC, false);
            lore.add(line);
        }

        return Item.builder()
                .setItemProvider(new ItemBuilder(ItemManager.getSeed(cropId, 1))
                        .addLoreLines(lore))
                .addClickHandler(buildClickHandler(cropId))
                .build();
    }

    private BiConsumer<Item, Click> buildClickHandler(String cropId) {
        return (item, click) -> {
            Player player = click.player();
            ClickType clickType = click.clickType();

            switch (clickType) {
                case LEFT -> new CropSettingsGui(cropId).open(player);
                case RIGHT -> {
                    player.closeInventory();
                    String code = Utilities.generateRandomCode(5);
                    player.sendMessage(Component.text("To confirm deletion, type: ")
                            .append(Component.text(code)
                                    .color(TextColor.color(0xFF474C))
                                    .decorate(TextDecoration.BOLD)));

                    ChatInputItem<String> deletePrompt = new ChatInputItem<>(
                            "Confirm Delete",
                            Material.PAPER,
                            TextColor.color(0xFF474C),
                            "Enter the confirmation code to delete:",
                            ChatInputType.STRING,
                            (p, input) -> {
                                if (input.equalsIgnoreCase(code)) {
                                    WildCrops.getInstance().getFileManager().deleteCropConfig(cropId);
                                    CropDefinitionRegistry.remove(cropId);
                                    p.sendMessage("Crop '" + cropId + "' deleted.");
                                } else {
                                    p.sendMessage("Incorrect code. Deletion canceled.");
                                }
                                WindowHistory.replace(p, () -> new CropSeedLayoutGui(currentPage).createWindow(p));
                            },
                            () -> WindowHistory.replace(player, () -> new CropSeedLayoutGui(currentPage).createWindow(player))
                    );
                    deletePrompt.handleClick(clickType, player, click);
                }

                case SHIFT_LEFT -> {
                    player.closeInventory();
                    ChatInputItem<String> renamePrompt = new ChatInputItem<>(
                            "Rename Crop",
                            Material.PAPER,
                            TextColor.color(0xFFFF00),
                            "Enter new crop name:",
                            ChatInputType.STRING,
                            (p, input) -> {
                                String newId = input.replace(" ", "_").toLowerCase();
                                if (WildCrops.getInstance().getFileManager().getCropData(newId) != null) {
                                    p.sendMessage("A crop with that name already exists.");
                                } else {
                                    WildCrops.getInstance().getFileManager().renameCropConfig(cropId, newId);
                                    p.sendMessage("Crop renamed to '" + newId + "'.");
                                }
                                WindowHistory.replace(p, () -> new CropSeedLayoutGui(currentPage).createWindow(p));
                            },
                            () -> WindowHistory.replace(player, () -> new CropSeedLayoutGui(currentPage).createWindow(player))
                    );
                    renamePrompt.handleClick(clickType, player, click);
                }
                default -> {
                }
            }
        };
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
                    ChatInputItem<String> prompt = new ChatInputItem<>(
                            "Create Seed",
                            Material.CRAFTING_TABLE,
                            TextColor.color(0x00FF00),
                            "Enter Crop Name (e.g. Coal, Gold):",
                            ChatInputType.STRING,
                            (p, input) -> {
                                String id = input.replace(" ", "_").toLowerCase();
                                if (id.isBlank() || !id.matches("^[a-z0-9_]+$")) {
                                    p.sendMessage("Invalid crop name.");
                                    WindowHistory.replace(p, () -> new CropSeedLayoutGui(currentPage).createWindow(p));
                                    return;
                                }
                                if (WildCrops.getInstance().getFileManager().getCropData(id) != null) {
                                    p.sendMessage("Crop already exists.");
                                    WindowHistory.replace(p, () -> new CropSeedLayoutGui(currentPage).createWindow(p));
                                    return;
                                }
                                WildCrops.getInstance().getFileManager().createCropConfig(id, null);
                                CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(id);
                                if (data != null) {
                                    CropDefinition newDef = CropDefinition.fromConfig(id, data.getConfig());
                                    CropDefinitionRegistry.update(id, newDef);
                                }
                                WindowHistory.replace(p, () -> new CropSeedLayoutGui(currentPage).createWindow(p));
                            },
                            () -> WindowHistory.replace(player, () -> new CropSeedLayoutGui(currentPage).createWindow(player))
                    );
                    prompt.handleClick(ClickType.LEFT, player, click);
                })
                .build();
    }

    public Window createWindow(Player player) {
        return Window.builder()
                .setViewer(player)
                .setUpperGui(gui)
                .setTitle("Custom Crops")
                .build(player);
    }

    public void open(Player player) {
        WindowHistory.open(player, () -> createWindow(player));
    }
}
