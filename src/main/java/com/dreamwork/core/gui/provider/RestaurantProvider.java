package com.dreamwork.core.gui.provider;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.InventoryProvider;
import com.dreamwork.core.gui.SmartInventory;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * 레스토랑 & 주점 GUI Provider
 * 
 * <p>
 * 농부의 고품질 작물로 버프 음식을 제작하고,
 * 각종 음료를 양조할 수 있는 타운 시설
 * </p>
 */
public class RestaurantProvider extends InventoryProvider {

    private final DreamWorkCore plugin;

    /** 현재 모드 (0: 요리, 1: 양조, 2: 메뉴) */
    private int currentMode = 0;

    /** 요리 레시피 목록 */
    private static final List<Recipe> COOKING_RECIPES = new ArrayList<>();

    /** 양조 레시피 목록 */
    private static final List<Recipe> BREWING_RECIPES = new ArrayList<>();

    static {
        // 요리 레시피
        COOKING_RECIPES.add(new Recipe(
                "hearty_stew", "§a든든한 스튜",
                Arrays.asList("CARROT:3", "POTATO:3", "COOKED_BEEF:1"),
                Arrays.asList(
                        new EffectData(PotionEffectType.STRENGTH, 1, 180),
                        new EffectData(PotionEffectType.SATURATION, 0, 60)),
                30));

        COOKING_RECIPES.add(new Recipe(
                "golden_feast", "§6황금빛 만찬",
                Arrays.asList("WHEAT:5", "GOLDEN_CARROT:2", "HONEY_BOTTLE:1"),
                Arrays.asList(
                        new EffectData(PotionEffectType.REGENERATION, 1, 300),
                        new EffectData(PotionEffectType.ABSORPTION, 1, 300)),
                50));

        COOKING_RECIPES.add(new Recipe(
                "seafood_platter", "§b해산물 모둠",
                Arrays.asList("COD:2", "SALMON:2", "TROPICAL_FISH:1"),
                Arrays.asList(
                        new EffectData(PotionEffectType.WATER_BREATHING, 0, 600),
                        new EffectData(PotionEffectType.DOLPHINS_GRACE, 0, 300)),
                40));

        // 양조 레시피
        BREWING_RECIPES.add(new Recipe(
                "strength_tonic", "§c힘의 강장제",
                Arrays.asList("NETHER_WART:1", "BLAZE_POWDER:1", "SUGAR:2"),
                Arrays.asList(
                        new EffectData(PotionEffectType.STRENGTH, 1, 180)),
                30));

        BREWING_RECIPES.add(new Recipe(
                "swiftness_elixir", "§e신속의 비약",
                Arrays.asList("NETHER_WART:1", "SUGAR:3", "RABBIT_FOOT:1"),
                Arrays.asList(
                        new EffectData(PotionEffectType.SPEED, 1, 240)),
                30));

        BREWING_RECIPES.add(new Recipe(
                "miners_brew", "§7광부의 술",
                Arrays.asList("COAL:2", "IRON_INGOT:1", "GLOWSTONE_DUST:1"),
                Arrays.asList(
                        new EffectData(PotionEffectType.HASTE, 1, 300),
                        new EffectData(PotionEffectType.NIGHT_VISION, 0, 300)),
                40));
    }

    public RestaurantProvider(Player player, DreamWorkCore plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public void init(Inventory inventory) {
        renderBackground(inventory);
        renderModeButtons(inventory);
        renderContent(inventory);
    }

    /**
     * 배경 렌더링
     */
    private void renderBackground(Inventory inventory) {
        ItemStack glass = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name("§8")
                .build();

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }
    }

