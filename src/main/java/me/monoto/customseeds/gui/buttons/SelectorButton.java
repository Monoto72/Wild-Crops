package me.monoto.customseeds.gui.buttons;

import dev.triumphteam.gui.actions.GuiCloseAction;
import dev.triumphteam.gui.container.GuiContainer;
import dev.triumphteam.gui.container.type.GuiContainerType;
import dev.triumphteam.gui.item.GuiItem;
import dev.triumphteam.gui.layout.GuiLayout;
import dev.triumphteam.gui.paper.Gui;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import dev.triumphteam.gui.slot.Slot;
import dev.triumphteam.gui.state.pagination.PagerState;
import me.monoto.customseeds.gui.CropSettingsGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;

public class SelectorButton {

    public static GuiItem<Player, ItemStack> create(
            String label,
            Material icon,
            TextColor labelColor,
            String selectionTitle,
            List<ItemStack> choices,
            BiConsumer<Player, ItemStack> onSelect,
            String cropType
    ) {
        PagerState<ItemStack> pageState = PagerState.of(
                choices,
                GuiLayout.box(Slot.of(1, 1), Slot.of(5, 9))
        );

        Gui selectionGui = Gui.of(6)
                .title(title -> {
                    title.remember(pageState);
                    title.render(() -> Component.text(selectionTitle + " | Page " + (pageState.getCurrentPage() + 1)));
                })
                .component(component -> {
                    component.remember(pageState);

                    component.render(container -> {
                        pageState.forEach(entry -> {
                            ItemStack item = entry.element();
                            container.setItem(entry.slot(), ItemBuilder.from(item).asGuiItem((player, context) -> {
                                onSelect.accept(player, item);
                                new CropSettingsGui(cropType).open(player);
                            }));
                        });

                        container.fill(
                                GuiLayout.box(Slot.of(6, 1), Slot.of(6, 9)),
                                ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                                        .name(Component.text(" ")).asGuiItem()
                        );

                        container.setItem(Slot.of(6, 4),
                                ItemBuilder.from(Material.ARROW)
                                        .name(Component.text("Previous Page"))
                                        .asGuiItem((player, context) -> pageState.prev())
                        );

                        container.setItem(Slot.of(6, 6),
                                ItemBuilder.from(Material.ARROW)
                                        .name(Component.text("Next Page"))
                                        .asGuiItem((player, context) -> pageState.next())
                        );
                    });
                })
                .build();

        return ItemBuilder.from(icon)
                .name(Component.text(label).color(labelColor).decoration(TextDecoration.ITALIC, false))
                .lore(Component.text("Click to choose").color(TextColor.color(0x7393B3)))
                .asGuiItem((player, context) -> selectionGui.open(player));
    }
}
