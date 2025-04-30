package me.monoto.customseeds.crops;

import me.monoto.customseeds.WildCrops;
import org.apache.commons.lang3.Range;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CropDefinition {
    private final String id;
    private final String cropType;

    // Display & planting
    private final Material seedMaterial;
    private final String displayName;
    private final List<String> lore;
    private final boolean bonemeal;
    private final List<Material> placeOn;

    // Growth
    private final int baseGrowTime;
    private final Material finalBlock;

    // Rewards
    private final List<Reward> rewards;

    public CropDefinition(
            String id,
            String cropType,
            Material seedMaterial,
            String displayName,
            List<String> lore,
            boolean bonemeal,
            List<Material> placeOn,
            int baseGrowTime,
            Material finalBlock,
            List<Reward> rewards
    ) {
        this.id            = id;
        this.cropType      = cropType;
        this.seedMaterial  = seedMaterial;
        this.displayName   = displayName;
        this.lore          = lore;
        this.bonemeal      = bonemeal;
        this.placeOn       = placeOn;
        this.baseGrowTime  = baseGrowTime;
        this.finalBlock    = (finalBlock != null ? finalBlock : Material.FERN);
        this.rewards       = rewards;
    }

    public String getId() { return id; }
    public String getType() { return cropType; }
    public Material getSeedMaterial() { return seedMaterial; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public boolean isBonemealAllowed() { return bonemeal; }
    public List<Material> getPlaceOn() { return placeOn; }
    public int getBaseGrowTime() { return baseGrowTime; }
    public Material getFinalBlock() { return finalBlock; }
    public List<Reward> getRewards() { return rewards; }

    /** A helper class representing a crop reward. */
    public record Reward(String type, double chance, Material material, Range<Integer> amount, List<String> commands) { }

    private static Range<Integer> parseRange(String input) {
        if (input == null) return Range.between(1,1);
        try {
            if (input.contains("-")) {
                String[] parts = input.split("-");
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return Range.between(min, max);
            } else {
                int val = Integer.parseInt(input.trim());
                return Range.between(val, val);
            }
        } catch (Exception ex) {
            return Range.between(1,1);
        }
    }

    public static CropDefinition fromConfig(String cropType, YamlConfiguration cfg) {
        String id = cfg.getString("id", cropType);

        // —— seedMaterial —— //
        Material seedMat;
        String matKey = cfg.getString("material");
        if (matKey == null) {
            WildCrops.getInstance().getLogger()
                    .warning("[" + cropType + "] missing 'material', defaulting to WHEAT_SEEDS");
            seedMat = Material.WHEAT_SEEDS;
        } else {
            try {
                seedMat = Material.valueOf(matKey.toUpperCase());
            } catch (IllegalArgumentException ex) {
                WildCrops.getInstance().getLogger()
                        .warning("[" + cropType + "] invalid material '" + matKey + "', defaulting to WHEAT_SEEDS");
                seedMat = Material.WHEAT_SEEDS;
            }
        }

        // —— displayName —— //
        String displayName = cfg.getString("display_name");
        if (displayName == null || displayName.isBlank()) {
            displayName = cropType;
        }

        // —— lore —— //
        List<String> loreList = cfg.getStringList("lore");
        if (loreList == null || loreList.isEmpty()) {
            loreList = new ArrayList<>();
            loreList.add("<gray>Place it on");
            loreList.add("<dark_gray>farmland <gray>to plant it.");
        }

        // —— bonemeal & place_on —— //
        boolean allowBone = cfg.getBoolean("options.bonemeal", false);
        List<Material> soils = new ArrayList<>();
        List<String> rawSoils = cfg.getStringList("options.place_on");
        if (rawSoils == null || rawSoils.isEmpty()) {
            soils.add(Material.FARMLAND);
        } else {
            for (String s : rawSoils) {
                try {
                    soils.add(Material.valueOf(s.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    WildCrops.getInstance().getLogger()
                            .warning("[" + cropType + "] invalid place_on '" + s + "'");
                }
            }
            if (soils.isEmpty()) soils.add(Material.FARMLAND);
        }

        // —— grow_time —— //
        int secs = cfg.getInt("grow_time", 15);
        int baseGrowTicks = Math.max(1, secs) * 20;

        // —— growth.final_block —— //
        Material finalBlk = seedMat;
        if (cfg.isString("growth.final_block")) {
            String fb = cfg.getString("growth.final_block");
            try {
                finalBlk = Material.valueOf(fb.toUpperCase());
            } catch (IllegalArgumentException ex) {
                WildCrops.getInstance().getLogger()
                        .warning("[" + cropType + "] invalid growth.final_block '" + fb + "'");
                finalBlk = Material.FERN;
            }
        }

        // —— rewards —— //
        List<Reward> rewards = new ArrayList<>();
        List<Map<?,?>> rawRewards = cfg.getMapList("rewards");
        if (rawRewards.isEmpty()) {
            // default: give seed back
            rewards.add(new Reward("seed", 1.0, null, Range.between(1,1), null));
        } else {
            for (Map<?,?> entry : rawRewards) {
                String type = entry.get("type") == null ? "" : entry.get("type").toString();
                double chance = 1.0;
                if (entry.containsKey("chance")) {
                    try {
                        String c = entry.get("chance").toString().replace("%","");
                        chance = Double.parseDouble(c)/100.0;
                    } catch (Exception ignored){}
                }
                Range<Integer> range = null;
                Material mat = null;
                List<String> cmds = null;
                switch (type) {
                    case "seed":
                    case "exp":
                    case "mcmmo_exp":
                    case "money":
                        range = parseRange(entry.get("amount") == null ? null : entry.get("amount").toString());
                        break;
                    case "item":
                        String m = entry.get("material")==null?"":entry.get("material").toString();
                        try {
                            mat = Material.valueOf(m.toUpperCase());
                        } catch (Exception e) {
                            WildCrops.getInstance().getLogger()
                                    .warning("[" + cropType + "] bad item material '"+m+"'");
                            continue;
                        }
                        range = parseRange(entry.get("amount")==null?null:entry.get("amount").toString());
                        break;
                    case "command":
                        Object o = entry.get("commands");
                        if (o instanceof List<?> list) {
                            //noinspection unchecked
                            cmds = (List<String>)(List<?>)list;
                        }
                        break;
                    default:
                        WildCrops.getInstance().getLogger()
                                .warning("[" + cropType + "] unknown reward type '"+type+"'");
                        continue;
                }
                rewards.add(new Reward(type, chance, mat, range, cmds));
            }
        }

        return new CropDefinition(
                id, cropType,
                seedMat,
                displayName,
                Collections.unmodifiableList(loreList),
                allowBone,
                Collections.unmodifiableList(soils),
                baseGrowTicks,
                finalBlk,
                Collections.unmodifiableList(rewards)
        );
    }

    public static CropDefinition getDefinitionByCropType(String cropType) {
        return CropDefinitionRegistry.getDefinitions().values().stream()
                .filter(d -> d.getType().equalsIgnoreCase(cropType))
                .findFirst().orElse(null);
    }
}
