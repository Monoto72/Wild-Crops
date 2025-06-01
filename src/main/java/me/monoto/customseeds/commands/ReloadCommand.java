package me.monoto.customseeds.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.utils.CommandUtils;
import me.monoto.customseeds.utils.ComponentUtils;
import net.kyori.adventure.text.format.TextDecoration;

public class ReloadCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> builder() {
        return Commands.literal("reload")
                .requires(src -> CommandUtils.isPermissible(src, "wildcrops.reload"))
                .executes(ReloadCommand::execute);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        try {
            WildCrops.getInstance().getFileManager().reloadCropConfigs();

            src.getSender().sendMessage(
                    ComponentUtils.success("Configuration and data have been reloaded!")
                            .decoration(TextDecoration.ITALIC, false)
            );
        } catch (Exception e) {
            src.getSender().sendMessage(
                    ComponentUtils.error("An error occurred while reloading: " + e.getMessage())
            );
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }
        return Command.SINGLE_SUCCESS;
    }
}