package me.monoto.customseeds.gui.windows;

import me.monoto.customseeds.gui.items.FillerItem;
import me.monoto.customseeds.gui.items.PageNavItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.window.AnvilWindow;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class BlockSearchWindow {

    public static void open(Player player, TextColor labelColor, List<Material> sourceMaterials, BiConsumer<Player, Material> onSelect) {
        List<Item> results = new ArrayList<>();

        // Results GUI
        PagedGui<Item> resultGui = PagedGui.itemsBuilder()
                .setStructure(
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "# # # < . > # # #"
                )
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', new FillerItem(Material.GRAY_STAINED_GLASS_PANE))
                .addIngredient('<', new PageNavItem(-1, Material.RED_DYE, "Previous Page", TextColor.color(0xFF474C)))
                .addIngredient('>', new PageNavItem(1, Material.LIME_DYE, "Next Page", TextColor.color(0x00FF00)))
                .build();

        // Window
        Window window = AnvilWindow.builder()
                .setViewer(player)
                .setUpperGui(PagedGui.itemsBuilder()
                        .setContent(results)
                        .setStructure("x x x")
                        .addIngredient('x', new FillerItem(Material.GRAY_STAINED_GLASS_PANE))
                )
                .setLowerGui(resultGui)
                .setTitle("Select a Output Item")
                .addRenameHandler(input -> {
                    results.clear();
                    String lower = input.toLowerCase();

                    for (Material mat : sourceMaterials) {
                        if (!mat.isItem()) continue;
                        if (mat.name().toLowerCase().contains(lower)) {
                            results.add(Item.builder()
                                    .setItemProvider(new ItemBuilder(mat)
                                            .setName(Component.text(mat.name())
                                                    .color(labelColor)
                                                    .decoration(TextDecoration.ITALIC, false))
                                            .addLoreLines(Component.text("Click to select this block")
                                                    .color(TextColor.color(0x7393B3))))
                                    .addClickHandler((item, click) -> {
                                        Player viewer = click.player();
                                        onSelect.accept(viewer, mat);
                                        click.player().closeInventory();
                                    })
                                    .build());
                        }
                    }

                    resultGui.setContent(results);
                })
                .build();

        window.open();
    }
}
