package me.monoto.customseeds.gui;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.gui.items.*;
import me.monoto.customseeds.utils.*;
import me.monoto.customseeds.utils.ChatInput.ChatInputType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.Range;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.window.Window;

import java.util.List;
import java.util.Map;

/**
 * Main settings GUI for cropping configurations.
 */
public class CropSettingsGui {

    private final String cropType;

    public CropSettingsGui(String cropType) {
        this.cropType = cropType;
    }

    private Gui createGui(CropDefinition def) {
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

        // Preview Seed
        gui.setItem('1', Item.builder()
                .setItemProvider(new ItemBuilder(ItemManager.getSeed(cropType, 1)))
        );

        // Crop Name & Lore
        gui.setItem('a', new OpenGuiItem(
                "Crop Name & Lore",
                Material.NAME_TAG,
                TextColor.color(0x00AA00),
                player -> new CropTextGui(cropType).open(player)
        ));

        // Physical Drops
        gui.setItem('b', new OpenGuiItem(
                "Crop Drops",
                Material.CHEST,
                TextColor.color(0x00AA00),
                player -> new CropRewardsGui(cropType, true).open(player)
        ));

        // Command Drops
        gui.setItem('c', new OpenGuiItem(
                "Command Drops",
                Material.COMMAND_BLOCK,
                TextColor.color(0x00AA00),
                player -> new CropRewardsGui(cropType, false).open(player)
        ));

        // Auto Replant Toggle
        gui.setItem('h', new ToggleItem(
                "Auto Replant",
                Material.DIAMOND_HOE,
                TextColor.color(0x00FF00),
                def.isAutoReplantAllowed(),
                (player, click) -> {
                    CropConfigData data = WildCrops.getInstance()
                            .getFileManager().getCropData(cropType);
                    if (data != null) {
                        boolean newVal = !def.isAutoReplantAllowed();
                        data.getConfig().set("settings.auto_replant", newVal);
                        WildCrops.getInstance().getFileManager().saveCropConfig(cropType);
                        updateDefinition(data);
                        WindowHistory.replace(player, () -> createWindow(player));
                    }
                }
        ));

        // Bone Meal Toggle
        gui.setItem('i', new ToggleItem(
                "Bone Meal",
                Material.BONE_MEAL,
                TextColor.color(0x00FF00),
                def.isBonemealAllowed(),
                (player, click) -> {
                    CropConfigData data = WildCrops.getInstance()
                            .getFileManager().getCropData(cropType);
                    if (data != null) {
                        boolean newVal = !def.isBonemealAllowed();
                        data.getConfig().set("settings.bone_meal", newVal);
                        WildCrops.getInstance().getFileManager().saveCropConfig(cropType);
                        updateDefinition(data);
                        WindowHistory.replace(player, () -> createWindow(player));
                    }
                }
        ));

        // Grow Time
        gui.setItem('d', new MultiActionItem(
                Component.text("Grow Time"),
                Material.CLOCK,
                TextColor.color(0xFFAA00),
                this::handleGrowTimeClick,
                List.of(
                        new ClickAction(ClickType.LEFT,
                                "Edit value",
                                Formatters.time(def.getBaseGrowTime() / 20)
                        )
                )
        ));

        // Seed Material
        Range<Integer> seedRange = def.getSeedRewardRange();
        gui.setItem('e', new MultiActionItem(
                Component.text("Seed Material"),
                def.getSeedMaterial(),
                TextColor.color(0x00AA00),
                (player, click) -> {
                    if (click.clickType().isLeftClick()) {
                        new BlockSelectorItem(
                                "Select Seed Material",
                                def.getSeedMaterial(),
                                TextColor.color(0x00AA00),
                                this::handleSeedMaterialSelect,
                                BlockCache.getCrops()
                        ).handleClick(click.clickType(), player, click);
                    } else if (click.clickType().isRightClick()) {
                        handleRewardClick(player, click, "seed");
                    }
                },
                List.of(
                        new ClickAction(ClickType.LEFT, "Change seed material"),
                        new ClickAction(ClickType.RIGHT, "Edit seed reward amount", Component.text(seedRange.getMinimum() + "-" + seedRange.getMaximum()))
                )
        ));

        // EXP Reward
        Range<Integer> expRange = def.getExpRewardRange();
        gui.setItem('f', new MultiActionItem(
                Component.text("EXP Reward"),
                Material.EXPERIENCE_BOTTLE,
                TextColor.color(0xFFAA00),
                (player, click) -> handleRewardClick(player, click, "exp"),
                List.of(
                        new ClickAction(ClickType.LEFT,
                                "Edit value",
                                Component.text(expRange.getMinimum() + "-" + expRange.getMaximum())
                        )
                )
        ));

        // Final Block
        gui.setItem('g', new BlockSelectorItem(
                "Final Block",
                def.getFinalBlock(),
                TextColor.color(0x00AA00),
                this::handleFinalBlockSelect,
                BlockCache.getPlaceableItems()
        ));

        // Money Reward
        Range<Integer> moneyRange = def.getMoneyRewardRange();
        gui.setItem('k', new MultiActionItem(
                Component.text("Money Reward"),
                Material.GOLD_INGOT,
                TextColor.color(0xFFAA00),
                (player, click) -> handleRewardClick(player, click, "money"),
                List.of(
                        new ClickAction(ClickType.LEFT,
                                "Edit value",
                                Component.text(moneyRange.getMinimum() + "-" + moneyRange.getMaximum())
                        )
                )
        ));

        // McMMO Reward
        Range<Integer> mmRange = def.getMcMMORewardRange();
        gui.setItem('l', new MultiActionItem(
                Component.text("McMMO Reward"),
                Material.ENCHANTED_BOOK,
                TextColor.color(0xFFAA00),
                (player, click) -> handleRewardClick(player, click, "mcmmo_exp"),
                List.of(
                        new ClickAction(ClickType.LEFT,
                                "Edit value",
                                Component.text(mmRange.getMinimum() + "-" + mmRange.getMaximum())
                        )
                )
        ));

        // Light Level
        gui.setItem('j', new MultiActionItem(
                Component.text("Light Level"),
                def.getMinLight() == 0 ? Material.REDSTONE_LAMP : Material.GLOWSTONE,
                TextColor.color(0xFFAA00),
                this::handleMinimumLightLevelClick,
                List.of(
                        new ClickAction(ClickType.LEFT,
                                "Edit value",
                                Component.text(def.getMinLight())
                        )
                )
        ));

        return gui;
    }

