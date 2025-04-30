package me.monoto.customseeds.gui.buttons;

import dev.triumphteam.gui.click.ClickContext;
import dev.triumphteam.gui.item.GuiItem;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import dev.triumphteam.nova.MutableState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;

public class ToggleButton {

    public static GuiItem<Player, ItemStack> create(
            String label,
            Material icon,
            TextColor labelColor,
            MutableState<Boolean> state,
            BiConsumer<Player, ClickContext> onToggle
    ) {
        boolean current = state.get();

        return ItemBuilder.from(icon)
                .glow(current)
                .name(Component.text(label)
                        .color(labelColor)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text(current ? "Allowed" : "Not allowed")
                                .color(current ? TextColor.color(0x00FF00) : TextColor.color(0xFF0000))
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("Click to toggle")
                                .color(TextColor.color(0x7393B3))
                                .decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem((player, context) -> {
                    boolean newState = !state.get();
                    state.set(newState);

                    if (onToggle != null) onToggle.accept(player, context);

                    player.sendMessage(Component.text(label + " " + (newState ? "Enabled" : "Disabled"))
                            .color(newState ? TextColor.color(0x00FF00) : TextColor.color(0xFF0000)));
                });
    }
}