    /**
     * 모드 버튼 렌더링
     */
    private void renderModeButtons(Inventory inventory) {
        // 요리 모드
        ItemStack cookingBtn = ItemBuilder.of(currentMode == 0 ? Material.CAMPFIRE : Material.COAL)
                .name(currentMode == 0 ? "§6§l[요리]" : "§7요리")
                .lore("§7버프 음식을 조리합니다")
                .build();
        inventory.setItem(0, cookingBtn);

        // 양조 모드
        ItemStack brewingBtn = ItemBuilder.of(currentMode == 1 ? Material.BREWING_STAND : Material.GLASS_BOTTLE)
                .name(currentMode == 1 ? "§5§l[양조]" : "§7양조")
                .lore("§7특수 물약을 양조합니다")
                .build();
        inventory.setItem(1, brewingBtn);

        // 메뉴 모드
        ItemStack menuBtn = ItemBuilder.of(currentMode == 2 ? Material.BOOK : Material.PAPER)
                .name(currentMode == 2 ? "§e§l[메뉴판]" : "§7메뉴판")
                .lore("§7음식/음료 효과를 확인합니다")
                .build();
        inventory.setItem(2, menuBtn);

        // 닫기 버튼
        ItemStack closeBtn = ItemBuilder.of(Material.BARRIER)
                .name("§c닫기")
                .build();
        inventory.setItem(8, closeBtn);
    }

    /**
     * 콘텐츠 렌더링
     */
    private void renderContent(Inventory inventory) {
        switch (currentMode) {
            case 0 -> renderCookingContent(inventory);
            case 1 -> renderBrewingContent(inventory);
            case 2 -> renderMenuContent(inventory);
        }
    }

    /**
     * 요리 콘텐츠 렌더링
     */
    private void renderCookingContent(Inventory inventory) {
        int slot = 19;
        for (Recipe recipe : COOKING_RECIPES) {
            if (slot > 43)
                break;

            boolean canCraft = checkIngredients(recipe);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7재료:");
            for (String ing : recipe.ingredients) {
                String[] parts = ing.split(":");
                String name = parts[0];
                int amount = Integer.parseInt(parts[1]);
                lore.add("§8- §f" + name + " x" + amount);
            }
            lore.add("");
            lore.add("§7효과:");
            for (EffectData effect : recipe.effects) {
                lore.add("§a- " + effect.type.getKey().getKey() + " Lv." + (effect.amplifier + 1) + " ("
                        + (effect.duration) + "초)");
            }
            lore.add("");
            lore.add("§8필요 레벨: §e" + recipe.levelRequired);
            lore.add(canCraft ? "§a[클릭] 조리하기" : "§c재료 부족");

            ItemStack item = ItemBuilder.of(canCraft ? Material.COOKED_BEEF : Material.ROTTEN_FLESH)
                    .name(recipe.displayName)
                    .lore(lore.toArray(new String[0]))
                    .build();

            inventory.setItem(slot, item);

            slot++;
            if (slot == 26)
                slot = 28;
            if (slot == 35)
                slot = 37;
        }
    }

    /**
     * 양조 콘텐츠 렌더링
     */
    private void renderBrewingContent(Inventory inventory) {
        int slot = 19;
        for (Recipe recipe : BREWING_RECIPES) {
            if (slot > 43)
                break;

            boolean canCraft = checkIngredients(recipe);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7재료:");
            for (String ing : recipe.ingredients) {
                String[] parts = ing.split(":");
                String name = parts[0];
                int amount = Integer.parseInt(parts[1]);
                lore.add("§8- §f" + name + " x" + amount);
            }
            lore.add("");
            lore.add("§7효과:");
            for (EffectData effect : recipe.effects) {
                lore.add("§a- " + effect.type.getKey().getKey() + " Lv." + (effect.amplifier + 1) + " ("
                        + (effect.duration) + "초)");
            }
            lore.add("");
            lore.add("§8필요 레벨: §e" + recipe.levelRequired);
            lore.add(canCraft ? "§a[클릭] 양조하기" : "§c재료 부족");

            ItemStack item = ItemBuilder.of(canCraft ? Material.POTION : Material.GLASS_BOTTLE)
                    .name(recipe.displayName)
                    .lore(lore.toArray(new String[0]))
                    .build();

            inventory.setItem(slot, item);

            slot++;
            if (slot == 26)
                slot = 28;
            if (slot == 35)
                slot = 37;
        }
    }