    private void handleRewardClick(Player player, Click click, String type) {
        if (!(click.clickType().isLeftClick()
                || (click.clickType().isRightClick() && type.equals("seed")))) {
            return;
        }
        ChatInputItem<String> prompt = new ChatInputItem<>(
                type + " Reward",
                Material.CLOCK,
                TextColor.color(0xFFAA00),
                "Enter amount (min-max):",
                ChatInputType.STRING,
                (p, input) -> {
                    String text = input.trim();
                    int min, max;
                    if (text.matches("\\d+\\s*-\\s*\\d+")) {
                        String[] parts = text.split("\\s*-\\s*");
                        try {
                            min = Integer.parseInt(parts[0]);
                        } catch (NumberFormatException e) {
                            min = 1;
                        }
                        try {
                            max = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            max = min;
                        }
                    } else if (text.matches("\\d+")) {
                        min = max = Integer.parseInt(text);
                    } else {
                        // invalid input: fallback to previous value
                        Range<Integer> oldRange = CropDefinition.parseRange(text);
                        min = oldRange.getMinimum();
                        max = oldRange.getMaximum();
                    }
                    String amountStr = (min == max) ? String.valueOf(min) : (min + "-" + max);

                    List<Map<String, Object>> list = (List<Map<String, Object>>) (List<?>) WildCrops.getInstance()
                            .getFileManager().getCropData(cropType)
                            .getConfig().getMapList("rewards");
                    for (Map<String, Object> m : list) {
                        if (type.equals(m.get("type"))) {
                            m.put("amount", amountStr);
                            break;
                        }
                    }
                    CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
                    data.getConfig().set("rewards", list);
                    WildCrops.getInstance().getFileManager().saveCropConfig(cropType);
                    updateDefinition(data);
                    WindowHistory.replace(p, () -> createWindow(p));
                },
                () -> WindowHistory.replace(player, () -> createWindow(player))
        );
        prompt.handleClick(click.clickType(), player, click);
    }

