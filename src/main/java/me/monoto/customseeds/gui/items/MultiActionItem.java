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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * An item which displays one or more click-actions (in a fixed, given order),
 * each with an optional "current value" in parentheses.
 */
public class MultiActionItem extends AbstractItem {

    private final Component name;
    private final Material icon;
    private final TextColor color;
    private final BiConsumer<Player, Click> onClick;
    private final List<ClickAction> actions;

    /**
     * @param name    display name
     * @param icon    item material
     * @param color   name color
     * @param onClick handler for any click
     * @param actions ordered list of Action entries (must have at least one)
     */
    public MultiActionItem(
            Component name,
            Material icon,
            TextColor color,
            BiConsumer<Player, Click> onClick,
            List<ClickAction> actions
    ) {
        if (actions == null || actions.isEmpty())
            throw new IllegalArgumentException("Must supply at least one action");
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.onClick = onClick;
        this.actions = new ArrayList<>(actions);
    }

    @Override
    public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
        TextColor gray = TextColor.color(0xAAAAAA);

        ItemBuilder builder = new ItemBuilder(icon)
                .setName(name.color(color).decoration(TextDecoration.ITALIC, false));

        actions.stream()
                .map(a -> {
                    Component line = Component.text(ClickAction.humanize(a.clickType) + ": ")
                            .color(gray)
                            .append(a.description.color(gray));

                    if (a.currentValue != null) {
                        line = line
                                .append(Component.text(" (v: ").color(gray))
                                .append(a.currentValue.color(gray))
                                .append(Component.text(")").color(gray));
                    }

                    return line;
                })
                .forEach(builder::addLoreLines);

        return builder;
    }


    @Override
    public void handleClick(
            @NotNull ClickType clickType,
            @NotNull Player player,
            @NotNull Click click
    ) {
        onClick.accept(player, click);
        notifyWindows();
    }
}
