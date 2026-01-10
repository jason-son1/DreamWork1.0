package com.dreamwork.core.item;

import com.dreamwork.core.DreamWorkCore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;

/**
 * 아이템 생성 팩토리
 * 
 * <p>
 * items.yml을 기반으로 커스텀 스탯 아이템을 생성합니다.
 * PDC(PersistentDataContainer)를 사용하여 스탯을 저장합니다.
 * </p>
 */
public class ItemFactory {

    private final DreamWorkCore plugin;
    private final Gson gson = new Gson();

    private final NamespacedKey STATS_KEY;
    private final NamespacedKey ITEM_ID_KEY;

    /** 아이템 템플릿 캐시 (itemId -> config) */
    private final Map<String, ItemTemplate> templates = new HashMap<>();

    public ItemFactory(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.STATS_KEY = new NamespacedKey(plugin, "stats");
        this.ITEM_ID_KEY = new NamespacedKey(plugin, "item_id");
        loadTemplates();
    }

    /**
     * items.yml에서 템플릿을 로드합니다.
     */
    public void loadTemplates() {
        templates.clear();

        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.exists()) {
            plugin.saveResource("items.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String itemId : config.getKeys(false)) {
            try {
                ConfigurationSection section = config.getConfigurationSection(itemId);
                if (section == null)
                    continue;

                ItemTemplate template = ItemTemplate.fromConfig(itemId, section);
                templates.put(itemId, template);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "아이템 템플릿 로드 실패: " + itemId, e);
            }
        }

        plugin.getLogger().info("ItemFactory: " + templates.size() + "개 아이템 템플릿 로드 완료");
    }

    /**
     * 아이템 ID로 아이템을 생성합니다.
     * 
     * @param itemId 아이템 ID
     * @return 생성된 아이템 (없으면 null)
     */
    public ItemStack createItem(String itemId) {
        return createItem(itemId, 1);
    }

    /**
     * 아이템 ID로 아이템을 생성합니다.
     * 
     * @param itemId 아이템 ID
     * @param amount 수량
     * @return 생성된 아이템 (없으면 null)
     */
    public ItemStack createItem(String itemId, int amount) {
        ItemTemplate template = templates.get(itemId);
        if (template == null)
            return null;

        return buildItem(template, amount);
    }

    /**
     * 템플릿으로 아이템을 빌드합니다.
     */
    private ItemStack buildItem(ItemTemplate template, int amount) {
        Material material = Material.matchMaterial(template.material);
        if (material == null)
            material = Material.PAPER;

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        // 이름
        meta.displayName(Component.text(translateColors(template.name)));

        // Lore + 스탯 표시
        List<Component> lore = new ArrayList<>();
        if (template.lore != null) {
            for (String line : template.lore) {
                lore.add(Component.text(translateColors(line)));
            }
        }

        // 스탯 Lore 자동 생성
        if (template.stats != null && !template.stats.isEmpty()) {
            lore.add(Component.text(""));
            lore.add(Component.text("§6§l[ 스탯 ]"));
            for (Map.Entry<String, Double> entry : template.stats.entrySet()) {
                String statName = getStatDisplayName(entry.getKey());
                double value = entry.getValue();
                String prefix = value >= 0 ? "§a+" : "§c";
                lore.add(Component.text("§7" + statName + ": " + prefix + (int) value));
            }
        }

        meta.lore(lore);

        // 인챈트
        if (template.enchants != null) {
            for (Map.Entry<String, Integer> entry : template.enchants.entrySet()) {
                Enchantment ench = Enchantment.getByName(entry.getKey().toUpperCase());
                if (ench != null) {
                    meta.addEnchant(ench, entry.getValue(), true);
                }
            }
        }

        // PDC에 스탯 저장
        if (template.stats != null && !template.stats.isEmpty()) {
            String statsJson = gson.toJson(template.stats);
            meta.getPersistentDataContainer().set(STATS_KEY, PersistentDataType.STRING, statsJson);
        }

        // PDC에 아이템 ID 저장
        meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, template.id);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 아이템에 스탯을 적용합니다.
     * 
     * @param item  아이템
     * @param stats 스탯 맵
     * @return 스탯이 적용된 아이템
     */
    public ItemStack applyStats(ItemStack item, Map<String, Double> stats) {
        if (item == null || stats == null || stats.isEmpty())
            return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        // 기존 스탯과 병합
        Map<String, Double> existingStats = readStats(item);
        existingStats.putAll(stats);

        // PDC 저장
        String statsJson = gson.toJson(existingStats);
        meta.getPersistentDataContainer().set(STATS_KEY, PersistentDataType.STRING, statsJson);

        // Lore 업데이트
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        // 기존 스탯 Lore 제거 후 재생성
        lore.removeIf(c -> c.toString().contains("스탯"));

        lore.add(Component.text(""));
        lore.add(Component.text("§6§l[ 스탯 ]"));
        for (Map.Entry<String, Double> entry : existingStats.entrySet()) {
            String statName = getStatDisplayName(entry.getKey());
            double value = entry.getValue();
            String prefix = value >= 0 ? "§a+" : "§c";
            lore.add(Component.text("§7" + statName + ": " + prefix + (int) value));
        }

        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 아이템에서 스탯을 읽습니다.
     * 
     * @param item 아이템
     * @return 스탯 맵 (없으면 빈 맵)
     */
    public Map<String, Double> readStats(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return new HashMap<>();

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String statsJson = pdc.get(STATS_KEY, PersistentDataType.STRING);

        if (statsJson == null || statsJson.isEmpty())
            return new HashMap<>();

        Type type = new TypeToken<Map<String, Double>>() {
        }.getType();
        try {
            return gson.fromJson(statsJson, type);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * 아이템의 커스텀 ID를 반환합니다.
     */
    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        return item.getItemMeta().getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.STRING);
    }

    /**
     * 등록된 아이템 ID 목록을 반환합니다.
     */
    public Set<String> getItemIds() {
        return Collections.unmodifiableSet(templates.keySet());
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

    // ==================== 내부 클래스 ====================

    /**
     * 아이템 템플릿 데이터
     */
    public static class ItemTemplate {
        public String id;
        public String material;
        public String name;
        public List<String> lore;
        public Map<String, Double> stats;
        public Map<String, Integer> enchants;

        public static ItemTemplate fromConfig(String id, ConfigurationSection section) {
            ItemTemplate t = new ItemTemplate();
            t.id = id;
            t.material = section.getString("material", "PAPER");
            t.name = section.getString("name", id);
            t.lore = section.getStringList("lore");

            // Stats
            ConfigurationSection statsSection = section.getConfigurationSection("stats");
            if (statsSection != null) {
                t.stats = new HashMap<>();
                for (String key : statsSection.getKeys(false)) {
                    t.stats.put(key, statsSection.getDouble(key));
                }
            }

            // Enchants
            ConfigurationSection enchSection = section.getConfigurationSection("enchants");
            if (enchSection != null) {
                t.enchants = new HashMap<>();
                for (String key : enchSection.getKeys(false)) {
                    t.enchants.put(key, enchSection.getInt(key));
                }
            }

            return t;
        }
    }
}
