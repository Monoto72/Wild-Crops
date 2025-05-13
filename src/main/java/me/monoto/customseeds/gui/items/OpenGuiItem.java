package me.monoto.customseeds.gui.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.function.Consumer;

public class OpenGuiItem extends AbstractItem {

    private final String label;
    private final Material icon;
    private final TextColor labelColor;
    private final Consumer<Player> openAction;

    public OpenGuiItem(String label, Material icon, TextColor labelColor, Consumer<Player> openAction) {
        this.label = label;
        this.icon = icon;
        this.labelColor = labelColor;
        this.openAction = openAction;
    }

    @Override
    public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
        return new ItemBuilder(icon)
                .setName(Component.text(label).color(labelColor).decoration(TextDecoration.ITALIC, false))
                .addLoreLines(Component.text("Click to open").color(TextColor.color(0x7393B3)));
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
        openAction.accept(player);
    }
}