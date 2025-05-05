package me.monoto.customseeds.gui;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.gui.items.BlockSelectorItem;
import me.monoto.customseeds.gui.items.ChatInputItem;
import me.monoto.customseeds.gui.items.FillerItem;
import me.monoto.customseeds.gui.items.ToggleItem;
import me.monoto.customseeds.utils.BlockCache;
import me.monoto.customseeds.utils.ChatInput.ChatInputType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.window.Window;


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
        Gui gui = Gui.builder()
                .setStructure(
                        "# # # # # # # # #",
                        "# 1 # a b c d e #",
                        "# # # f g h i j #",
                        "# # # k l m n o #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', new FillerItem(Material.GRAY_STAINED_GLASS_PANE))
                .build();
        // Toggle Items
        gui.setItem('a', new ToggleItem("Auto Replant", Material.DIAMOND_HOE, TextColor.color(0x00FF00), false,
                (player, click) -> player.sendMessage(Component.text("Toggled Auto Replant."))));

        gui.setItem('b', new ToggleItem("Bone Meal", Material.BONE_MEAL, TextColor.color(0x00FF00), false,
                (player, click) -> player.sendMessage(Component.text("Toggled Bone Meal."))));

        // Grow Time Input
        gui.setItem('c', new ChatInputItem<>(
                "Grow Time",
                Material.CLOCK,
                TextColor.color(0xFFAA00),
                "Enter grow time in seconds (1 - 86400):",
                ChatInputType.MAX_INT(1, 86400),
                (player, time) -> {
                    CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
                    if (data != null) {
                        data.getConfig().set("grow_time", time);
                        WildCrops.getInstance().getFileManager().saveCropConfig(cropType);
                        CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), data.getConfig());
                        CropDefinitionRegistry.update(cropType, newDef);
                    }
                    Bukkit.getScheduler().runTask(WildCrops.getInstance(), () -> new CropSettingsGui(cropType).open(player));
                }));

        // Seed Item
        gui.setItem('d', new BlockSelectorItem(
                "Seed Item",
                def.getSeedMaterial(),
                TextColor.color(0x00AA00),
                (player, selected) -> {
                    CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
                    if (data != null) {
                        data.getConfig().set("material", selected.name());
                        WildCrops.getInstance().getFileManager().saveCropConfig(cropType);
                        CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), data.getConfig());
                        CropDefinitionRegistry.update(cropType, newDef);
                    }
                    Bukkit.getScheduler().runTask(WildCrops.getInstance(), () -> new CropSettingsGui(cropType).open(player));
                },
                BlockCache.getCrops()
        ));

        // EXP Reward Input
        gui.setItem('e', new ChatInputItem<>(
                "EXP Reward",
                Material.CLOCK,
                TextColor.color(0xFFAA00),
                "Enter EXP reward (e.g., 300):",
                ChatInputType.INT,
                (player, exp) -> {
                    // TODO: store exp
                }));

        // Final Block Selector
        gui.setItem('f', new BlockSelectorItem(
                "Final Block",
                def.getFinalBlock(),
                TextColor.color(0x00AA00),
                (player, selected) -> {
                    CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
                    if (data != null) {
                        data.getConfig().set("growth.final_block", selected.name());
                        WildCrops.getInstance().getFileManager().saveCropConfig(cropType);
                        CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), data.getConfig());
                        CropDefinitionRegistry.update(cropType, newDef);
                    }
                    Bukkit.getScheduler().runTask(WildCrops.getInstance(), () -> new CropSettingsGui(cropType).open(player));
                },
                BlockCache.getPlaceableItems()
        ));

        // Money Reward
        gui.setItem('g', new ChatInputItem<>(
                "Money Reward",
                Material.GOLD_INGOT,
                TextColor.color(0xFFAA00),
                "Enter money reward (e.g., 300):",
                ChatInputType.INT,
                (player, amount) -> {
                    // TODO: store amount

                    Bukkit.getScheduler().runTask(WildCrops.getInstance(), () -> new CropSettingsGui(cropType).open(player));
                }));

        // McMMO Reward
        gui.setItem('h', new ChatInputItem<>(
                "McMMO Reward",
                Material.ENCHANTED_BOOK,
                TextColor.color(0xFFAA00),
                "Enter McMMO EXP reward (e.g., 300):",
                ChatInputType.INT,
                (player, exp) -> {
                    // TODO: store McMMO reward

                    Bukkit.getScheduler().runTask(WildCrops.getInstance(), () -> new CropSettingsGui(cropType).open(player));
                }));

        return gui;
    }

    public void open(Player player) {
        Window.builder()
                .setViewer(player)
                .setUpperGui(gui)
                .setTitle("Crop Settings")
                .open(player);
    }

    public String getCropType() {
        return cropType;
    }
}
