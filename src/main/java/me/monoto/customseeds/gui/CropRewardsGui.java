package me.monoto.customseeds.gui;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.gui.items.BlockSelectorItem;
import me.monoto.customseeds.gui.items.ChatInputItem;
import me.monoto.customseeds.gui.items.FillerItem;
import me.monoto.customseeds.gui.items.MultiActionItem;
import me.monoto.customseeds.gui.windows.RewardEditWindow;
import me.monoto.customseeds.utils.BlockCache;
import me.monoto.customseeds.utils.ChatInput.ChatInputType;
import me.monoto.customseeds.utils.ClickAction;
import me.monoto.customseeds.utils.WindowHistory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.Range;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CropRewardsGui {

    private final String cropId;
    private final boolean isItemMenu;
    private final Gui gui;
    private static final char[] SLOTS = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g',
            'h', 'i', 'j', 'k', 'l', 'm', 'n'
    };

    public CropRewardsGui(String cropId, boolean isItemMenu) {
        this.cropId = cropId;
        this.isItemMenu = isItemMenu;
        CropDefinition def = CropDefinitionRegistry.get(cropId);
        this.gui = createGui(def);
    }

    private Gui createGui(CropDefinition def) {
        Gui gui = Gui.builder()
                .setStructure(
                        "# # # # # # # # #",
                        "# a b c d e f g #",
                        "# h i j k l m n #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', new FillerItem(Material.GRAY_STAINED_GLASS_PANE))
                .build();

        List<CropDefinition.Reward> rewards = def.getRewards();
        int idx = 0;

        if (isItemMenu) {
            // render each item reward
            for (int i = 0; i < rewards.size() && idx < SLOTS.length; i++) {
                CropDefinition.Reward r = rewards.get(i);
                if (!"item".equalsIgnoreCase(r.type())) continue;
                int rewardIndex = i;
                char slot = SLOTS[idx++];

                MultiActionItem item = new MultiActionItem(
                        Component.text("Reward"),
                        r.material(),
                        TextColor.color(0x00FF00),
                        (player, click) -> {
                            switch (click.clickType()) {
                                case LEFT -> new RewardEditWindow(cropId, rewardIndex, true).open(player);
                                case SHIFT_LEFT, SHIFT_RIGHT -> new BlockSelectorItem(
                                        "Edit Reward Block",
                                        r.material(),
                                        TextColor.color(0x00FF00),
                                        (p, sel) -> onUpdateRewardBlock(p, rewardIndex, sel),
                                        BlockCache.getAllDrops()
                                ).handleClick(click.clickType(), player, click);
                                case RIGHT -> mutateAndSave(player, list -> list.remove(rewardIndex));
                                default -> {
                                }
                            }
                        },
                        List.of(
                                new ClickAction(ClickType.LEFT, "Edit Reward", r.material().name()),
                                new ClickAction(ClickType.SHIFT_LEFT, "Edit Reward (quick)"),
                                new ClickAction(ClickType.SHIFT_RIGHT, "Remove Reward")
                        )
                );
                gui.setItem(slot, item);
            }
        } else {
            // render each command across all command rewards
            int cmdIdx = 0;
            for (int i = 0; i < rewards.size() && idx < SLOTS.length; i++) {
                CropDefinition.Reward r = rewards.get(i);
                if (!"command".equalsIgnoreCase(r.type()) || r.commands() == null) continue;
                for (String cmd : r.commands()) {
                    char slot = SLOTS[idx++];
                    int commandIndex = cmdIdx++;

                    MultiActionItem item = new MultiActionItem(
                            Component.text("Command"),
                            Material.PAPER,
                            TextColor.color(0x00FF00),
                            (player, click) -> {
                                switch (click.clickType()) {
                                    case LEFT -> new RewardEditWindow(cropId, commandIndex, false).open(player);
                                    case SHIFT_LEFT, SHIFT_RIGHT -> {
                                        new ChatInputItem<>(
                                                "Edit Command",
                                                Material.PAPER,
                                                TextColor.color(0x00FF00),
                                                "Enter new command (currently: /" + cmd + "):",
                                                ChatInputType.STRING,
                                                (p, input) -> onUpdateCommand(p, commandIndex, input),
                                                () -> WindowHistory.replace(player, () -> this.createWindow(player))
                                        ).handleClick(click.clickType(), player, click);
                                    }
                                    case RIGHT -> mutateAndSave(player, list -> removeCommandAt(list, commandIndex));
                                    default -> {
                                    }
                                }
                            },
                            List.of(
                                    new ClickAction(ClickType.LEFT, "Edit Reward", abbreviate(cmd)),
                                    new ClickAction(ClickType.SHIFT_LEFT, "Edit Reward (quick)"),
                                    new ClickAction(ClickType.SHIFT_RIGHT, "Remove Reward")
                            )
                    );
                    gui.setItem(slot, item);
                }
            }
        }

        // "Add New" slot
        if (idx < SLOTS.length) {
            char slot = SLOTS[idx];
            if (isItemMenu) {
                BlockSelectorItem addBase = new BlockSelectorItem(
                        "Add New Reward",
                        Material.LIME_DYE,
                        TextColor.color(0x00FF00),
                        this::onAddItemReward,
                        BlockCache.getAllDrops()
                );
                MultiActionItem add = new MultiActionItem(
                        Component.text("Add New Reward"),
                        Material.LIME_DYE,
                        TextColor.color(0x00FF00),
                        (player, click) -> {
                            if (!click.clickType().isLeftClick()) return;

                            addBase.handleClick(ClickType.LEFT, player, click);
                        },
                        List.of(
                                new ClickAction(ClickType.LEFT, "Set reward")
                        )
                );
                gui.setItem(slot, add);
            } else {
                MultiActionItem add = new MultiActionItem(
                        Component.text("Add Command Reward"),
                        Material.LIME_DYE,
                        TextColor.color(0x00FF00),
                        (player, click) -> {
                            if (!click.clickType().isLeftClick()) return;

                            // build the ChatInputItem here so `player` is available
                            ChatInputItem<String> prompt = new ChatInputItem<>(
                                    "Add Command Reward",
                                    Material.LIME_DYE,
                                    TextColor.color(0x00FF00),
                                    "Enter command to add (without /):",
                                    ChatInputType.STRING,
                                    (p, input) -> {
                                        onAddCommandReward(p, input);
                                        WindowHistory.replace(player, () -> this.createWindow(player));
                                    },
                                    () -> WindowHistory.replace(player, () -> this.createWindow(player))
                            );
                            prompt.handleClick(ClickType.LEFT, player, click);
                        },
                        List.of(
                                new ClickAction(ClickType.LEFT, "Edit reward")
                        )
                );
                gui.setItem(slot, add);
            }
        }

        return gui;
    }

    private void removeCommandAt(List<CropDefinition.Reward> list, int cmdIndex) {
        // flatten all commands, remove at index, then rebuild single command reward
        List<String> all = new ArrayList<>();
        for (CropDefinition.Reward r : list)
            if ("command".equals(r.type()) && r.commands() != null) all.addAll(r.commands());
        all.remove(cmdIndex);
        list.removeIf(r -> "command".equals(r.type()));
        if (!all.isEmpty()) list.add(new CropDefinition.Reward(
                "command", 1.0, null,
                CropDefinition.parseRange("1"), Collections.unmodifiableList(all)
        ));
    }

    private void mutateAndSave(Player player, Consumer<List<CropDefinition.Reward>> mutator) {
        CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropId);
        YamlConfiguration cfg = data.getConfig();

        // apply mutation
        CropDefinition oldDef = CropDefinitionRegistry.get(cropId);
        List<CropDefinition.Reward> list = new ArrayList<>(oldDef.getRewards());
        mutator.accept(list);

        // flatten commands into a single reward entry
        List<CropDefinition.Reward> flat = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        for (CropDefinition.Reward r : list) {
            if ("command".equals(r.type()) && r.commands() != null) commands.addAll(r.commands());
            else flat.add(r);
        }
        if (!commands.isEmpty()) {
            flat.add(new CropDefinition.Reward(
                    "command", 1.0, null,
                    CropDefinition.parseRange("1"), Collections.unmodifiableList(commands)
            ));
        }

        // save flat list
        List<Map<String, Object>> yaml = CropDefinitionRegistry.serializeRewards(flat);
        cfg.set("rewards", yaml);
        WildCrops.getInstance().getFileManager().saveCropConfig(cropId);

        CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), cfg);
        CropDefinitionRegistry.update(cropId, newDef);

        Bukkit.getScheduler().runTask(WildCrops.getInstance(), () -> WindowHistory.replace(player, () -> this.createWindow(player)));
    }

    private void onUpdateRewardBlock(Player player, int rewardIndex, Material selected) {
        mutateAndSave(player, list -> {
            CropDefinition.Reward old = list.get(rewardIndex);
            list.set(rewardIndex, new CropDefinition.Reward(
                    old.type(), old.chance(), selected, old.amount(), old.commands()));
        });
    }

    private void onUpdateCommand(Player player, int cmdIndex, String input) {
        mutateAndSave(player, list -> {
            // find existing command reward and update that index
            for (int k = 0; k < list.size(); k++) {
                CropDefinition.Reward r = list.get(k);
                if ("command".equalsIgnoreCase(r.type()) && r.commands() != null) {
                    List<String> cmds = new ArrayList<>(r.commands());
                    if (cmdIndex >= 0 && cmdIndex < cmds.size()) {
                        cmds.set(cmdIndex, input);
                        list.set(k, new CropDefinition.Reward(
                                r.type(), r.chance(), r.material(), r.amount(),
                                Collections.unmodifiableList(cmds)
                        ));
                    }
                    break;
                }
            }
        });
    }

    private void onAddItemReward(Player player, Material selected) {
        mutateAndSave(player, list -> list.add(new CropDefinition.Reward("item", 1.0, selected, Range.between(1, 1), null)));
    }

    private void onAddCommandReward(Player player, String input) {
        mutateAndSave(player, list -> {
            // append to existing command reward or create new
            boolean found = false;
            for (int k = 0; k < list.size(); k++) {
                CropDefinition.Reward r = list.get(k);
                if ("command".equalsIgnoreCase(r.type()) && r.commands() != null) {
                    List<String> cmds = new ArrayList<>(r.commands());
                    cmds.add(input);
                    list.set(k, new CropDefinition.Reward(
                            r.type(), r.chance(), r.material(), r.amount(),
                            Collections.unmodifiableList(cmds)
                    ));
                    found = true;
                    break;
                }
            }
            if (!found) {
                list.add(new CropDefinition.Reward(
                        "command", 1.0, null,
                        CropDefinition.parseRange("1"),
                        Collections.unmodifiableList(List.of(input))
                ));
            }
        });
    }

    private String abbreviate(String s) {
        return s.length() > 20 ? "/" + s.substring(0, 20) + "…" : s;
    }

    public Window createWindow(Player player) {
        CropDefinition def = CropDefinitionRegistry.get(cropId);
        Gui gui = createGui(def);

        return Window.builder()
                .setViewer(player)
                .setUpperGui(gui)
                .setTitle(isItemMenu ? "Item Rewards" : "Command Rewards")
                .build(player);
    }

    public void open(Player player) {
        WindowHistory.open(player, () -> this.createWindow(player));
    }
}
