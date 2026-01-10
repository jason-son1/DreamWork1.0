package com.dreamwork.core.item;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 아이템 빌더 유틸리티
 * 
 * <p>
 * Fluent API로 아이템을 쉽게 생성합니다.
 * </p>
 * 
 * <h2>사용법:</h2>
 * 
 * <pre>{@code
 * ItemStack item = ItemBuilder.of(Material.DIAMOND_SWORD)
 *         .name("§6화염의 검")
 *         .lore("§7불타는 힘을 가진 검")
 *         .stat("str", 15)
 *         .stat("crit", 5)
 *         .enchant(Enchantment.FIRE_ASPECT, 2)
 *         .build();
 * }</pre>
 */
public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;
    private final List<Component> lore = new ArrayList<>();
    private final Map<String, Double> stats = new HashMap<>();

    private ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    private ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
        if (meta.lore() != null) {
            this.lore.addAll(meta.lore());
        }
    }

    /**
     * Material로 빌더를 생성합니다.
     */
    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    /**
     * 기존 아이템으로 빌더를 생성합니다.
     */
    public static ItemBuilder from(ItemStack item) {
        return new ItemBuilder(item);
    }

    /**
     * 아이템 이름을 설정합니다.
     */
    public ItemBuilder name(String name) {
        meta.displayName(Component.text(translateColors(name)));
        return this;
    }

    /**
     * Lore 한 줄을 추가합니다.
     */
    public ItemBuilder lore(String... lines) {
        for (String line : lines) {
            lore.add(Component.text(translateColors(line)));
        }
        return this;
    }

    /**
     * 빈 Lore 줄을 추가합니다.
     */
    public ItemBuilder loreBlank() {
        lore.add(Component.text(""));
        return this;
    }

    /**
     * 스탯을 추가합니다.
     */
    public ItemBuilder stat(String statName, double value) {
        stats.put(statName, value);
        return this;
    }

    /**
     * 스탯 맵 전체를 추가합니다.
     */
    public ItemBuilder stats(Map<String, Double> stats) {
        this.stats.putAll(stats);
        return this;
    }

    /**
     * 인챈트를 추가합니다.
     */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    /**
     * 아이템 플래그를 추가합니다.
     */
    public ItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    /**
     * 수량을 설정합니다.
     */
    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    /**
     * 언브레이커블 설정
     */
    public ItemBuilder unbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    /**
     * 커스텀 모델 데이터 설정
     */
    public ItemBuilder customModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    /**
     * 아이템을 빌드합니다.
     */
    public ItemStack build() {
        // 스탯 Lore 자동 생성
        if (!stats.isEmpty()) {
            lore.add(Component.text(""));
            lore.add(Component.text("§6§l[ 스탯 ]"));
            for (Map.Entry<String, Double> entry : stats.entrySet()) {
                String statName = getStatDisplayName(entry.getKey());
                double value = entry.getValue();
                String prefix = value >= 0 ? "§a+" : "§c";
                lore.add(Component.text("§7" + statName + ": " + prefix + (int) value));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 아이템을 빌드하고 ItemFactory로 스탯을 PDC에 저장합니다.
     */
    public ItemStack build(ItemFactory factory) {
        ItemStack result = build();
        if (!stats.isEmpty()) {
            factory.applyStats(result, stats);
        }
        return result;
    }

    private String translateColors(String text) {
        return text.replace("&", "§");
    }

    private String getStatDisplayName(String key) {
        return switch (key.toLowerCase()) {
            case "str", "strength" -> "힘";
            case "dex", "dexterity" -> "민첩";
            case "con", "constitution" -> "체력";
            case "int", "intelligence" -> "지능";
            case "luck", "luk" -> "행운";
            case "crit" -> "치명타";
            case "def", "defense" -> "방어력";
            default -> key;
        };
    }
}
