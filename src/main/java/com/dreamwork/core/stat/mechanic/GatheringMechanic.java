package com.dreamwork.core.stat.mechanic;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.stat.StatManager.PlayerStats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 채집 메카닉 계산기
 * 
 * <p>
 * DEX, LUCK, INT 스탯을 채집 활동에 적용합니다.
 * </p>
 * 
 * <h2>기능:</h2>
 * <ul>
 * <li>더블 드롭: LUCK 기반 확률</li>
 * <li>자동 제련: INT 기반 확률</li>
 * <li>내구도 보호: DEX 기반 확률</li>
 * </ul>
 */
public class GatheringMechanic {

    private final DreamWorkCore plugin;
    private final StatManager statManager;
    private final Random random = new Random();

    // 설정값
    private double luckDoubleDropFactor = 0.002;
    private double intAutoSmeltFactor = 0.005;
    private double dexDurabilityFactor = 0.003;

    // 제련 매핑
    private static final Map<Material, Material> SMELT_MAP = new HashMap<>();

    static {
        SMELT_MAP.put(Material.IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        SMELT_MAP.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        SMELT_MAP.put(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);
        SMELT_MAP.put(Material.COBBLESTONE, Material.STONE);
        SMELT_MAP.put(Material.SAND, Material.GLASS);
        SMELT_MAP.put(Material.RAW_IRON, Material.IRON_INGOT);
        SMELT_MAP.put(Material.RAW_GOLD, Material.GOLD_INGOT);
        SMELT_MAP.put(Material.RAW_COPPER, Material.COPPER_INGOT);
    }

    public GatheringMechanic(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.statManager = plugin.getStatManager();
        loadConfig();
    }

    public void loadConfig() {
        this.luckDoubleDropFactor = plugin.getConfig().getDouble("gathering.luck-double-drop-factor", 0.002);
        this.intAutoSmeltFactor = plugin.getConfig().getDouble("gathering.int-auto-smelt-factor", 0.005);
        this.dexDurabilityFactor = plugin.getConfig().getDouble("gathering.dex-durability-factor", 0.003);
    }

    /**
     * 더블 드롭 발동 여부를 확인합니다.
     * 
     * @param player 플레이어
     * @return 더블 드롭 발동 여부
     */
    public boolean shouldDoubleDrop(Player player) {
        PlayerStats stats = statManager.getStats(player);
        double chance = stats.getLuck() * luckDoubleDropFactor;
        return random.nextDouble() < chance;
    }

    /**
     * 자동 제련 발동 여부를 확인합니다.
     * 
     * @param player 플레이어
     * @return 자동 제련 발동 여부
     */
    public boolean shouldAutoSmelt(Player player) {
        PlayerStats stats = statManager.getStats(player);
        double chance = stats.getInt() * intAutoSmeltFactor;
        return random.nextDouble() < chance;
    }

    /**
     * 내구도 손실 방지 발동 여부를 확인합니다.
     * 
     * @param player 플레이어
     * @return 내구도 보호 발동 여부
     */
    public boolean preventDurabilityLoss(Player player) {
        PlayerStats stats = statManager.getStats(player);
        double chance = stats.getDex() * dexDurabilityFactor;
        return random.nextDouble() < chance;
    }

    /**
     * 광석을 제련된 결과물로 변환합니다.
     * 
     * @param ore 광석 Material
     * @return 제련 결과 (없으면 null)
     */
    public Material getSmeltResult(Material ore) {
        return SMELT_MAP.get(ore);
    }

    /**
     * 아이템을 제련된 버전으로 교체합니다.
     * 
     * @param item 원본 아이템
     * @return 제련된 아이템 (제련 불가시 원본 반환)
     */
    public ItemStack smeltItem(ItemStack item) {
        Material smelted = getSmeltResult(item.getType());
        if (smelted != null) {
            return new ItemStack(smelted, item.getAmount());
        }
        return item;
    }

    /**
     * 더블 드롭 확률을 계산합니다. (표시용)
     */
    public double getDoubleDropChance(Player player) {
        PlayerStats stats = statManager.getStats(player);
        return stats.getLuck() * luckDoubleDropFactor * 100;
    }

    /**
     * 자동 제련 확률을 계산합니다. (표시용)
     */
    public double getAutoSmeltChance(Player player) {
        PlayerStats stats = statManager.getStats(player);
        return stats.getInt() * intAutoSmeltFactor * 100;
    }

    /**
     * 내구도 보호 확률을 계산합니다. (표시용)
     */
    public double getDurabilityProtectChance(Player player) {
        PlayerStats stats = statManager.getStats(player);
        return stats.getDex() * dexDurabilityFactor * 100;
    }
}
