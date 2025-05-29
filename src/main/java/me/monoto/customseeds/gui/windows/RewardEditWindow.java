package me.monoto.customseeds.gui.windows;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.gui.items.BlockSelectorItem;
import me.monoto.customseeds.gui.items.ChatInputItem;
import me.monoto.customseeds.gui.items.FillerItem;
import me.monoto.customseeds.gui.items.MultiActionItem;
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
import java.util.List;
import java.util.Map;

public class RewardEditWindow {

    private final String cropId;
    private final int rewardIndex;
    private final Gui gui;
    private final boolean isItem;

    public RewardEditWindow(String cropId, int rewardIndex, boolean isItem) {
        this.cropId = cropId;
        this.rewardIndex = rewardIndex;
        this.isItem = isItem;

        CropDefinition def = CropDefinitionRegistry.get(cropId);
        this.gui = createGui(def);
    }

    private Gui createGui(CropDefinition def) {
        Gui gui = Gui.builder()
                .setStructure(
                        "# # # # # # # # #",
                        "# # a # b # c # #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', new FillerItem(Material.GRAY_STAINED_GLASS_PANE))
                .build();

        List<CropDefinition.Reward> rewards = def.getRewards();
        if (rewardIndex < 0 || rewardIndex >= rewards.size()) return gui;
        CropDefinition.Reward r = rewards.get(rewardIndex);

        // --- slot 'a': chance ---
        String chanceLabel = (int) (r.chance() * 100) + "%";
        gui.setItem('a', new MultiActionItem(
                Component.text("Chance"),
                Material.PAPER,
                TextColor.color(0xFFFF55),
                (player, click) -> {
                    if (!click.clickType().isLeftClick()) return;

                    ChatInputItem<String> prompt = new ChatInputItem<>(
                            "Chance: " + chanceLabel,
                            Material.PAPER,
                            TextColor.color(0xFFFF55),
                            "Enter chance (e.g. 10%):",
                            ChatInputType.STRING,
                            (p, input) -> {
                                String val = input.replace("%", "").trim();
                                double pct;
                                try {
                                    pct = Double.parseDouble(val) / 100.0;
                                } catch (Exception ex) {
                                    pct = r.chance();
                                }
                                // copy commands to avoid alias
                                List<String> cmdsCopy = r.commands() != null
                                        ? new ArrayList<>(r.commands())
                                        : new ArrayList<>();
                                onSave(p, pct, r.amount(), r.material(), cmdsCopy);
                            },
                            () -> WindowHistory.replace(player, () -> this.createWindow(player))
                    );

                    prompt.handleClick(ClickType.LEFT, player, click);
                },
                List.of(
                        new ClickAction(ClickType.LEFT, "Set chance", chanceLabel)
                )
        ));

        // --- slot 'b': block or command ---
        if (r.material() != null) {
            // block selector (unchanged)
            gui.setItem('b', new BlockSelectorItem(
                    "Block: " + r.material().name(),
                    r.material(),
                    TextColor.color(0x55FF55),
                    (player, sel) -> {
                        // always copy commands
                        List<String> cmdsCopy = r.commands() != null
                                ? new ArrayList<>(r.commands())
                                : new ArrayList<>();
                        onSave(player, r.chance(), r.amount(), sel, cmdsCopy);
                    },
                    BlockCache.getAllDrops()
            ));
        } else {
            // command editor (single command overwrite)
            String existing = (!r.commands().isEmpty()) ? r.commands().get(0) : "";
            gui.setItem('b', new MultiActionItem(
                    Component.text("Command"),
                    Material.PAPER,
                    TextColor.color(0x55FF55),
                    (player, click) -> {
                        if (!click.clickType().isLeftClick()) return;

                        ChatInputItem<String> prompt = new ChatInputItem<>(
                                "Command: /" + existing,
                                Material.PAPER,
                                TextColor.color(0x55FF55),
                                "Enter new command:",
                                ChatInputType.STRING,
                                (p, input) -> {
                                    List<String> one = new ArrayList<>();
                                    one.add(input);
                                    onSave(p, r.chance(), r.amount(), null, one);
                                },
                                () -> WindowHistory.replace(player, () -> this.createWindow(player))
                        );

                        prompt.handleClick(ClickType.LEFT, player, click);
                    },
                    List.of(
                            new ClickAction(ClickType.LEFT, "Set command", existing)
                    )
            ));
        }

        // --- slot 'c': amount range ---
        if (r.material() != null) {
            Range<Integer> amt = r.amount();
            String rangeLabel = amt.getMinimum().equals(amt.getMaximum())
                    ? amt.getMinimum().toString()
                    : amt.getMinimum() + "-" + amt.getMaximum();

            gui.setItem('c', new MultiActionItem(
                    Component.text("Reward Amount"),
                    Material.PAPER,
                    TextColor.color(0x55FF55),
                    (player, click) -> {
                        if (!click.clickType().isLeftClick()) return;

                        ChatInputItem<String> prompt = new ChatInputItem<>(
                                "Amount: " + rangeLabel,
                                Material.PAPER,
                                TextColor.color(0x55FF55),
                                "Enter range (min-max or single number):",
                                ChatInputType.STRING,
                                (p, input) -> {
                                    String text = input.trim();
                                    Range<Integer> newAmt;
                                    if (text.contains("-")) {
                                        String[] parts = text.split("-");
                                        int min = 1, max = 1;
                                        try {
                                            min = Integer.parseInt(parts[0].trim());
                                        } catch (Exception ignored) {
                                        }
                                        try {
                                            max = Integer.parseInt(parts[1].trim());
                                        } catch (Exception ignored) {
                                        }
                                        newAmt = Range.between(min, max);
                                    } else {
                                        int val;
                                        try {
                                            val = Integer.parseInt(text);
                                        } catch (Exception ex) {
                                            val = amt.getMinimum();
                                        }
                                        newAmt = Range.between(val, val);
                                    }
                                    // copy commands too
                                    List<String> cmdsCopy = r.commands() != null
                                            ? new ArrayList<>(r.commands())
                                            : new ArrayList<>();
                                    onSave(p, r.chance(), newAmt, r.material(), cmdsCopy);
                                },
                                () -> WindowHistory.replace(player, () -> this.createWindow(player))
                        );

                        prompt.handleClick(ClickType.LEFT, player, click);
                    },
                    List.of(
                            new ClickAction(ClickType.LEFT, "Set amount range", rangeLabel)
                    )
            ));
        } else {
            gui.setItem('c', new FillerItem(Material.GRAY_STAINED_GLASS_PANE));
        }

        return gui;
    }

    private void onSave(Player player,
                        double chance,
                        Range<Integer> amount,
                        Material mat,
                        List<String> cmds) {

        CropConfigData data = WildCrops.getInstance()
                .getFileManager().getCropData(cropId);
        YamlConfiguration cfg = data.getConfig();

        List<CropDefinition.Reward> list = new ArrayList<>(
                CropDefinitionRegistry.get(cropId).getRewards()
        );
        CropDefinition.Reward old = list.get(rewardIndex);

        // Always wrap commands in a fresh list
        List<String> copy = cmds == null
                ? new ArrayList<>()
                : new ArrayList<>(cmds);

        list.set(rewardIndex, new CropDefinition.Reward(
                old.type(), chance, mat, amount, copy
        ));

        List<Map<String, Object>> yaml = CropDefinitionRegistry.serializeRewards(list);
        cfg.set("rewards", yaml);
        WildCrops.getInstance().getFileManager().saveCropConfig(cropId);
        CropDefinition newDef = CropDefinition.fromConfig(
                data.getFileNameWithoutExtension(), cfg
        );
        CropDefinitionRegistry.update(cropId, newDef);

        Bukkit.getScheduler().runTask(WildCrops.getInstance(),
                () -> WindowHistory.replace(player, () -> this.createWindow(player))
        );
    }

    public Window createWindow(Player player) {
        CropDefinition def = CropDefinitionRegistry.get(cropId);
        Gui gui = createGui(def);

        return Window.builder()
                .setViewer(player)
                .setUpperGui(gui)
                .setTitle("Edit Reward")
                .build(player);
    }

    public void open(Player player) {
        WindowHistory.open(player, () -> createWindow(player));
    }
}
