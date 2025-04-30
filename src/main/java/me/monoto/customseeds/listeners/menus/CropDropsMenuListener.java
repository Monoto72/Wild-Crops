package me.monoto.customseeds.listeners.menus;

import me.monoto.customseeds.WildCrops;
import me.monoto.customseeds.crops.CropConfigData;
import me.monoto.customseeds.crops.CropDefinition;
import me.monoto.customseeds.crops.CropDefinitionRegistry;
import me.monoto.customseeds.gui.CropDropsMenuHolder;
import me.monoto.customseeds.gui.CropSettingsGui;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.Range;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CropDropsMenuListener implements Listener {
    private static final int START_SLOT = 10;
    private static final int END_SLOT = 16;

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CropDropsMenuHolder)) return;
        CropDropsMenuHolder menu = (CropDropsMenuHolder) top.getHolder();
        Inventory menuInv = menu.getInventory();
        String cropType = menu.getCropType();
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);

        // adding new drop from player inventory
        if (event.getClickedInventory() == player.getInventory()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            // find empty center slot
            int empty = -1;
            for (int i = START_SLOT; i <= END_SLOT; i++) {
                ItemStack it = menuInv.getItem(i);
                if (it == null || it.getType() == Material.AIR) {
                    empty = i; break;
                }
            }
            if (empty < 0) {
                player.sendMessage(Component.text("No empty drop slot available."));
                return;
            }
            int idx = empty - START_SLOT;
            Material mat = clicked.getType();
            Range<Integer> defaultRange = Range.between(1, 3);

            // insert new reward of type=item
            updateItemReward(cropType, idx, mat, defaultRange);
            menuInv.setItem(empty, menu.createRewardItem(mat, defaultRange));
            player.sendMessage(Component.text("Added drop " + mat.name() + " 1-3"));

            int slot = empty;
            WildCrops.getInstance().getChatInput().openChatInput(player,
                    "Enter drop range for " + mat.name() + " (e.g. 1-3):", input -> {
                        Range<Integer> nr = parseRange(input, defaultRange);
                        if (nr.getMinimum()==0 && nr.getMaximum()==0) {
                            removeItemReward(cropType, idx);
                            menuInv.setItem(slot, new ItemStack(Material.AIR));
                            player.sendMessage(Component.text("Drop removed."));
                        } else {
                            updateItemReward(cropType, idx, mat, nr);
                            menuInv.setItem(slot, menu.createRewardItem(mat, nr));
                            player.sendMessage(Component.text("Drop range updated to: " + nr.getMinimum()+"-"+nr.getMaximum()));
                        }
                        new CropDropsMenuHolder(cropType).open(player);
                    }
            );
            return;
        }

        int slot = event.getSlot();
        if (slot < START_SLOT || slot > END_SLOT) {
            if (slot == 26) {
                new CropSettingsGui(cropType).open(player);
            }
            return;
        }

        ItemStack clicked = menuInv.getItem(slot);
        if (clicked == null || clicked.getType()==Material.AIR) return;
        int idx = slot - START_SLOT;
        Material mat = clicked.getType();
        Range<Integer> current = getItemRange(cropType, idx);
        String defText = current.getMinimum()+"-"+current.getMaximum();

        if (event.getClick().isLeftClick()) {
            WildCrops.getInstance().getChatInput().openChatInput(player,
                    "Edit drop range for " + mat.name() + " (e.g. " + defText + "):", input -> {
                        Range<Integer> nr = parseRange(input, current);
                        if (nr.getMinimum()==0 && nr.getMaximum()==0) {
                            removeItemReward(cropType, idx);
                            menuInv.setItem(slot, new ItemStack(Material.AIR));
                        } else {
                            updateItemReward(cropType, idx, mat, nr);
                            menuInv.setItem(slot, menu.createRewardItem(mat, nr));
                        }
                        new CropDropsMenuHolder(cropType).open(player);
                    }
            );
        } else if (event.getClick().isRightClick()) {
            removeItemReward(cropType, idx);
            menuInv.setItem(slot, new ItemStack(Material.AIR));
            new CropDropsMenuHolder(cropType).open(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof CropDropsMenuHolder)) return;
        CropDropsMenuHolder menu = (CropDropsMenuHolder) top.getHolder();
        String cropType = menu.getCropType();

        CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(cropType);
        if (data==null) return;
        YamlConfiguration config = data.getConfig();

        // ensure at least one item reward exists
        List<Map<String,Object>> rewards = (List<Map<String,Object>>) (List<?>) config.getMapList("rewards");
        boolean hasItem = rewards.stream()
                .anyMatch(m -> "item".equalsIgnoreCase((String) m.get("type")));
        if (!hasItem) {
            updateItemReward(cropType, 0, Material.WHEAT_SEEDS, Range.between(1,3));
        }
    }

    private void updateItemReward(String type, int idx, Material mat, Range<Integer> range) {
        CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(type);
        if (data==null) return;
        YamlConfiguration config = data.getConfig();
        List<Map<String,Object>> list = (List<Map<String,Object>>) (List<?>) config.getMapList("rewards");
        // collect existing item rewards
        List<Map<String,Object>> items = list.stream()
                .filter(m -> "item".equalsIgnoreCase((String)m.get("type")))
                .collect(Collectors.toList());
        // remove all item rewards
        list.removeIf(m -> "item".equalsIgnoreCase((String)m.get("type")));
        // ensure size
        while (items.size() <= idx) items.add(Map.of("type","item"));
        Map<String,Object> entry = items.get(idx);
        entry.put("type","item");
        entry.put("material", mat.name());
        entry.put("amount", range.getMinimum()+"-"+range.getMaximum());
        // re-add with correct order
        list.addAll(entryPosition(idx, list.size()), items);
        config.set("rewards", list);
        WildCrops.getInstance().getFileManager().saveCropConfig(type);
        CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), config);
        CropDefinitionRegistry.update(type, newDef);
    }

    private void removeItemReward(String type, int idx) {
        CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(type);
        if (data==null) return;
        YamlConfiguration config = data.getConfig();
        List<Map<String,Object>> list = (List<Map<String,Object>>) (List<?>) config.getMapList("rewards");
        // collect item entries and remove
        List<Map<String,Object>> items = list.stream()
                .filter(m -> "item".equalsIgnoreCase((String)m.get("type")))
                .collect(Collectors.toList());
        if (idx < items.size()) {
            items.remove(idx);
        }
        list.removeIf(m -> "item".equalsIgnoreCase((String)m.get("type")));
        list.addAll(items);
        config.set("rewards", list);
        WildCrops.getInstance().getFileManager().saveCropConfig(type);
        CropDefinition newDef = CropDefinition.fromConfig(data.getFileNameWithoutExtension(), config);
        CropDefinitionRegistry.update(type, newDef);
    }

    @SuppressWarnings("unchecked")
    private Range<Integer> getItemRange(String type, int idx) {
        CropConfigData data = WildCrops.getInstance().getFileManager().getCropData(type);
        if (data==null) return Range.between(1,3);
        YamlConfiguration config = data.getConfig();
        List<Map<String,Object>> list = (List<Map<String,Object>>) (List<?>) config.getMapList("rewards");
        List<Map<String,Object>> items = list.stream()
                .filter(m -> "item".equalsIgnoreCase((String)m.get("type")))
                .collect(Collectors.toList());
        if (idx >= items.size()) return Range.between(1,3);
        String amt = (String) items.get(idx).get("amount");
        return parseRange(amt, Range.between(1,3));
    }

    private Range<Integer> parseRange(String input, Range<Integer> def) {
        try {
            if (input.contains("-")) {
                String[] p = input.split("-");
                int a = Integer.parseInt(p[0].trim());
                int b = Integer.parseInt(p[1].trim());
                return Range.between(a,b);
            } else {
                int v = Integer.parseInt(input.trim());
                return Range.between(v,v);
            }
        } catch (Exception e) {
            return def;
        }
    }

    /** Utility to insert items at correct spot; here simply returns end to append. */
    private int entryPosition(int idx, int size) {
        return size;
    }
}
