package me.monoto.customseeds.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.monoto.customseeds.utils.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class HelpCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> builder(Map<String, String> usageMap) {
        return Commands.literal("help")
                .executes(ctx -> runHelp(ctx, usageMap));
    }

    private static int runHelp(CommandContext<CommandSourceStack> ctx,
                               Map<String, String> usageMap) {
        CommandSender sender = ctx.getSource().getSender();

        sender.sendMessage(
                ComponentUtils.PREFIX.append(
                        Component.text("Commands", TextColor.color(0xFFB052))
                                .decoration(TextDecoration.BOLD, true)
                )
        );

        for (Map.Entry<String, String> entry : usageMap.entrySet()) {
            String cmd = entry.getKey();
            String desc = entry.getValue();

            String perm = switch (cmd) {
                case "reload" -> "wildcrops.reload";
                case "settings" -> "wildcrops.admin";
                case "give" -> "wildcrops.give";
                default -> null;
            };

            if (perm != null && !sender.hasPermission(perm)) {
                continue;
            }

            String displayCommand = cmd.isEmpty() ? "/wildcrops" : "/wildcrops " + cmd;

            Component clickableCmd = Component.text(displayCommand, TextColor.color(0xFFD700))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("Click to suggest", TextColor.color(0xAAAAAA))
                    ))
                    .clickEvent(ClickEvent.suggestCommand(displayCommand));

            Component line = Component.text(" • ", TextColor.color(0xCCCCCC))
                    .append(clickableCmd)
                    .append(Component.text(" — " + desc, TextColor.color(0xEEEEEE)));

            sender.sendMessage(line);
        }

        return 1;
    }
}
