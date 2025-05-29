package me.monoto.customseeds.utils.ChatInput;

import me.monoto.customseeds.WildCrops;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

public class TypedChatInput {
    public static <T> void open(
            Player player,
            String prompt,
            ChatInputType<T> type,
            BiConsumer<Player, T> onSuccess,
            Runnable onCancel
    ) {
        WildCrops.getInstance().getChatInput().openChatInput(
                player,
                prompt,
                input -> {
                    try {
                        T parsed = type.parse(input);
                        onSuccess.accept(player, parsed);
                    } catch (IllegalArgumentException ex) {
                        player.sendMessage("§c" + ex.getMessage());
                    }
                },
                onCancel
        );
    }
}