    private void handleGrowTimeClick(Player player, Click click) {
        if (!click.clickType().isLeftClick()) return;
        new ChatInputItem<Integer>(
                "Grow Time",
                Material.CLOCK,
                TextColor.color(0xFFAA00),
                "Enter grow time in seconds (1 - 86400):",
                ChatInputType.MAX_INT(1, 86400),
                (p, time) -> {
                    CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
                    if (data != null) {
                        data.getConfig().set("settings.grow_time", time);
                        WildCrops.getInstance().getFileManager().saveCropConfig(cropType);
                        updateDefinition(data);
                    }
                    WindowHistory.replace(p, () -> createWindow(p));
                },
                () -> WindowHistory.replace(player, () -> createWindow(player))
        ).handleClick(ClickType.LEFT, player, click);
    }

    private void handleMinimumLightLevelClick(Player player, Click click) {
        if (!click.clickType().isLeftClick()) return;
        new ChatInputItem<Integer>(
                "Grow Time",
                Material.CLOCK,
                TextColor.color(0xFFAA00),
                "Enter a minimum light level (0 - 15):",
                ChatInputType.MAX_INT(0, 15),
                (p, time) -> {
                    CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
                    if (data != null) {
                        data.getConfig().set("settings.min_light_level", time);
                        WildCrops.getInstance().getFileManager().saveCropConfig(cropType);
                        updateDefinition(data);
                    }
                    WindowHistory.replace(p, () -> createWindow(p));
                },
                () -> WindowHistory.replace(player, () -> createWindow(player))
        ).handleClick(ClickType.LEFT, player, click);
    }

    private void handleSeedMaterialSelect(Player player, Material mat) {
        CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
        if (data != null) {
            data.getConfig().set("material", mat.name());
            WildCrops.getInstance().getFileManager().saveCropConfig(cropType);
            updateDefinition(data);
        }
        Bukkit.getScheduler().runTask(WildCrops.getInstance(),
                () -> WindowHistory.replace(player, () -> createWindow(player)));
    }

    private void handleFinalBlockSelect(Player player, Material mat) {
        CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
        if (data != null) {
            data.getConfig().set("settings.final_block", mat.name());
            WildCrops.getInstance().getFileManager().saveCropConfig(cropType);
            updateDefinition(data);
        }
        Bukkit.getScheduler().runTask(WildCrops.getInstance(),
                () -> WindowHistory.replace(player, () -> createWindow(player)));
    }

    private void updateDefinition(CropConfigData data) {
        CropDefinition newDef = CropDefinition.fromConfig(
                data.getFileNameWithoutExtension(),
                data.getConfig()
        );
        CropDefinitionRegistry.update(cropType, newDef);
    }

    public Window createWindow(Player player) {
        CropDefinition def = CropDefinitionRegistry.get(cropType);
        Gui gui = createGui(def);

        return Window.builder()
                .setViewer(player)
                .setUpperGui(gui)
                .setTitle("Crop Settings")
                .build(player);
    }

    public void open(Player player) {
        WindowHistory.open(player, () -> createWindow(player));
    }
}
