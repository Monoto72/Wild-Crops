package me.monoto.customseeds;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import me.monoto.customseeds.crops.*;
import me.monoto.customseeds.gui.CropSeedLayoutMenuHolder;
import me.monoto.customseeds.utils.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class WildCropsCommand {
    // Surely we clean this up later, maybe even make it a command manager or something
    // But for now, this is fine
    // Might also need to redeclare a hierarchy for perms as we removed the old plugin.yml
    public static LiteralArgumentBuilder<CommandSourceStack> commandRoot = Commands.literal("wildcrops")
            .requires(source -> isPermissible(source, "wildcrops.use"))
            .executes(WildCropsCommand::helpCommand)
            .then(Commands.literal("reload")
                    .requires(source -> isPermissible(source, "wildcrops.reload"))
                    .executes(WildCropsCommand::reloadCommand))
            .then(Commands.literal("give")
                    .requires(source -> isPermissible(source, "wildcrops.give"))
                    .then(Commands.argument("target", ArgumentTypes.player())
                            .then(Commands.argument("seed", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        for (CropDefinition definition : CropDefinitionRegistry.getDefinitions().values()) {
                                            builder.suggest(definition.getType());
                                        }
                                        return builder.buildFuture();
                                    })
                                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304))
                                            .executes(WildCropsCommand::giveCommand)))))
            .then(Commands.literal("settings")
                    .requires(source -> isPermissible(source, "wildcrops.settings"))
                    .executes(WildCropsCommand::settingsCommand))
            .then(Commands.literal("debug")
                    .requires(source -> isPermissible(source, "wildcrops.debug"))
                    .executes(WildCropsCommand::debugCommand));

    private static int helpCommand(CommandContext<CommandSourceStack> ctx) {
        // Build a fancy help message using Adventure components.
        TextComponent help = Component.text("")
                .append(Component.text("WildCrops Help\n")
                        .color(TextColor.color(0x00FF00))
                        .decorate(TextDecoration.BOLD))
                .append(Component.text("/wildcrops reload ")
                        .color(TextColor.color(0xFFFF00))
                        .hoverEvent(Component.text("wildcrops.reload").color(TextColor.color(0x808080)))
                        .clickEvent(ClickEvent.suggestCommand("/wildcrops reload")))
                .append(Component.text(" - Reloads the plugin.\n")
                        .color(TextColor.color(0xAAAAAA)))
                .append(Component.text("/wildcrops give <player> <seed> <amount> ")
                        .color(TextColor.color(0xFFFF00))
                        .hoverEvent(Component.text("wildcrops.give").color(TextColor.color(0x808080)))
                        .clickEvent(ClickEvent.suggestCommand("/wildcrops give ")))
                .append(Component.text(" - Gives seeds to a player.\n")
                        .color(TextColor.color(0xAAAAAA)))
                .append(Component.text("/wildcrops settings ")
                        .color(TextColor.color(0xFFFF00))
                        .hoverEvent(Component.text("wildcrops.settings").color(TextColor.color(0x808080)))
                        .clickEvent(ClickEvent.suggestCommand("/wildcrops settings")))
                .append(Component.text(" - Opens your settings menu.\n")
                        .color(TextColor.color(0xAAAAAA)));
        ctx.getSource().getSender().sendMessage(help);
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadCommand(CommandContext<CommandSourceStack> ctx) {
        WildCrops.getInstance().getFileManager().reloadCropConfigs();
        ctx.getSource().getSender().sendMessage(Component.text("Plugin reloaded!"));
        return Command.SINGLE_SUCCESS;
    }

    private static int giveCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
        final Player target = targetResolver.resolve(ctx.getSource()).getFirst();

        String seedType = StringArgumentType.getString(ctx, "seed").toLowerCase();
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        CropDefinition definition = CropDefinition.getDefinitionByCropType(seedType);

        if (definition == null) {
            ctx.getSource().getSender().sendMessage(Component.text("Invalid seed type."));
            return 0;
        }

        if (!hasAvailableSlot(target)) {
            ctx.getSource().getSender().sendMessage(Component.text("Target player's inventory is full."));
            return 0;
        }

        ctx.getSource().getSender().sendMessage(definition.getType());
        if (definition == null) return 0;

        ItemStack seed = ItemManager.getSeed(definition.getId(), amount);
        target.getInventory().addItem(seed);
        ctx.getSource().getSender().sendMessage(Component.text("Gave " + amount + " " + seedType + " seed(s) to " + target.getName() + "."));
        return Command.SINGLE_SUCCESS;
    }

    private static int settingsCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender(); // Retrieve the command sender
        Entity executor = ctx.getSource().getExecutor();

        if (!(executor instanceof Player player)) {
            sender.sendPlainMessage("Only players can run this command!");
            return Command.SINGLE_SUCCESS;
        }

        new CropSeedLayoutMenuHolder().open(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int debugCommand(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender(); // Retrieve the command sender
        Entity executor = ctx.getSource().getExecutor();

        if (!(executor instanceof Player player)) {
            sender.sendPlainMessage("Only players can run this command!");
            return Command.SINGLE_SUCCESS;
        }

        Block block = player.getTargetBlockExact(5);
        if (block == null) {
            player.sendMessage(Component.text("No block in sight!").color(TextColor.color(0xFF474C)));
            return Command.SINGLE_SUCCESS;
        }

        CropData data = CropUtils.getCropData(block);
        if (data == null) {
            player.sendMessage(Component.text("No crop found!").color(TextColor.color(0xFF474C)));
            return Command.SINGLE_SUCCESS;
        }

        CropDefinition definition = CropDefinitionRegistry.get(data.getCropType());

        if (definition == null) {
            player.sendMessage(Component.text("No crop definition found!").color(TextColor.color(0xFF474C)));
            return Command.SINGLE_SUCCESS;
        }

        player.sendMessage(Component.text("Crop Type: " + data.getCropType()).color(TextColor.color(0x00FF00)));
        player.sendMessage(Component.text("Age: " + data.getAge()).color(TextColor.color(0x00FF00)));
        player.sendMessage(Component.text("Progress: " + data.getProgress()).color(TextColor.color(0x00FF00)));
        player.sendMessage(Component.text("Is Fully Grown: " + data.isFullyGrown()).color(TextColor.color(0x00FF00)));

        return Command.SINGLE_SUCCESS;
    }


    /**
     * Checks if the player has at least one available inventory slot.
     */
    private static boolean hasAvailableSlot(Player player) {
        return Arrays.stream(player.getInventory().getContents()).anyMatch(item -> item == null);
    }

    private static boolean isPermissible(CommandSourceStack source, String permission) {
        return source.getSender().hasPermission("customcrops.command")
                || source.getSender().hasPermission("customcrops.admin")
                || source.getSender().isOp()
                || source.getSender() instanceof ConsoleCommandSender;
    }
}
