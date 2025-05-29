package me.monoto.customseeds.gui;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.gui.items.ChatInputItem;
import me.monoto.customseeds.gui.items.FillerItem;
import me.monoto.customseeds.gui.items.MultiActionItem;
import me.monoto.customseeds.utils.ChatInput.ChatInputType;
import me.monoto.customseeds.utils.ClickAction;
import me.monoto.customseeds.utils.Formatters;
import me.monoto.customseeds.utils.WindowHistory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;

public class CropTextGui {
    private final String cropId;
    private static final char[] SLOTS = {'a', 'b', 'c', 'd', 'e'};

    public CropTextGui(String cropId) {
        this.cropId = cropId;
    }

    private Gui createGui(List<String> loreLines, String displayName) {
        Gui gui = Gui.builder()
                .setStructure(
                        "# # # # # # # # #",
                        "# 1 # a b c d e #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', new FillerItem(Material.BLACK_STAINED_GLASS_PANE))
                .build();

        // Display Name editor
        gui.setItem('1', new MultiActionItem(
                Component.text("Display Name"),
                Material.NAME_TAG,
                TextColor.color(0x00AAFF),
                (player, click) -> {
                    if (click.clickType().isLeftClick()) {
                        ChatInputItem<String> prompt = new ChatInputItem<>(
                                "Display Name",
                                Material.NAME_TAG,
                                TextColor.color(0x00AAFF),
                                "Enter display name (MiniMessage):",
                                ChatInputType.STRING,
                                (p, input) -> {
                                    CropConfigData data = WildCrops.getInstance()
                                            .getFileManager().getCropData(cropId);
                                    if (data != null) {
                                        data.getConfig().set("display_name", input);
                                        WildCrops.getInstance().getFileManager().saveCropConfig(cropId);
                                    }
                                    WindowHistory.replace(p, () -> createWindow(p));
                                },
                                () -> WindowHistory.replace(player, () -> createWindow(player))
                        );
                        prompt.handleClick(ClickType.LEFT, player, click);
                    }
                },
                List.of(new ClickAction(
                        ClickType.LEFT,
                        "Edit value",
                        Formatters.format(displayName)
                ))
        ));

        int existingCount = loreLines.size();
        int slotsToShow = Math.min(existingCount + 1, SLOTS.length);

        for (int i = 0; i < slotsToShow; i++) {
            char slot = SLOTS[i];
            int index = i;
            boolean isAddSlot = (index == existingCount);
            String current = isAddSlot ? null : loreLines.get(index);

            Material icon = isAddSlot ? Material.GREEN_DYE : Material.PAPER;
            Component title = Component.text(isAddSlot ? "Add Lore" : "Lore #" + (index + 1));

            List<ClickAction> actions = new ArrayList<>();
            actions.add(new ClickAction(
                    ClickType.LEFT,
                    isAddSlot ? "Add lore" : "Edit value",
                    isAddSlot ? null : Formatters.format(current)
            ));
            if (!isAddSlot) {
                actions.add(
                        new ClickAction(ClickType.RIGHT, "Delete lore")
                );
            }

            gui.setItem(slot, new MultiActionItem(
                    title,
                    icon,
                    TextColor.color(0xFFAA00),
                    (player, click) -> {
                        if (click.clickType().isLeftClick()) {
                            ChatInputItem<String> prompt = new ChatInputItem<>(
                                    "Lore Line " + (index + 1),
                                    icon,
                                    TextColor.color(0x00AA00),
                                    "Enter lore text (MiniMessage):",
                                    ChatInputType.STRING,
                                    (p, input) -> {
                                        CropConfigData data = WildCrops.getInstance()
                                                .getFileManager().getCropData(cropId);
                                        if (data != null) {
                                            List<String> updated = new ArrayList<>(
                                                    data.getConfig().getStringList("lore")
                                            );
                                            if (isAddSlot) {
                                                updated.add(input);
                                            } else {
                                                updated.set(index, input);
                                            }
                                            data.getConfig().set("lore", updated);
                                            WildCrops.getInstance().getFileManager().saveCropConfig(cropId);
                                        }
                                        WindowHistory.replace(p, () -> createWindow(p));
                                    },
                                    () -> WindowHistory.replace(player, () -> createWindow(player))
                            );
                            prompt.handleClick(ClickType.LEFT, player, click);

                        } else if (click.clickType().isRightClick() && !isAddSlot) {
                            CropConfigData data = WildCrops.getInstance()
                                    .getFileManager().getCropData(cropId);
                            if (data != null) {
                                List<String> updated = new ArrayList<>(
                                        data.getConfig().getStringList("lore")
                                );
                                if (index < updated.size()) {
                                    updated.remove(index);
                                    data.getConfig().set("lore", updated);
                                    WildCrops.getInstance().getFileManager().saveCropConfig(cropId);
                                }
                            }
                            WindowHistory.replace(player, () -> createWindow(player));
                        }
                    },
                    actions
            ));
        }

        return gui;
    }

    public Window createWindow(Player player) {
        CropConfigData data = WildCrops.getInstance()
                .getFileManager().getCropData(cropId);
        List<String> lore = data.getConfig().getStringList("lore");
        String name = data.getConfig().getString("display_name", cropId);

        Gui gui = createGui(lore, name);
        return Window.builder()
                .setViewer(player)
                .setUpperGui(gui)
                .setTitle("Edit Crop Text")
                .build(player);
    }

    public void open(Player player) {
        WindowHistory.open(player, () -> createWindow(player));
    }
}
