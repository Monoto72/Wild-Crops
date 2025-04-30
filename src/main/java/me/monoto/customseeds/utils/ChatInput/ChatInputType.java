package me.monoto.customseeds.utils.ChatInput;

import java.util.function.Function;

public class ChatInputType<T> {

    public static final ChatInputType<String> STRING = new ChatInputType<>(s -> s);
    public static final ChatInputType<Integer> INT = new ChatInputType<>(input -> {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid whole number.");
        }
    });

    public static ChatInputType<Integer> MAX_INT(int min, int max) {
        return new ChatInputType<>(input -> {
            try {
                int val = Integer.parseInt(input);
                if (val < min || val > max) {
                    throw new IllegalArgumentException("Enter a number between " + min + " and " + max + ".");
                }
                return val;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Please enter a valid number.");
            }
        });
    }

    private final Function<String, T> parser;

    public ChatInputType(Function<String, T> parser) {
        this.parser = parser;
    }

    public T parse(String input) {
        return parser.apply(input);
    }
}
