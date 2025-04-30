package me.monoto.customseeds.gui;

import dev.triumphteam.gui.paper.Gui;
import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.gui.buttons.ChatInputButton;
import me.monoto.customseeds.gui.buttons.SelectorButton;
import me.monoto.customseeds.gui.buttons.ToggleButton;
import me.monoto.customseeds.utils.BlockCache;
import me.monoto.customseeds.utils.ChatInput.ChatInputType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class CropSettingsGui {

    private final CropDefinition def;
    private final String cropType;
    private final Gui gui;

    public CropSettingsGui(String cropType) {
        this.cropType = cropType;
        this.def = CropDefinitionRegistry.get(cropType);
        this.gui = createGui();
    }

    private Gui createGui() {
        return Gui.of(5)
            .title(Component.text("Crop Settings"))
            .component(component -> {
                final var autoReplant = component.remember(false);
                final var boneMeal = component.remember(false);

                component.render(container -> {
                    container.setItem(14, ToggleButton.create(
                            "Auto Replant",
                            Material.DIAMOND_HOE,
                            TextColor.color(0x00FF00),
                            autoReplant,
                            (player, context) -> {
                                player.sendMessage(Component.text("Replant: " + autoReplant));
                            }
                    ));

                    container.setItem(15, ToggleButton.create(
                            "Bone Meal",
                            Material.BONE_MEAL,
                            TextColor.color(0x00FF00),
                            boneMeal,
                            (player, context) -> {
                                player.sendMessage(Component.text("Bone Meal: " + boneMeal));
                            }
                    ));
                });
            }).statelessComponent(container -> {
                container.setItem(13, ChatInputButton.create(
                        "Set Grow Time",
                        Material.CLOCK,
                        TextColor.color(0xFFAA00),
                        "Enter grow time in seconds (e.g., 1 - 86400):",
                        ChatInputType.MAX_INT(1, 86400),
                        (player1, time) -> {
                            CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
                            if (data != null) {
                                data.getConfig().set("grow_time", time);
                                WildCrops.getInstance().getFileManager().saveCropConfig(cropType);

                                CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), data.getConfig());
                                CropDefinitionRegistry.update(cropType, newDef);
                            }

                            new CropSettingsGui(cropType).open(player1);
                        }
                ));

                container.setItem(12, SelectorButton.create(
                        "Set a Final Block",
                        def.getFinalBlock(),
                        TextColor.color(0x00AA00),
                        "Choose a Final Block",
                        BlockCache.getPlaceableBlocks(),
                        (player, selectedItem) -> {
                            CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
                            if (data != null) {
                                data.getConfig().set("growth.final_block", selectedItem.getType().name());
                                WildCrops.getInstance().getFileManager().saveCropConfig(cropType);

                                CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), data.getConfig());
                                CropDefinitionRegistry.update(cropType, newDef);
                            }
                        },
                        cropType
                ));
            }).build();
    }

    public String getCropType() {
        return cropType;
    }

    public void open(Player player) {
        gui.open(player);
    }
}
