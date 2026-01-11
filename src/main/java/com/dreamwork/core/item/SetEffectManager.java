package com.dreamwork.core.item;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.manager.Manager;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.util.*;

/**
 * 아이템 세트 효과 매니저
 * 
 * <p>
 * sets.yml에 정의된 세트 아이템을 로드하고,
 * 플레이어의 장비 착용 상태를 확인하여 세트 효과(스탯 보너스)를 계산합니다.
 * </p>
 */
public class SetEffectManager extends Manager {

    private final DreamWorkCore plugin;
    private final Map<String, SetDefinition> sets = new HashMap<>();

    public SetEffectManager(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        loadSets();
        enabled = true;
        plugin.getLogger().info("SetEffectManager 활성화 완료 (" + sets.size() + "개 세트 로드됨)");
    }

    @Override
    public void onDisable() {
        sets.clear();
        enabled = false;
    }

    @Override
    public void reload() {
        sets.clear();
        loadSets();
    }

    /**
     * sets.yml 파일 로드
     */
    private void loadSets() {
        File file = new File(plugin.getDataFolder(), "sets.yml");
        if (!file.exists()) {
            plugin.saveResource("sets.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                String name = config.getString(key + ".name");
                Map<String, Material> parts = new HashMap<>();

                // 파츠 로드 (helmet, chestplate, leggings, boots)
                if (config.contains(key + ".items.helmet"))
                    parts.put("helmet", Material.valueOf(config.getString(key + ".items.helmet")));
                if (config.contains(key + ".items.chestplate"))
                    parts.put("chestplate", Material.valueOf(config.getString(key + ".items.chestplate")));
                if (config.contains(key + ".items.leggings"))
                    parts.put("leggings", Material.valueOf(config.getString(key + ".items.leggings")));
                if (config.contains(key + ".items.boots"))
                    parts.put("boots", Material.valueOf(config.getString(key + ".items.boots")));

                // 효과 로드 (부위 수 -> 스탯 맵)
                Map<Integer, Map<String, Integer>> effects = new HashMap<>();
                if (config.contains(key + ".effects")) {
                    for (String countStr : config.getConfigurationSection(key + ".effects").getKeys(false)) {
                        int count = Integer.parseInt(countStr);
                        Map<String, Integer> stats = new HashMap<>();
                        for (String stat : config.getConfigurationSection(key + ".effects." + countStr)
                                .getKeys(false)) {
                            stats.put(stat.toUpperCase(), config.getInt(key + ".effects." + countStr + "." + stat));
                        }
                        effects.put(count, stats);
                    }
                }

                sets.put(key, new SetDefinition(key, name, parts, effects));
            } catch (Exception e) {
                plugin.getLogger().warning("세트 로드 실패 (" + key + "): " + e.getMessage());
            }
        }
    }

    /**
     * 플레이어 인벤토리를 검사하여 적용된 세트 효과의 합계를 반환합니다.
     * 
     * @param inv 플레이어 인벤토리
     * @return 스탯 보너스 맵 (STR -> 10, DEX -> 5 등)
     */
    public Map<String, Integer> calculateSetBonuses(PlayerInventory inv) {
        Map<String, Integer> totalBonus = new HashMap<>();

        // 각 세트별로 착용 부위 수 카운트
        for (SetDefinition set : sets.values()) {
            int count = 0;
            if (isPartEquipped(inv.getHelmet(), set, "helmet"))
                count++;
            if (isPartEquipped(inv.getChestplate(), set, "chestplate"))
                count++;
            if (isPartEquipped(inv.getLeggings(), set, "leggings"))
                count++;
            if (isPartEquipped(inv.getBoots(), set, "boots"))
                count++;

            if (count > 0) {
                // 해당 개수에 맞는 효과 적용 (누적일지, 최대치일지는 기획에 따름. 보통 RPG는 달성한 최고 단계만 적용 or 각 단계별. 여기선 달성한
                // '최고 단계'만 적용하는 것으로 가정하거나, 설정에 따라 다르지만 간단히 구현)
                // 일반적인 세트 효과: 2셋 효과, 4셋 효과가 별도로 있으면 둘 다 적용될 수도 있고, 하나만 될 수도 있음.
                // 여기서는 "달성한 모든 단계의 효과를 합산"하는 방식으로 구현 (2셋 효과 + 4셋 효과)

                for (Map.Entry<Integer, Map<String, Integer>> entry : set.getEffects().entrySet()) {
                    if (count >= entry.getKey()) {
                        entry.getValue().forEach((stat, value) -> totalBonus.merge(stat, value, Integer::sum));
                    }
                }
            }
        }

        return totalBonus;
    }

    private boolean isPartEquipped(ItemStack item, SetDefinition set, String partName) {
        if (item == null || item.getType() == Material.AIR)
            return false;
        Material required = set.getParts().get(partName);
        return required != null && item.getType() == required;
    }

    /**
     * 세트 정의 클래스
     */
    private static class SetDefinition {
        private final String id;
        private final String name;
        private final Map<String, Material> parts;
        private final Map<Integer, Map<String, Integer>> effects;

        public SetDefinition(String id, String name, Map<String, Material> parts,
                Map<Integer, Map<String, Integer>> effects) {
            this.id = id;
            this.name = name;
            this.parts = parts;
            this.effects = effects;
        }

        public Map<String, Material> getParts() {
            return parts;
        }

        public Map<Integer, Map<String, Integer>> getEffects() {
            return effects;
        }
    }
}
