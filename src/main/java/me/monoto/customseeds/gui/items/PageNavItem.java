package me.monoto.customseeds.gui.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.AbstractPagedGuiBoundItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

public class PageNavItem extends AbstractPagedGuiBoundItem {

    private final int direction;
    private final Material icon;
    private final String label;
    private final TextColor color;

    public PageNavItem(int direction, Material icon, String label, TextColor color) {
        this.direction = direction;
        this.icon = icon;
        this.label = label;
        this.color = color;
    }

    @Override
    public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
        return new ItemBuilder(icon)
                .setName(Component.text(label).color(color).decoration(TextDecoration.ITALIC, false))
                .addLoreLines(
                        Component.text("Click to go " + (direction > 0 ? "forward" : "back"))
                                .color(TextColor.color(0x7393B3)),
                        Component.text("Shift-Click to jump to " + (direction > 0 ? "last" : "first"))
                                .color(TextColor.color(0x7393B3))
                );
    }

    @Override
    public void handleClick(ClickType clickType, @NotNull Player player, @NotNull Click click) {
        PagedGui<?> gui = getGui();
        if (clickType.isShiftClick()) {
            gui.setPage(direction > 0 ? gui.getPageCount() - 1 : 0);
        } else {
            int newPage = gui.getPage() + direction;
            if (newPage >= 0 && newPage < gui.getPageCount()) {
                gui.setPage(newPage);
            }
        }
    }
}