    /**
     * 메뉴 콘텐츠 렌더링
     */
    private void renderMenuContent(Inventory inventory) {
        ItemStack info = ItemBuilder.of(Material.BOOK)
                .name("§e레스토랑 안내")
                .lore("")
                .lore("§7레스토랑에서는 고품질 작물로")
                .lore("§7버프 음식을 만들 수 있습니다.")
                .lore("")
                .lore("§a요리: §f농부의 작물로 버프 음식 제작")
                .lore("§5양조: §f특수 재료로 강력한 물약 제작")
                .lore("")
                .lore("§8* 3성 작물 사용 시 효과 강화!")
                .build();
        inventory.setItem(22, info);
    }

    /**
     * 재료 체크
     */
    private boolean checkIngredients(Recipe recipe) {
        for (String ing : recipe.ingredients) {
            String[] parts = ing.split(":");
            Material mat = Material.matchMaterial(parts[0]);
            if (mat == null)
                continue;

            int required = Integer.parseInt(parts[1]);
            int count = 0;

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == mat) {
                    count += item.getAmount();
                }
            }

            if (count < required)
                return false;
        }
        return true;
    }

    /**
     * 레시피 제작
     */
    private void craftRecipe(Recipe recipe) {
        // 재료 소모
        for (String ing : recipe.ingredients) {
            String[] parts = ing.split(":");
            Material mat = Material.matchMaterial(parts[0]);
            if (mat == null)
                continue;

            int toRemove = Integer.parseInt(parts[1]);
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == mat) {
                    int remove = Math.min(toRemove, item.getAmount());
                    item.setAmount(item.getAmount() - remove);
                    toRemove -= remove;
                    if (toRemove <= 0)
                        break;
                }
            }
        }

        // 효과 적용
        for (EffectData effect : recipe.effects) {
            player.addPotionEffect(new PotionEffect(effect.type, effect.duration * 20, effect.amplifier));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
        player.sendMessage("§a[레스토랑] §f" + recipe.displayName + "§f을(를) 즐겼습니다!");

        player.closeInventory();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getSlot();

        // 모드 버튼
        if (slot == 0) {
            currentMode = 0;
            refresh();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        } else if (slot == 1) {
            currentMode = 1;
            refresh();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        } else if (slot == 2) {
            currentMode = 2;
            refresh();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        } else if (slot == 8) {
            player.closeInventory();
        } else {
            // 레시피 클릭
            int recipeIndex = getRecipeIndex(slot);
            if (recipeIndex >= 0) {
                List<Recipe> recipes = currentMode == 0 ? COOKING_RECIPES : BREWING_RECIPES;
                if (recipeIndex < recipes.size()) {
                    Recipe recipe = recipes.get(recipeIndex);
                    if (checkIngredients(recipe)) {
                        craftRecipe(recipe);
                    }
                }
            }
        }
    }

    private int getRecipeIndex(int slot) {
        int[] slots = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot)
                return i;
        }
        return -1;
    }

    /**
     * GUI 열기
     */
    public static void open(Player player, DreamWorkCore plugin) {
        SmartInventory.builder()
                .title("§6§l레스토랑 & 주점")
                .size(54)
                .provider(new RestaurantProvider(player, plugin))
                .build()
                .open(player);
    }

    /**
     * 레시피 데이터 클래스
     */
    private static class Recipe {
        @SuppressWarnings("unused")
        final String id;
        final String displayName;
        final List<String> ingredients;
        final List<EffectData> effects;
        final int levelRequired;

        Recipe(String id, String displayName, List<String> ingredients, List<EffectData> effects, int levelRequired) {
            this.id = id;
            this.displayName = displayName;
            this.ingredients = ingredients;
            this.effects = effects;
            this.levelRequired = levelRequired;
        }
    }

    /**
     * 효과 데이터 클래스
     */
    private static class EffectData {
        final PotionEffectType type;
        final int amplifier;
        final int duration; // 초

        EffectData(PotionEffectType type, int amplifier, int durationSeconds) {
            this.type = type;
            this.amplifier = amplifier;
            this.duration = durationSeconds;
        }
    }
}
