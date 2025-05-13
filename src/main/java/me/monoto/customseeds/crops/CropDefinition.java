package me.monoto.customseeds.crops;

import me.monoto.customseeds.WildCrops;
import org.apache.commons.lang3.Range;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

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
    private final SoilPlacement placement;
    private final int minLight;

    // Rewards
    private final List<Reward> rewards;

    public enum SoilPlacement {
        BELOW,   // e.g. wheat, beetroot, nether wart
        SIDE     // e.g. cocoa pods
    }

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
            SoilPlacement placement,
            int minLight,
            List<Reward> rewards
    ) {
        this.id = id;
        this.cropType = cropType;
        this.seedMaterial = seedMaterial;
        this.displayName = displayName;
        this.lore = lore;
        this.bonemeal = bonemeal;
        this.placeOn = placeOn;
        this.baseGrowTime = baseGrowTime;
        this.finalBlock = (finalBlock != null ? finalBlock : Material.FERN);
        this.placement = placement;
        this.minLight = minLight;
        this.rewards = rewards;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return cropType;
    }

    public Material getSeedMaterial() {
        return seedMaterial;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public boolean isBonemealAllowed() {
        return bonemeal;
    }

    public int getBaseGrowTime() {
        return baseGrowTime;
    }

    public Material getFinalBlock() {
        return finalBlock;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public SoilPlacement getPlacement() {
        return placement;
    }

    public int getMinLight() {
        return minLight;
    }

    /**
     * A helper class representing a crop reward.
     */
    public record Reward(String type, double chance, Material material, Range<Integer> amount, List<String> commands) {
    }

    private static Range<Integer> parseRange(String input) {
        if (input == null) return Range.between(1, 1);
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
            return Range.between(1, 1);
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

        // —— determine placement automatically —— //
        SoilPlacement placement;
        if (seedMat == Material.COCOA_BEANS) {
            placement = SoilPlacement.SIDE;
        } else {
            placement = SoilPlacement.BELOW;
        }

        // —— minLightLevel —— //
        int minLight = cfg.getInt("options.min_light_level", 8);

        // —— bonemeal & place_on —— //
        boolean allowBone = cfg.getBoolean("options.bonemeal", false);

        // I'm pretty sure we phased this out, this can stick for now. Maybe when we add custom tiles
        // we can use this again. i.e. place_on: [diamond_ore, cobblestone] etc.
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
        List<Map<String, Object>> rewardEntries = new ArrayList<>();
        for (Map<?, ?> entry : cfg.getMapList("rewards")) {
            Map<String, Object> newEntry = new HashMap<>();
            for (Map.Entry<?, ?> e : entry.entrySet()) {
                if (e.getKey() instanceof String key) {
                    newEntry.put(key, e.getValue());
                }
            }
            rewardEntries.add(newEntry);
        }

        if (rewardEntries.isEmpty()) {
            rewards.add(new Reward("seed", 1.0, null, Range.between(1, 1), null));
        } else {
            for (Map<String, Object> entry : rewardEntries) {
                String type = entry.getOrDefault("type", "").toString();
                double chance = 1.0;
                if (entry.containsKey("chance")) {
                    try {
                        String c = entry.get("chance").toString().replace("%", "");
                        chance = Double.parseDouble(c) / 100.0;
                    } catch (Exception ignored) {
                    }
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
                        String m = entry.getOrDefault("material", "").toString();
                        try {
                            mat = Material.valueOf(m.toUpperCase());
                        } catch (Exception e) {
                            WildCrops.getInstance().getLogger()
                                    .warning("[" + cropType + "] bad item material '" + m + "'");
                            continue;
                        }
                        range = parseRange(entry.get("amount") == null ? null : entry.get("amount").toString());
                        break;
                    case "command":
                        Object o = entry.get("commands");
                        if (o instanceof List<?> list) {
                            // No cast warning needed now because Map is <String, Object>
                            cmds = new ArrayList<>();
                            for (Object cmdObj : list) {
                                if (cmdObj != null) {
                                    cmds.add(cmdObj.toString());
                                }
                            }
                        }
                        break;
                    default:
                        WildCrops.getInstance().getLogger()
                                .warning("[" + cropType + "] unknown reward type '" + type + "'");
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
                placement,
                minLight,
                Collections.unmodifiableList(rewards)
        );
    }

    public static CropDefinition getDefinitionByCropType(String cropType) {
        return CropDefinitionRegistry.getDefinitions().values().stream()
                .filter(d -> d.getType().equalsIgnoreCase(cropType))
                .findFirst().orElse(null);
    }
}
