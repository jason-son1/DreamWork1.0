package com.dreamwork.core.economy;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.item.ItemFactory;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

/**
 * 커스텀 드롭 테이블 매니저
 * 
 * <p>
 * 직업별 드롭 확률과 아이템을 관리합니다.
 * </p>
 */
public class LootTableManager {

    private final DreamWorkCore plugin;
    private final Random random = new Random();

    /** 드롭 테이블 (jobId -> (Material -> LootEntry)) */
    private final Map<String, Map<Material, LootEntry>> lootTables = new HashMap<>();

    public LootTableManager(DreamWorkCore plugin) {
        this.plugin = plugin;
        loadDrops();
    }

    /**
     * drops.yml에서 드롭 테이블을 로드합니다.
     */
    public void loadDrops() {
        lootTables.clear();

        File file = new File(plugin.getDataFolder(), "drops.yml");
        if (!file.exists()) {
            plugin.saveResource("drops.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String jobId : config.getKeys(false)) {
            ConfigurationSection jobSection = config.getConfigurationSection(jobId);
            if (jobSection == null)
                continue;

            Map<Material, LootEntry> jobLoot = new HashMap<>();

            for (String materialName : jobSection.getKeys(false)) {
                try {
                    Material material = Material.matchMaterial(materialName);
                    if (material == null)
                        continue;

                    ConfigurationSection entrySection = jobSection.getConfigurationSection(materialName);
                    if (entrySection == null)
                        continue;

                    double baseChance = entrySection.getDouble("base", 0.0);
                    double perLevel = entrySection.getDouble("per-level", 0.0);
                    String itemId = entrySection.getString("item", null);

                    jobLoot.put(material, new LootEntry(baseChance, perLevel, itemId));

                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "드롭 설정 로드 실패: " + materialName, e);
                }
            }

            lootTables.put(jobId, jobLoot);
        }

        plugin.getLogger().info("LootTableManager: " + lootTables.size() + "개 직업 드롭 테이블 로드됨");
    }

    /**
     * 드롭 확률을 계산합니다.
     * 
     * @param jobId 직업 ID
     * @param block 블록 타입
     * @param level 직업 레벨
     * @return 드롭 확률 (0.0 ~ 1.0)
     */
    public double getDropChance(String jobId, Material block, int level) {
        Map<Material, LootEntry> jobLoot = lootTables.get(jobId);
        if (jobLoot == null)
            return 0.0;

        LootEntry entry = jobLoot.get(block);
        if (entry == null)
            return 0.0;

        return Math.min(1.0, entry.baseChance + (entry.perLevel * level));
    }

    /**
     * 드롭 아이템을 생성합니다.
     * 
     * @param jobId 직업 ID
     * @param block 블록 타입
     * @return 드롭 아이템 (없으면 null)
     */
    public ItemStack getDropItem(String jobId, Material block) {
        Map<Material, LootEntry> jobLoot = lootTables.get(jobId);
        if (jobLoot == null)
            return null;

        LootEntry entry = jobLoot.get(block);
        if (entry == null || entry.itemId == null)
            return null;

        ItemFactory factory = plugin.getItemFactory();
        return factory.createItem(entry.itemId);
    }

    /**
     * 드롭을 시도합니다.
     * 
     * @param jobId 직업 ID
     * @param block 블록 타입
     * @param level 직업 레벨
     * @return 드롭된 아이템 (드롭 실패시 null)
     */
    public ItemStack tryDrop(String jobId, Material block, int level) {
        double chance = getDropChance(jobId, block, level);

        if (random.nextDouble() < chance) {
            return getDropItem(jobId, block);
        }

        return null;
    }

    /**
     * 드롭 테이블 엔트리
     */
    private record LootEntry(double baseChance, double perLevel, String itemId) {
    }
}
