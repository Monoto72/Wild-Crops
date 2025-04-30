package me.monoto.customseeds.gui.buttons;

import dev.triumphteam.gui.item.GuiItem;
import dev.triumphteam.gui.paper.Gui;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class OpenGuiButton {

    /**
     * A simple button that opens the specified GUI when clicked.
     */
    public static GuiItem<Player, ItemStack> create(
            String label,
            Material icon,
            TextColor labelColor,
            Gui targetGui
    ) {
        return ItemBuilder.from(icon)
                .name(Component.text(label)
                        .color(labelColor)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(Component.text("Click to open").color(TextColor.color(0x7393B3)))
                .asGuiItem((player, context) -> targetGui.open(player));
    }
}
