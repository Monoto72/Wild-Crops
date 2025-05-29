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
     * @param onCancel the action to perform if the player cancels the input
     *                 <p>
     *                 This method will soon be deprecated in favor of a more generic Dialog Context Menus from 1.21.6
     *                 For now we use a cheap hacky way to get the input from the player
     */
    public void openChatInput(
            Player player,
            String prompt,
            Consumer<String> callback,
            Runnable onCancel
    ) {
        player.closeInventory();

        ConversationFactory factory = new ConversationFactory(plugin)
                .withModality(true)
                .thatExcludesNonPlayersWithMessage("Only players can answer this!")
                .withLocalEcho(false)
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public @NotNull String getPromptText(@NotNull ConversationContext context) {
                        return prompt + " (type '!exit' or '!cancel' to go back)";
                    }

                    @Override
                    public Prompt acceptInput(@NotNull ConversationContext context, String input) {
                        // cancel cases:
                        if (input.equalsIgnoreCase("!exit") || input.equalsIgnoreCase("!cancel")) {
                            context.getForWhom().sendRawMessage("§cCanceled, returning to menu…");
                            onCancel.run();
                            return Prompt.END_OF_CONVERSATION;
                        }
                        // normal case:
                        callback.accept(input);
                        return Prompt.END_OF_CONVERSATION;
                    }
                });

        Conversation Conversation = factory.buildConversation(player);
        Conversation.begin();
    }
}
