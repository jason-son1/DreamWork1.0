package com.dreamwork.core.item;

import com.dreamwork.core.DreamWorkCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * 성장형 도구 팩토리
 * 
 * <p>
 * Plan 2.0 기준:
 * - 모든 직업 도구(곡괭이/괭이/낚싯대/무기)의 성장형 티어 시스템
 * - PDC로 티어/경험치 저장
 * - 특정 레벨/조건 달성 시 진화
 * </p>
 */
public class EvolvingToolFactory {

    private final DreamWorkCore plugin;

    // PDC 키 정의
    private final NamespacedKey tierKey;
    private final NamespacedKey expKey;
    private final NamespacedKey toolTypeKey;
    private final NamespacedKey jobKey;

    public EvolvingToolFactory(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.tierKey = new NamespacedKey(plugin, "evolving_tier");
        this.expKey = new NamespacedKey(plugin, "evolving_exp");
        this.toolTypeKey = new NamespacedKey(plugin, "evolving_tool_type");
        this.jobKey = new NamespacedKey(plugin, "evolving_job");
    }

    // ====================== 광부 도구 ======================

    /**
     * 수습생의 곡괭이 (Tier 1) - Lv.10+
     */
    public ItemStack createApprenticePickaxe() {
        ItemStack item = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GRAY + "수습생의 곡괭이");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "광부 조합의 신입이 사용하는 곡괭이.",
                "",
                ChatColor.GOLD + "티어: " + ChatColor.WHITE + "I (수습)",
                ChatColor.YELLOW + "보너스: " + ChatColor.WHITE + "내구도 +50%",
                "",
                ChatColor.DARK_GRAY + "[사냥꾼 Lv.10 필요]"));

        // 인챈트
        meta.addEnchant(Enchantment.UNBREAKING, 2, true);

        // PDC 데이터
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.INTEGER, 1);
        pdc.set(expKey, PersistentDataType.INTEGER, 0);
        pdc.set(toolTypeKey, PersistentDataType.STRING, "pickaxe");
        pdc.set(jobKey, PersistentDataType.STRING, "miner");

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 개척자의 곡괭이 (Tier 2) - Lv.30+
     */
    public ItemStack createPioneerPickaxe() {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + "개척자의 곡괭이");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "광맥을 찾아 떠나는 개척자의 도구.",
                "",
                ChatColor.GOLD + "티어: " + ChatColor.WHITE + "II (개척자)",
                ChatColor.YELLOW + "보너스: " + ChatColor.WHITE + "채굴 속도 증가",
                ChatColor.GREEN + "특수: " + ChatColor.WHITE + "광맥 탐지 Lv.2 사용 가능",
                "",
                ChatColor.DARK_GRAY + "[광부 Lv.30 필요]"));

        // 인챈트
        meta.addEnchant(Enchantment.EFFICIENCY, 4, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);

        // PDC 데이터
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.INTEGER, 2);
        pdc.set(expKey, PersistentDataType.INTEGER, 0);
        pdc.set(toolTypeKey, PersistentDataType.STRING, "pickaxe");
        pdc.set(jobKey, PersistentDataType.STRING, "miner");

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 장인의 곡괭이 (Tier 3) - Lv.50+
     */
    public ItemStack createArtisanPickaxe() {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "장인의 곡괭이");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "대장간에서 특별히 제작된 최고급 곡괭이.",
                "",
                ChatColor.GOLD + "티어: " + ChatColor.WHITE + "III (장인)",
                ChatColor.YELLOW + "보너스: " + ChatColor.WHITE + "행운의 손 (희귀 드롭률 증가)",
                ChatColor.GREEN + "특수: " + ChatColor.WHITE + "대장간에서 수리 가능",
                "",
                ChatColor.DARK_GRAY + "[광부 Lv.50 필요]"));

        // 인챈트
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.FORTUNE, 3, true);

        // PDC 데이터
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.INTEGER, 3);
        pdc.set(expKey, PersistentDataType.INTEGER, 0);
        pdc.set(toolTypeKey, PersistentDataType.STRING, "pickaxe");
        pdc.set(jobKey, PersistentDataType.STRING, "miner");

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 대지의 심장 (Tier 4) - 전설 곡괭이
     */
    public ItemStack createLegendaryPickaxe() {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "§l대지의 심장");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.DARK_PURPLE + "고대의 드림스톤으로 제작된 전설의 곡괭이.",
                "",
                ChatColor.GOLD + "티어: " + ChatColor.WHITE + "IV (전설)",
                ChatColor.YELLOW + "보너스: " + ChatColor.WHITE + "모든 광부 능력 강화",
                ChatColor.RED + "특수: " + ChatColor.WHITE + "용암 파괴 (용암→흑요석 변환)",
                "",
                ChatColor.LIGHT_PURPLE + "\"땅의 숨결을 느껴라...\"",
                "",
                ChatColor.DARK_GRAY + "[광부 Lv.100 + 드림스톤 필요]"));

        // 인챈트
        meta.addEnchant(Enchantment.EFFICIENCY, 6, true);
        meta.addEnchant(Enchantment.UNBREAKING, 4, true);
        meta.addEnchant(Enchantment.FORTUNE, 4, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);

        // PDC 데이터
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.INTEGER, 4);
        pdc.set(expKey, PersistentDataType.INTEGER, 0);
        pdc.set(toolTypeKey, PersistentDataType.STRING, "pickaxe");
        pdc.set(jobKey, PersistentDataType.STRING, "miner");

        item.setItemMeta(meta);
        return item;
    }

    // ====================== 농부 도구 ======================

    /**
     * 초심자의 괭이 (Tier 0) - 기본
     */
    public ItemStack createBasicHoe() {
        ItemStack item = new ItemStack(Material.IRON_HOE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GRAY + "초심자의 괭이");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "농부 조합에서 지급하는 기본 괭이.",
                "",
                ChatColor.GOLD + "티어: " + ChatColor.WHITE + "0 (기본)",
                ChatColor.YELLOW + "보너스: " + ChatColor.WHITE + "내구도 증가",
                "",
                ChatColor.DARK_GRAY + "[농부 전용]"));

        // PDC 데이터
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.INTEGER, 0);
        pdc.set(toolTypeKey, PersistentDataType.STRING, "hoe");
        pdc.set(jobKey, PersistentDataType.STRING, "farmer");

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 개량된 괭이 (Tier 1) - Lv.20+
     */
    public ItemStack createImprovedHoe() {
        ItemStack item = new ItemStack(Material.IRON_HOE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + "개량된 괭이");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "대장간에서 개량된 효율적인 괭이.",
                "",
                ChatColor.GOLD + "티어: " + ChatColor.WHITE + "I (숙련)",
                ChatColor.YELLOW + "보너스: " + ChatColor.WHITE + "풍작 (10% 확률로 수확물 2배)",
                "",
                ChatColor.DARK_GRAY + "[농부 Lv.20 필요]"));

        // 인챈트
        meta.addEnchant(Enchantment.UNBREAKING, 2, true);

        // PDC 데이터
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.INTEGER, 1);
        pdc.set(toolTypeKey, PersistentDataType.STRING, "hoe");
        pdc.set(jobKey, PersistentDataType.STRING, "farmer");

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 대지주의 괭이 (Tier 2) - Lv.50+
     */
    public ItemStack createMasterHoe() {
        ItemStack item = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "대지주의 괭이");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "광활한 농장을 관리하는 대지주의 괭이.",
                "",
                ChatColor.GOLD + "티어: " + ChatColor.WHITE + "II (마스터)",
                ChatColor.YELLOW + "보너스: " + ChatColor.WHITE + "풍작 효과 강화",
                ChatColor.GREEN + "특수: " + ChatColor.WHITE + "우클릭 시 3x3 범위 수확",
                "",
                ChatColor.DARK_GRAY + "[농부 Lv.50 필요]"));

        // 인챈트
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.FORTUNE, 2, true);

        // PDC 데이터
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.INTEGER, 2);
        pdc.set(toolTypeKey, PersistentDataType.STRING, "hoe");
        pdc.set(jobKey, PersistentDataType.STRING, "farmer");

        item.setItemMeta(meta);
        return item;
    }

    // ====================== 어부 도구 ======================

    /**
     * 숙련가의 낚싯대 (Tier 1) - Lv.20+
     */
    public ItemStack createSkilledRod() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + "숙련가의 낚싯대");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "숙련된 어부의 손길이 느껴지는 낚싯대.",
                "",
                ChatColor.GOLD + "티어: " + ChatColor.WHITE + "I (숙련)",
                ChatColor.YELLOW + "보너스: " + ChatColor.WHITE + "월척 확률 증가",
                "",
                ChatColor.DARK_GRAY + "[어부 Lv.20 필요]"));

        // 인챈트
        meta.addEnchant(Enchantment.LURE, 2, true);
        meta.addEnchant(Enchantment.UNBREAKING, 2, true);

        // PDC 데이터
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.INTEGER, 1);
        pdc.set(toolTypeKey, PersistentDataType.STRING, "fishing_rod");
        pdc.set(jobKey, PersistentDataType.STRING, "fisher");

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 강태공의 낚싯대 (Tier 2) - Lv.50+
     */
    public ItemStack createMasterRod() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "§l강태공의 낚싯대");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "전설의 어부가 사용했다는 신비로운 낚싯대.",
                "",
                ChatColor.GOLD + "티어: " + ChatColor.WHITE + "II (전설)",
                ChatColor.YELLOW + "보너스: " + ChatColor.WHITE + "모든 어부 능력 강화",
                ChatColor.GREEN + "특수: " + ChatColor.WHITE + "20% 확률로 미끼 소모 안 함",
                "",
                ChatColor.LIGHT_PURPLE + "\"물 위에서 기다리는 자에게 행운이...\"",
                "",
                ChatColor.DARK_GRAY + "[어부 Lv.50 + 보물 필요]"));

        // 인챈트
        meta.addEnchant(Enchantment.LURE, 3, true);
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);

        // PDC 데이터
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.INTEGER, 2);
        pdc.set(toolTypeKey, PersistentDataType.STRING, "fishing_rod");
        pdc.set(jobKey, PersistentDataType.STRING, "fisher");

        item.setItemMeta(meta);
        return item;
    }

    // ====================== 사냥꾼 도구 ======================

    /**
     * 정육도 (Lv.20+) - 루팅 효율 특화
     */
    public ItemStack createButcherKnife() {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.RED + "정육도");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "몬스터의 가죽과 뼈를 효율적으로 수집하는 칼.",
                "",
                ChatColor.GOLD + "타입: " + ChatColor.WHITE + "Butcher (도축용)",
                ChatColor.YELLOW + "보너스: " + ChatColor.WHITE + "루팅 효율 2배",
                "",
                ChatColor.DARK_GRAY + "[사냥꾼 Lv.20 필요]"));

        // 인챈트
        meta.addEnchant(Enchantment.LOOTING, 4, true);
        meta.addEnchant(Enchantment.UNBREAKING, 2, true);

        // PDC 데이터
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.INTEGER, 1);
        pdc.set(toolTypeKey, PersistentDataType.STRING, "sword_butcher");
        pdc.set(jobKey, PersistentDataType.STRING, "hunter");

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 슬레이어 (Lv.50+) - 치명타 특화
     */
    public ItemStack createSlayer() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.DARK_RED + "§l슬레이어");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "수많은 몬스터의 피로 물든 사냥꾼의 검.",
                "",
                ChatColor.GOLD + "타입: " + ChatColor.WHITE + "Slayer (참수용)",
                ChatColor.YELLOW + "보너스: " + ChatColor.WHITE + "치명타 확률 2배",
                ChatColor.GREEN + "특수: " + ChatColor.WHITE + "빠른 공격 속도",
                "",
                ChatColor.DARK_GRAY + "[사냥꾼 Lv.50 + 보스 재료 필요]"));

        // 인챈트
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);

        // PDC 데이터
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(tierKey, PersistentDataType.INTEGER, 2);
        pdc.set(toolTypeKey, PersistentDataType.STRING, "sword_slayer");
        pdc.set(jobKey, PersistentDataType.STRING, "hunter");

        item.setItemMeta(meta);
        return item;
    }

    // ====================== 유틸리티 ======================

    /**
     * 아이템이 성장형 도구인지 확인합니다.
     */
    public boolean isEvolvingTool(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(tierKey, PersistentDataType.INTEGER);
    }

    /**
     * 도구의 티어를 반환합니다.
     */
    public int getTier(ItemStack item) {
        if (!isEvolvingTool(item))
            return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(tierKey, PersistentDataType.INTEGER, 0);
    }

    /**
     * 도구의 누적 경험치를 반환합니다.
     */
    public int getExp(ItemStack item) {
        if (!isEvolvingTool(item))
            return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(expKey, PersistentDataType.INTEGER, 0);
    }

    /**
     * 도구의 직업 타입을 반환합니다.
     */
    public String getJobType(ItemStack item) {
        if (!isEvolvingTool(item))
            return "";
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(jobKey, PersistentDataType.STRING, "");
    }

    /**
     * 도구에 경험치를 추가합니다.
     */
    public void addExp(ItemStack item, int amount) {
        if (!isEvolvingTool(item))
            return;

        ItemMeta meta = item.getItemMeta();
        int current = meta.getPersistentDataContainer()
                .getOrDefault(expKey, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(expKey, PersistentDataType.INTEGER, current + amount);
        item.setItemMeta(meta);
    }
}
