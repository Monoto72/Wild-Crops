package me.monoto.customseeds.gui.items;

import me.monoto.customseeds.gui.windows.BlockSearchWindow;
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

import java.util.List;
import java.util.function.BiConsumer;

public class BlockSelectorItem extends AbstractItem {

    private final String label;
    private final Material icon;
    private final TextColor labelColor;
    private final BiConsumer<Player, Material> onSelect;
    private final List<Material> allowedMaterials; // <- add this

    public BlockSelectorItem(String label, Material icon, TextColor labelColor,
                             BiConsumer<Player, Material> onSelect,
                             List<Material> allowedMaterials) { // <- add this
        this.label = label;
        this.icon = icon;
        this.labelColor = labelColor;
        this.onSelect = onSelect;
        this.allowedMaterials = allowedMaterials;
    }

    @Override
    public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
        return new ItemBuilder(icon)
                .setName(Component.text(label)
                        .color(labelColor)
                        .decoration(TextDecoration.ITALIC, false))
                .addLoreLines(Component.text("Click to choose a block")
                        .color(TextColor.color(0x7393B3)));
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
        BlockSearchWindow.open(player, labelColor, allowedMaterials, onSelect);
    }
}