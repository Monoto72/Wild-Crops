package me.monoto.customseeds.gui;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.gui.items.BlockSelectorItem;
import me.monoto.customseeds.gui.items.ChatInputItem;
import me.monoto.customseeds.gui.items.FillerItem;
import me.monoto.customseeds.utils.BlockCache;
import me.monoto.customseeds.utils.ChatInput.ChatInputType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.window.Window;

import java.util.List;

public class CropRewardsGui {

    private final String cropType;
    private final boolean isItemMenu;
    private final Gui gui;

    public CropRewardsGui(String cropType, boolean isItemMenu) {
        this.cropType = cropType;
        this.isItemMenu = isItemMenu;
        this.gui = createGui();
    }

    private Gui createGui() {
        Gui gui = Gui.builder()
                .setStructure(
                        "# # # # # # # # #",
                        "# a b c d e f g #",
                        "# h i j k l m n #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', new FillerItem(Material.GRAY_STAINED_GLASS_PANE))
                .build();

        CropDefinition def = CropDefinitionRegistry.get(cropType);
        List<CropDefinition.Reward> rewards = def.getRewards().stream()
                .filter(r -> (isItemMenu ? r.type().equalsIgnoreCase("item") : r.type().equalsIgnoreCase("command")))
                .toList();

        char[] rewardSlots = {
                'a', 'b', 'c', 'd', 'e', 'f', 'g',
                'h', 'i', 'j', 'k', 'l', 'm', 'n'
        };

        int idx = 0;
        for (CropDefinition.Reward reward : rewards) {
            if (isItemMenu) {
                if (idx >= rewardSlots.length) break;  // Safety check
                Material mat = reward.material();
                gui.setItem(rewardSlots[idx], new BlockSelectorItem(
                        "Reward: " + mat.name(),
                        mat,
                        TextColor.color(0x00FF00),
                        (player, selected) -> {
                            player.sendMessage(Component.text("Updated reward to: " + selected.name()));
                            Bukkit.getScheduler().runTask(WildCrops.getInstance(),
                                    () -> new CropRewardsGui(cropType, true).open(player));
                        },
                        BlockCache.getPlaceableItems()
                ));
                idx++;
            } else {
                // 💥 Loop over commands inside the reward
                List<String> commands = reward.commands();
                if (commands == null || commands.isEmpty()) continue;

                for (String command : commands) {
                    if (idx >= rewardSlots.length) break;
                    gui.setItem(rewardSlots[idx], new ChatInputItem<>(
                            "Command: /" + (command.length() > 20 ? command.substring(0, 20) + "..." : command),
                            Material.PAPER,
                            TextColor.color(0x00FF00),
                            "Edit command (currently: /" + command + "):",
                            ChatInputType.STRING,
                            (player, input) -> {
                                // TODO: Implement logic to update this specific command
                                player.sendMessage(Component.text("Updated command: /" + input));
                                Bukkit.getScheduler().runTask(WildCrops.getInstance(),
                                        () -> new CropRewardsGui(cropType, false).open(player));
                            }
                    ));
                    idx++;
                }
            }
        }

        // Add "Add Reward" if room left
        if (idx < rewardSlots.length) {
            if (isItemMenu) {
                gui.setItem(rewardSlots[idx], new BlockSelectorItem(
                        "Add New Reward",
                        Material.LIME_DYE,
                        TextColor.color(0x00FF00),
                        (player, selected) -> {
                            // 🔥 Add new item reward logic
                            player.sendMessage(Component.text("Added new reward: " + selected.name()));
                            Bukkit.getScheduler().runTask(WildCrops.getInstance(),
                                    () -> new CropRewardsGui(cropType, true).open(player));
                        },
                        BlockCache.getCrops()
                ));
            } else {
                gui.setItem(rewardSlots[idx], new ChatInputItem<>(
                        "Add Command Reward",
                        Material.LIME_DYE,
                        TextColor.color(0x00FF00),
                        "Enter command to add (without /):",
                        ChatInputType.STRING,
                        (player, input) -> {
                            // 🔥 Add new command reward logic
                            player.sendMessage(Component.text("Added new command: /" + input));
                            Bukkit.getScheduler().runTask(WildCrops.getInstance(),
                                    () -> new CropRewardsGui(cropType, false).open(player));
                        }
                ));
            }
        }

        return gui;
    }


    public void open(Player player) {
        Window.builder()
                .setViewer(player)
                .setUpperGui(gui)
                .setTitle(isItemMenu ? "Item Rewards" : "Command Rewards")
                .open(player);
    }
}
