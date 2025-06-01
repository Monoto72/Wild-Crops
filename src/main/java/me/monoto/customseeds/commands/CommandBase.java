package me.monoto.customseeds.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.gui.CropSeedLayoutGui;
import me.monoto.customseeds.utils.CommandUtils;
import me.monoto.customseeds.utils.ComponentUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public class CommandBase {

    public CommandBase(WildCrops plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Map<String, String> usage = new LinkedHashMap<>();

            usage.put("", "Opens the crop settings GUI");
            usage.put("help", "Shows this help menu");
            usage.put("reload", "Reloads configuration and data");
            usage.put("give", "Gives a crop seed to a player");
            usage.put("version", "Shows the current version");

            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("wildcrops")
                    .requires(src -> CommandUtils.isPermissible(src, "wildcrops.settings"))
                    .executes(CommandBase::openGui)
                    .then(HelpCommand.builder(usage))
                    .then(ReloadCommand.builder())
                    .then(GiveCommand.builder())
                    .then(VersionCommand.builder());

            event.registrar().register(root.build());
        });
    }

    private static int openGui(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        CommandSender sender = src.getSender();
        Entity exec = src.getExecutor();

        if (!(exec instanceof Player player)) {
            sender.sendMessage(ComponentUtils.error("Only players can use this command!"));
            return Command.SINGLE_SUCCESS;
        }

        new CropSeedLayoutGui(0).open(player);
        return Command.SINGLE_SUCCESS;
    }
}
