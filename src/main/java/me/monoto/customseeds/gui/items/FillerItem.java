package me.monoto.customseeds.gui.items;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

public class FillerItem extends AbstractItem {

    private final Material material;

    public FillerItem(Material material) {
        this.material = material;
    }

    @Override
    public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
        return new ItemBuilder(material)
                .setName(Component.text(" "))
                .hideTooltip(true);
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {

    }
}
