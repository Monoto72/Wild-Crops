package me.monoto.customseeds.crops;

import org.apache.commons.lang3.Range;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;
import java.util.stream.Collectors;

public class CropDefinition {
    private final String id;
    private final String cropType;

    // Display & planting
    private final Material seedMaterial;
    private final String displayName;
    private final List<String> lore;
    private final boolean bonemeal;
    private final boolean autoReplant;

    // Growth
    private final int baseGrowTime;
    private final Material finalBlock;
    private final SoilPlacement placement;
    private final int minLight;

    // Rewards
    private final List<Reward> rewards;
    private final Map<String, Reward> rewardByType;

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
            boolean autoReplant,
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
        this.lore = Collections.unmodifiableList(new ArrayList<>(lore));
        this.bonemeal = bonemeal;
        this.autoReplant = autoReplant;
        this.baseGrowTime = baseGrowTime;
        this.finalBlock = (finalBlock != null ? finalBlock : Material.FERN);
        this.placement = placement;
        this.minLight = minLight;
        this.rewards = Collections.unmodifiableList(new ArrayList<>(rewards));
        this.rewardByType = this.rewards.stream()
                .collect(Collectors.toUnmodifiableMap(
                        Reward::type,
                        r -> r,
                        (first, second) -> first
                ));
    }

    // Getters
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

    public boolean isAutoReplantAllowed() {
        return autoReplant;
    }

    public int getBaseGrowTime() {
        return baseGrowTime;
    }

    public Material getFinalBlock() {
        return finalBlock;
    }

    public int getMinLight() {
        return minLight;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    // Reward lookup
    public Optional<Reward> getReward(String type) {
        return Optional.ofNullable(rewardByType.get(type));
    }

    public Range<Integer> getRewardRange(String type) {
        return getReward(type)
                .map(Reward::amount)
                .orElse(Range.between(0, 0));
    }

    public Range<Integer> getExpRewardRange() {
        return getRewardRange("exp");
    }

    public Range<Integer> getMoneyRewardRange() {
        return getRewardRange("money");
    }

    public Range<Integer> getMcMMORewardRange() {
        return getRewardRange("mcmmo_exp");
    }

    public Range<Integer> getSeedRewardRange() {
        return getRewardRange("seed");
    }

    /**
     * Represents a crop reward entry.
     */
    public record Reward(
            String type,
            double chance,
            Material material,
            Range<Integer> amount,
            List<String> commands
    ) {
    }

    // Utility
    public static Range<Integer> parseRange(String input) {
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

    /**
     * Constructs a CropDefinition from config.
     */
    public static CropDefinition fromConfig(String cropType, YamlConfiguration cfg) {
        String id = cfg.getString("id", cropType);

        // —— Display & planting ——
        Material seedMat;
        try {
            seedMat = Material.valueOf(cfg.getString("material", "WHEAT_SEEDS").toUpperCase());
        } catch (Exception e) {
            seedMat = Material.WHEAT_SEEDS;
        }
        String displayName = cfg.getString("display_name", cropType);
        List<String> loreList = cfg.getStringList("lore");

        // —— Growth options ——
        boolean allowBone = cfg.getBoolean("settings.bone_meal", false);
        boolean allowAuto = cfg.getBoolean("settings.auto_replant", false);
        int secs = cfg.getInt("settings.grow_time", 15);
        int ticks = Math.max(1, secs) * 20;
        int minLight = cfg.getInt("settings.min_light_level", 8);
        Material finalBlk;
        try {
            finalBlk = Material.valueOf(cfg.getString("settings.final_block", seedMat.name()).toUpperCase());
        } catch (Exception e) {
            finalBlk = seedMat;
        }
        SoilPlacement placement = (seedMat == Material.COCOA_BEANS)
                ? SoilPlacement.SIDE : SoilPlacement.BELOW;

        // —— Rewards ——
        List<Reward> rewards = new ArrayList<>();
        for (Map<?, ?> entry : cfg.getMapList("rewards")) {
            String type = Objects.toString(entry.get("type"), "");
            double chance = 1.0;
            if (entry.containsKey("chance")) {
                String c = Objects.toString(entry.get("chance"), "").replace("%", "");
                try {
                    chance = Double.parseDouble(c) / 100.0;
                } catch (Exception ignored) {
                }
            }
            Material mat = null;
            if ("item".equals(type) && entry.containsKey("material")) {
                try {
                    mat = Material.valueOf(entry.get("material").toString().toUpperCase());
                } catch (Exception ignored) {
                }
            }
            Range<Integer> range = parseRange(Objects.toString(entry.get("amount"), "1"));
            @SuppressWarnings("unchecked")
            List<String> cmds = entry.containsKey("commands")
                    ? (List<String>) entry.get("commands") : Collections.emptyList();
            rewards.add(new Reward(type, chance, mat, range, cmds));
        }

        return new CropDefinition(
                id,
                cropType,
                seedMat,
                displayName,
                loreList,
                allowBone,
                allowAuto,
                ticks,
                finalBlk,
                placement,
                minLight,
                rewards
        );
    }

    public static CropDefinition getDefinitionByCropType(String cropType) {
        return CropDefinitionRegistry.getDefinitions().values().stream()
                .filter(d -> d.getType().equalsIgnoreCase(cropType))
                .findFirst().orElse(null);
    }
}