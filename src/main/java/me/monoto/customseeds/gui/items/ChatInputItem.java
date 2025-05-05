package me.monoto.customseeds.gui.items;

import me.monoto.customseeds.utils.ChatInput.ChatInputType;
import me.monoto.customseeds.utils.ChatInput.TypedChatInput;
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

public class ChatInputItem<T> extends AbstractItem {

    private final String label;
    private final Material icon;
    private final TextColor labelColor;
    private final String promptText;
    private final ChatInputType<T> inputType;
    private final BiConsumer<Player, T> onInput;

    public ChatInputItem(String label, Material icon, TextColor labelColor, String promptText, ChatInputType<T> inputType, BiConsumer<Player, T> onInput) {
        this.label = label;
        this.icon = icon;
        this.labelColor = labelColor;
        this.promptText = promptText;
        this.inputType = inputType;
        this.onInput = onInput;
    }

    @Override
    public @NotNull ItemProvider getItemProvider(@NotNull Player player) {
        return new ItemBuilder(icon)
                .setName(Component.text(label).color(labelColor).decoration(TextDecoration.ITALIC, false))
                .addLoreLines(Component.text("Click to enter input").color(TextColor.color(0x7393B3)).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, Player player, @NotNull Click click) {
        player.closeInventory();
        TypedChatInput.open(player, promptText, inputType, onInput);
    }

}
