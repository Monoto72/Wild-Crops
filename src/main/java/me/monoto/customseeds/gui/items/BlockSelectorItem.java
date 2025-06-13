package me.monoto.customseeds.gui.items;

import me.monoto.customseeds.gui.windows.BlockSearchWindow;
import me.monoto.customseeds.utils.ClickAction;
import me.monoto.customseeds.utils.WindowHistory;
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

/**
 * An item which opens a block-picker window and uses shared ClickAction lore formatting.
 */
public class BlockSelectorItem extends AbstractItem {

    private final Component name;
    private final Material icon;
    private final TextColor color;
    private final BiConsumer<Player, Material> onSelect;
    private final List<Material> allowedMaterials;

    public BlockSelectorItem(
            String label,
            Material icon,
            TextColor labelColor,
            BiConsumer<Player, Material> onSelect,
            List<Material> allowedMaterials
    ) {
        this.name = Component.text(label);
        this.icon = icon;
        this.color = labelColor;
        this.onSelect = onSelect;
        this.allowedMaterials = allowedMaterials;
    }

    @Override
    public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
        ItemBuilder builder = new ItemBuilder(icon)
                .setName(name.color(color).decoration(TextDecoration.ITALIC, false));

        List<ClickAction> actions = List.of(
                new ClickAction(ClickType.LEFT, "Edit block", icon.name())
        );

        for (ClickAction action : actions) {
            Component prefix = Component.text(ClickAction.humanize(action.clickType) + ": ")
                    .color(TextColor.color(0xAAAAAA));

            Component line = prefix.append(
                    action.description.color(TextColor.color(0xAAAAAA))
            );

            if (action.currentValue != null) {
                line = line.append(
                                Component.text(" (v: ")
                                        .color(TextColor.color(0xAAAAAA))
                        )
                        .append(action.currentValue.color(TextColor.color(0xAAAAAA)))
                        .append(Component.text(")").color(TextColor.color(0xAAAAAA)));
            }

            builder.addLoreLines(line);
        }

        return builder;
    }

    @Override
    public void handleClick(
            @NotNull ClickType clickType,
            @NotNull Player player,
            @NotNull Click click
    ) {
        BiConsumer<Player, Material> boundSelect = (ignored, mat) -> onSelect.accept(player, mat);

        WindowHistory.replace(player, () ->
                BlockSearchWindow.createWindow(player, color, allowedMaterials, boundSelect)
        );
    }
}
