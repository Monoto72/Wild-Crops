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

import java.util.function.BiConsumer;

public class ToggleItem extends AbstractItem {

    private final String label;
    private final Material icon;
    private final TextColor labelColor;
    private final BiConsumer<Player, Click> onToggle;

    private boolean enabled;

    public ToggleItem(String label, Material icon, TextColor labelColor,
                      boolean initialState, BiConsumer<Player, Click> onToggle) {
        this.label = label;
        this.icon = icon;
        this.labelColor = labelColor;
        this.enabled = initialState;
        this.onToggle = onToggle;
    }

    @Override
    public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
        return new ItemBuilder(icon)
                .setName(Component.text(label)
                        .color(labelColor)
                        .decoration(TextDecoration.ITALIC, false))
                .addLoreLines(
                        Component.text(enabled ? "Allowed" : "Not allowed")
                                .color(enabled ? TextColor.color(0x00FF00) : TextColor.color(0xFF0000))
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("Click to toggle")
                                .color(TextColor.color(0x7393B3))
                                .decoration(TextDecoration.ITALIC, false)
                );
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
        enabled = !enabled;

        if (onToggle != null) {
            onToggle.accept(player, click);
        }

        player.sendMessage(Component.text(label + " " + (enabled ? "Enabled" : "Disabled"))
                .color(enabled ? TextColor.color(0x00FF00) : TextColor.color(0xFF0000)));

        notifyWindows(); // Re-render the item
    }

    public boolean isEnabled() {
        return enabled;
    }
}
