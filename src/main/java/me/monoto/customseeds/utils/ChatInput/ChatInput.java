package me.monoto.customseeds.utils.ChatInput;

import me.monoto.customseeds.WildCrops;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ChatInput {

    private final WildCrops plugin;

    public ChatInput(WildCrops plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens a chat prompt for the given player with a custom prompt message.
     * When the player enters text in chat, the callback is invoked with that input.
     *
     * @param player   the player to prompt
     * @param prompt   the message to display to the player
     * @param callback the callback to process the player's input
     */
    public void openChatInput(Player player, String prompt, Consumer<String> callback) {
        player.closeInventory();

        ConversationFactory factory = new ConversationFactory(plugin)
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public @NotNull String getPromptText(@NotNull ConversationContext context) {
                        return prompt;
                    }

                    @Override
                    public Prompt acceptInput(@NotNull ConversationContext context, String input) {
                        callback.accept(input);
                        return Prompt.END_OF_CONVERSATION;
                    }
                })
            .withLocalEcho(false); // Hide player's input from themselves
        Conversation conversation = factory.buildConversation(player);
        conversation.begin();
    }
}
