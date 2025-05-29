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
import java.util.function.Consumer;

/**
 * A simple item which opens a GUI and uses shared ClickAction lore formatting.
 */
public class OpenGuiItem extends AbstractItem {

    private final Component name;
    private final Material icon;
    private final TextColor color;
    private final Consumer<Player> openAction;

    public OpenGuiItem(String label, Material icon, TextColor labelColor, Consumer<Player> openAction) {
        this.name = Component.text(label);
        this.icon = icon;
        this.color = labelColor;
        this.openAction = openAction;
    }

    @Override
    public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
        ItemBuilder builder = new ItemBuilder(icon)
                .setName(name.color(color).decoration(TextDecoration.ITALIC, false));

        List<ClickAction> actions = List.of(
                new ClickAction(ClickType.LEFT, "Open GUI")
        );
        for (ClickAction action : actions) {
            Component prefix = Component.text(ClickAction.humanize(action.clickType) + ": ")
                    .color(TextColor.color(0xAAAAAA));
            Component line = prefix.append(
                    action.description.color(TextColor.color(0xAAAAAA))
            );
            builder.addLoreLines(line);
        }

        return builder;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
        openAction.accept(player);
    }
}
