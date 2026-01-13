package com.dreamwork.core.item.custom;

import com.dreamwork.core.DreamWorkCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * 합금 레시피 정의
 * 
 * <p>
 * Plan 2.0 기준:
 * - 대장간에서만 제작 가능
 * - 강화된 강철, 드림워크 합금 등
 * </p>
 */
public class AlloyRecipes {

    private final DreamWorkCore plugin;
    private final NamespacedKey alloyTypeKey;

    /** 합금 레시피 목록 */
    private final Map<String, AlloyRecipe> recipes = new LinkedHashMap<>();

    public AlloyRecipes(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.alloyTypeKey = new NamespacedKey(plugin, "alloy_type");
        initRecipes();
    }

    /**
     * 레시피 초기화
     */
    private void initRecipes() {
        // 강화된 강철
        recipes.put("reinforced_steel", new AlloyRecipe(
                "reinforced_steel", "강화된 강철",
                Map.of(
                        Material.IRON_INGOT, 10,
                        Material.COPPER_INGOT, 10,
                        Material.GOLD_INGOT, 5),
                createReinforcedSteel()));

        // 드림워크 합금
        recipes.put("dreamwork_alloy", new AlloyRecipe(
                "dreamwork_alloy", "드림워크 합금",
                Map.of(
                        Material.IRON_INGOT, 20,
                        Material.GOLD_INGOT, 10,
                        Material.NETHERITE_SCRAP, 2
                // Note: 드림스톤 1개 필요 (PDC 체크)
                ),
                createDreamworkAlloy()));

        // 건축가용 합금
        recipes.put("architect_alloy", new AlloyRecipe(
                "architect_alloy", "건축가의 합금",
                Map.of(
                        Material.IRON_INGOT, 15,
                        Material.COPPER_INGOT, 15,
                        Material.BRICK, 10),
                createArchitectAlloy()));
    }

    // ====================== 합금 아이템 생성 ======================

    /**
     * 강화된 강철 생성
     */
    public ItemStack createReinforcedSteel() {
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "강화된 강철");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "여러 금속을 합금하여 만든 강철.",
                "",
                ChatColor.YELLOW + "용도: " + ChatColor.WHITE + "건축가 보호 구역 설정",
                ChatColor.YELLOW + "      " + ChatColor.WHITE + "중급 도구 제작",
                "",
                ChatColor.DARK_GRAY + "[대장간 전용 제작]"));

        // 발광 효과
        meta.setEnchantmentGlintOverride(true);

        // PDC 식별자
        meta.getPersistentDataContainer().set(alloyTypeKey, PersistentDataType.STRING, "reinforced_steel");
        meta.setCustomModelData(10030);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 드림워크 합금 생성
     */
    public ItemStack createDreamworkAlloy() {
        ItemStack item = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.DARK_PURPLE + "§l드림워크 합금");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.LIGHT_PURPLE + "드림스톤의 힘이 깃든 신비로운 합금.",
                "",
                ChatColor.GOLD + "등급: " + ChatColor.LIGHT_PURPLE + "전설",
                ChatColor.YELLOW + "용도: " + ChatColor.WHITE + "전설 도구/장비 제작",
                "",
                ChatColor.GRAY + "§o\"만지면 손끝이 따뜻해진다...\"",
                "",
                ChatColor.DARK_GRAY + "[대장간 전용 제작]"));

        // 발광 효과
        meta.setEnchantmentGlintOverride(true);

        // PDC 식별자
        meta.getPersistentDataContainer().set(alloyTypeKey, PersistentDataType.STRING, "dreamwork_alloy");
        meta.setCustomModelData(10031);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 건축가의 합금 생성
     */
    public ItemStack createArchitectAlloy() {
        ItemStack item = new ItemStack(Material.COPPER_INGOT);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "건축가의 합금");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "건축물의 기반을 튼튼히 하는 특수 합금.",
                "",
                ChatColor.YELLOW + "용도: " + ChatColor.WHITE + "건축 블록 권한 확장",
                ChatColor.YELLOW + "      " + ChatColor.WHITE + "특수 건축 자재 제작",
                "",
                ChatColor.DARK_GRAY + "[대장간 전용 제작]"));

        // 발광 효과
        meta.setEnchantmentGlintOverride(true);

        // PDC 식별자
        meta.getPersistentDataContainer().set(alloyTypeKey, PersistentDataType.STRING, "architect_alloy");
        meta.setCustomModelData(10032);

        item.setItemMeta(meta);
        return item;
    }

    // ====================== 유틸리티 ======================

    /**
     * 레시피 목록 반환
     */
    public Map<String, AlloyRecipe> getRecipes() {
        return Collections.unmodifiableMap(recipes);
    }

    /**
     * 레시피 조회
     */
    public AlloyRecipe getRecipe(String id) {
        return recipes.get(id);
    }

    /**
     * 아이템이 합금인지 확인
     */
    public boolean isAlloy(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(alloyTypeKey, PersistentDataType.STRING);
    }

    /**
     * 합금 타입 반환
     */
    public String getAlloyType(ItemStack item) {
        if (!isAlloy(item))
            return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(alloyTypeKey, PersistentDataType.STRING);
    }

    /**
     * 합금 레시피 데이터 클래스
     */
    public static class AlloyRecipe {
        public final String id;
        public final String displayName;
        public final Map<Material, Integer> ingredients;
        public final ItemStack result;

        public AlloyRecipe(String id, String displayName, Map<Material, Integer> ingredients, ItemStack result) {
            this.id = id;
            this.displayName = displayName;
            this.ingredients = ingredients;
            this.result = result;
        }
    }
}
