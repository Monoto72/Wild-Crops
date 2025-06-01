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

import java.util.Objects;

public class VersionCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> builder() {
        return Commands.literal("version")
                .requires(src -> CommandUtils.isPermissible(src, "wildcrops.version"))
                .executes(VersionCommand::execute);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        String version = WildCrops.getInstance().getDescription().getVersion();
        String currentVersion = WildCrops.getInstance().getVersion();

        src.getSender().sendMessage(
                !Objects.equals(currentVersion, version) ?
                        ComponentUtils.warning("Outdated Version: " + currentVersion + " by Monoto") :
                        ComponentUtils.success("Version: " + version + " by Monoto")
        );


        return Command.SINGLE_SUCCESS;
    }
}