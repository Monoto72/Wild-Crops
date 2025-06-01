package me.monoto.customseeds.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import me.monoto.customseeds.commands.arguments.CropTypeArgument;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.utils.CommandUtils;
import me.monoto.customseeds.utils.ComponentUtils;
import me.monoto.customseeds.utils.ItemManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GiveCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> builder() {
        return Commands.literal("give")
                .requires(src -> CommandUtils.isPermissible(src, "wildcrops.give"))
                .then(Commands.argument("target", ArgumentTypes.player())
                        .then(Commands.argument("seed", CropTypeArgument.cropType())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304))
                                        .executes(GiveCommand::execute))));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack sourceStack = ctx.getSource();

        PlayerSelectorArgumentResolver resolver =
                ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
        Player target = resolver.resolve(sourceStack).getFirst();
        if (target == null) {
            sourceStack.getSender().sendMessage(
                    ComponentUtils.error("Could not find any online player matching that selector."));
            return 0;
        }

        CropDefinition definition = ctx.getArgument("seed", CropDefinition.class);
        int amount = ctx.getArgument("amount", Integer.class);
        ItemStack template = ItemManager.getSeed(definition.getId(), 1);

        int capacity = calculateRemainingCapacity(target.getInventory(), template);
        if (capacity < amount) {
            sourceStack.getSender().sendMessage(
                    ComponentUtils.error("Target only has room for " + capacity +
                            " more item" + (capacity == 1 ? "" : "s") + "."));
            return 0;
        }

        int fullStacks = amount / 64;
        int remainder = amount % 64;
        List<ItemStack> toGive = new ArrayList<>(fullStacks + (remainder > 0 ? 1 : 0));

        for (int i = 0; i < fullStacks; i++) {
            ItemStack stack64 = template.clone();
            stack64.setAmount(64);
            toGive.add(stack64);
        }
        if (remainder > 0) {
            ItemStack stackRem = template.clone();
            stackRem.setAmount(remainder);
            toGive.add(stackRem);
        }

        for (ItemStack stack : toGive) {
            target.getInventory().addItem(stack);
        }

        String senderMsg = String.format(
                "Gave %d× %s Seeds to %s (using %d stack%s).",
                amount,
                definition.getType(),
                target.getName(),
                toGive.size(),
                (toGive.size() == 1 ? "" : "s")
        );
        sourceStack.getSender().sendMessage(ComponentUtils.success(senderMsg));

        String targetMsg = String.format(
                "You have received %d× %s Seeds from %s.",
                amount,
                definition.getType(),
                sourceStack.getSender().getName()
        );
        target.sendMessage(ComponentUtils.success(targetMsg));

        return Command.SINGLE_SUCCESS;
    }

    private static int calculateRemainingCapacity(@NotNull Inventory inv, @NotNull ItemStack template) {
        int total = 0;
        for (ItemStack slotStack : inv.getStorageContents()) {
            if (slotStack == null || slotStack.getType() == Material.AIR) {
                total += 64;
            } else if (slotStack.isSimilar(template)) {
                int currentAmount = slotStack.getAmount();
                if (currentAmount < 64) {
                    total += (64 - currentAmount);
                }
            }
        }
        return total;
    }
}