package me.monoto.customseeds.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.utils.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class CropTypeArgument implements CustomArgumentType.Converted<CropDefinition, String> {
    private static final DynamicCommandExceptionType ERROR_INVALID_CROPTYPE =
            new DynamicCommandExceptionType(cropType -> MessageComponentSerializer.message().serialize(
                    ComponentUtils.PREFIX.append(
                            Component.text("CropType (" + cropType + ") does not exist!")
                    )
            ));

    @Override
    public @NotNull CropDefinition convert(String nativeType) throws CommandSyntaxException {
        // Perform case‐insensitive search across all registered CropDefinitions.
        String lookup = nativeType.toLowerCase(Locale.ROOT);

        for (CropDefinition def : CropDefinitionRegistry.getDefinitions().values()) {
            if (def.getType().equalsIgnoreCase(lookup)) {
                return def;
            }
        }

        // If nothing matched, throw the error
        throw ERROR_INVALID_CROPTYPE.create(nativeType);
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(
            CommandContext<S> ctx,
            SuggestionsBuilder builder
    ) {
        // Suggest every registered definition.getType() that starts with the current partial
        String remaining = builder.getRemainingLowerCase();
        for (CropDefinition def : CropDefinitionRegistry.getDefinitions().values()) {
            String type = def.getType();
            if (type.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(type);
            }
        }
        return builder.buildFuture();
    }

    /**
     * Static factory method (used by Brigadier command registration).
     */
    public static CropTypeArgument cropType() {
        return new CropTypeArgument();
    }
}
