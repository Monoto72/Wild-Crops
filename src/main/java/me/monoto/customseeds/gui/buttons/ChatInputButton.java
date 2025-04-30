package me.monoto.customseeds.gui.buttons;

import dev.triumphteam.gui.item.GuiItem;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import me.monoto.customseeds.utils.ChatInput.ChatInputType;
import me.monoto.customseeds.utils.ChatInput.TypedChatInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;

public class ChatInputButton {

    public static <T> GuiItem<Player, ItemStack> create(
            String label,
            Material icon,
            TextColor labelColor,
            String promptText,
            ChatInputType<T> inputType,
            BiConsumer<Player, T> onInput
    ) {
        return ItemBuilder.from(icon)
                .name(Component.text(label)
                        .color(labelColor)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Click to enter input")
                                .color(TextColor.color(0x7393B3))
                                .decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem((player, context) -> {
                    player.closeInventory();
                    TypedChatInput.open(player, promptText, inputType, onInput);
                });
    }
}
