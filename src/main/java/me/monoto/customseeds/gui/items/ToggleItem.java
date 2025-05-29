package me.monoto.customseeds.gui.items;

import me.monoto.customseeds.utils.ClickAction;
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
        ItemBuilder builder = new ItemBuilder(icon)
                .setName(Component.text(label)
                        .color(labelColor)
                        .decoration(TextDecoration.ITALIC, false));

        List<ClickAction> actions = List.of(
                new ClickAction(
                        ClickType.LEFT,
                        "Toggle " + label,
                        enabled ? Component.text("ON") : Component.text("OFF")
                )
        );

        for (ClickAction action : actions) {
            Component prefix = Component.text(ClickAction.humanize(action.clickType) + ": ")
                    .color(TextColor.color(0xAAAAAA));
            Component line = prefix.append(
                    action.description.color(TextColor.color(0xAAAAAA))
            );
            if (action.currentValue != null) {
                line = line.append(Component.text(" (v: ")
                                .color(TextColor.color(0xAAAAAA)))
                        .append(action.currentValue.color(TextColor.color(0xAAAAAA)))
                        .append(Component.text(")").color(TextColor.color(0xAAAAAA)));
            }
            builder.addLoreLines(line);
        }


        return builder;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
        enabled = !enabled;

        if (onToggle != null) {
            onToggle.accept(player, click);
        }

        player.sendMessage(Component.text(label + " " + (enabled ? "Enabled" : "Disabled"))
                .color(enabled ? TextColor.color(0x00FF00) : TextColor.color(0xFF0000)));

        notifyWindows();
    }

    public boolean isEnabled() {
        return enabled;
    }
}
