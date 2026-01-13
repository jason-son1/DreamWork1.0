package com.dreamwork.core.gui.provider;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.InventoryProvider;
import com.dreamwork.core.gui.SmartInventory;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 대장간 (The Forge) GUI Provider
 * 
 * <p>
 * Plan 2.0 기준:
 * - 미확인 광석 감정
 * - 합금 제작
 * - 장비 수리/강화
 * </p>
 */
public class ForgeProvider extends InventoryProvider {

    private final DreamWorkCore plugin;

    /** GUI 모드 */
    private ForgeMode currentMode = ForgeMode.MAIN;

    /** 감정 슬롯 */
    private static final int IDENTIFICATION_SLOT = 22;

    /** 합금 재료 슬롯들 */
    private static final int[] ALLOY_INGREDIENT_SLOTS = { 10, 11, 12, 19, 20, 21 };

    /** 합금 결과 슬롯 */
    private static final int ALLOY_RESULT_SLOT = 24;

    /** 메뉴 네비게이션 슬롯 */
    private static final int NAV_IDENTIFICATION = 1;
    private static final int NAV_ALLOY = 4;
    private static final int NAV_REPAIR = 7;

    // 레시피 및 설정 데이터 (Static Cache)
    private static final List<AlloyRecipe> ALLOY_RECIPES = new ArrayList<>();
    private static Map<String, Double> IDENTIFICATION_CHANCES = new LinkedHashMap<>();
    private static Map<String, IdentificationResult> IDENTIFICATION_RESULTS = new HashMap<>();
    private static double LEVEL_BONUS_PER_TIER = 0.005;
    private static int IDENTIFICATION_COST = 100;
    private static int REPAIR_COST = 500;
    private static int REPAIR_LEVEL_REQ = 30;

    static {
        // 실제 로드는 loadConfig()에서 수행
    }

    public static void loadConfig(DreamWorkCore plugin) {
        ALLOY_RECIPES.clear();
        IDENTIFICATION_CHANCES.clear();
        IDENTIFICATION_RESULTS.clear();

        File file = new File(plugin.getDataFolder(), "facilities/forge.yml");
        if (!file.exists()) {
            plugin.getLogger().warning("forge.yml 파일을 찾을 수 없습니다.");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // 1. 합금 레시피 로드
        if (config.contains("alloys")) {
            for (String key : config.getConfigurationSection("alloys").getKeys(false)) {
                try {
                    String path = "alloys." + key;
                    String displayName = config.getString(path + ".display_name");
                    Material resultMat = Material
                            .matchMaterial(config.getString(path + ".result_material", "IRON_INGOT"));
                    int customModelData = config.getInt(path + ".custom_model_data", 0);
                    List<String> lore = config.getStringList(path + ".lore");
                    List<String> ingredients = config.getStringList(path + ".ingredients");
                    int levelReq = config.getInt(path + ".level_required", 0);

                    ALLOY_RECIPES
                            .add(new AlloyRecipe(displayName, resultMat, customModelData, lore, ingredients, levelReq));
                } catch (Exception e) {
                    plugin.getLogger().warning("합금 레시피 로드 실패: " + key);
                }
            }
        }

        // 2. 감정 설정 로드
        IDENTIFICATION_COST = config.getInt("files.identification.cost", 100);
        LEVEL_BONUS_PER_TIER = config.getDouble("files.identification.level_bonus_per_tier", 0.005);

        if (config.contains("files.identification.chances")) {
            double currentChance = 0.0;
            for (String key : config.getConfigurationSection("files.identification.chances").getKeys(false)) {
                String path = "files.identification.chances." + key;
                double chance = config.getDouble(path + ".chance");
                currentChance += chance;
                IDENTIFICATION_CHANCES.put(key, currentChance);

                IdentificationResult result = new IdentificationResult();
                result.name = config.getString(path + ".name");
                result.material = Material.matchMaterial(config.getString(path + ".material", "STONE"));
                result.minAmount = config.getInt(path + ".min_amount", 1);
                result.maxAmount = config.getInt(path + ".max_amount", 1);
                result.broadcast = config.getBoolean(path + ".broadcast", false);
                result.customModelData = config.getInt(path + ".custom_model_data", 0);

                IDENTIFICATION_RESULTS.put(key, result);
            }
        }

        // 3. 수리 설정 로드
        REPAIR_COST = config.getInt("repair.base_cost", 500);
        REPAIR_LEVEL_REQ = config.getInt("repair.level_required", 30);
    }

    public ForgeProvider(Player player, DreamWorkCore plugin) {
        super(player);
        this.plugin = plugin;

        if (ALLOY_RECIPES.isEmpty()) {
            loadConfig(plugin);
        }
    }

    @Override
    public void init(Inventory inv) {
        ItemStack border = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++)
            inv.setItem(i, border);

        inv.setItem(NAV_IDENTIFICATION, ItemBuilder.of(Material.RAW_IRON)
                .name("§6§l광물 감정")
                .lore("")
                .lore("§7미확인 광물을 감정합니다.")
                .lore("")
                .lore("§e[클릭] 감정 메뉴 열기")
                .build());

        inv.setItem(NAV_ALLOY, ItemBuilder.of(Material.NETHERITE_INGOT)
                .name("§b§l합금 제작")
                .lore("")
                .lore("§7다양한 재료를 조합하여 합금을 만듭니다.")
                .lore("")
                .lore("§e[클릭] 합금 메뉴 열기")
                .build());

        inv.setItem(NAV_REPAIR, ItemBuilder.of(Material.ANVIL)
                .name("§a§l장비 수리")
                .lore("")
                .lore("§7장비를 수리하거나 강화합니다.")
                .lore("§7(광부 레벨 " + REPAIR_LEVEL_REQ + " 이상 필요)")
                .lore("")
                .lore("§e[클릭] 수리 메뉴 열기")
                .build());

        switch (currentMode) {
            case IDENTIFICATION -> renderIdentificationUI(inv);
            case ALLOY -> renderAlloyUI(inv);
            case REPAIR -> renderRepairUI(inv);
            default -> {
            }
        }
    }

    private void renderIdentificationUI(Inventory inv) {
        inv.setItem(IDENTIFICATION_SLOT, ItemBuilder.of(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .name("§e§l광물 감정 슬롯")
                .lore("")
                .lore("§7미확인 광물을 여기에 놓으세요")
                .lore("")
                .lore("§a감정 비용: §f" + IDENTIFICATION_COST + " Dream")
                .build());

        inv.setItem(IDENTIFICATION_SLOT + 9, ItemBuilder.of(Material.EMERALD_BLOCK)
                .name("§a§l감정하기")
                .lore("")
                .lore("§7위에 놓인 미확인 광물을 감정합니다.")
                .build());

        inv.setItem(IDENTIFICATION_SLOT + 2, ItemBuilder.of(Material.BOOK)
                .name("§e감정 결과 확률")
                .lore("")
                .lore("§7광부 레벨이 높을수록")
                .lore("§7좋은 결과가 나올 확률이 상승합니다.")
                .build());
    }

    private void renderAlloyUI(Inventory inv) {
        for (int slot : ALLOY_INGREDIENT_SLOTS) {
            inv.setItem(slot, ItemBuilder.of(Material.WHITE_STAINED_GLASS_PANE)
                    .name("§e재료 슬롯")
                    .lore("§7주괴를 넣으세요")
                    .build());
        }

        inv.setItem(ALLOY_RESULT_SLOT, ItemBuilder.of(Material.BARRIER)
                .name("§c결과 없음")
                .lore("§7올바른 재료를 넣으면")
                .lore("§7합금을 제작할 수 있습니다.")
                .build());

        ItemBuilder recipeBook = ItemBuilder.of(Material.CRAFTING_TABLE).name("§6합금 레시피").lore("");
        for (AlloyRecipe recipe : ALLOY_RECIPES) {
            recipeBook.lore("§a" + recipe.displayName.replace("&", "§") + ":");
            for (String ing : recipe.ingredients) {
                String[] parts = ing.split(":");
                recipeBook.lore("  §7" + getMaterialName(parts[0]) + " " + parts[1] + "개");
            }
            recipeBook.lore("");
        }
        inv.setItem(16, recipeBook.build());

        inv.setItem(34, ItemBuilder.of(Material.SMITHING_TABLE)
                .name("§a§l합금 제작")
                .lore("")
                .lore("§7재료를 넣고 클릭하세요")
                .build());
    }

    private void renderRepairUI(Inventory inv) {
        int minerLevel = getMinerLevel();
        if (minerLevel < REPAIR_LEVEL_REQ) {
            inv.setItem(22, ItemBuilder.of(Material.BARRIER)
                    .name("§c§l잠금")
                    .lore("")
                    .lore("§7광부 레벨 " + REPAIR_LEVEL_REQ + " 이상 필요")
                    .lore("§7현재 레벨: §e" + minerLevel)
                    .build());
            return;
        }

        inv.setItem(20,
                ItemBuilder.of(Material.PURPLE_STAINED_GLASS_PANE).name("§e수리할 장비").lore("§7손상된 장비를 넣으세요").build());
        inv.setItem(24,
                ItemBuilder.of(Material.YELLOW_STAINED_GLASS_PANE).name("§e수리 재료").lore("§7수리에 필요한 재료를 넣으세요").build());
        inv.setItem(31, ItemBuilder.of(Material.ANVIL).name("§a§l수리하기").lore("")
                .lore("§7비용: §f" + REPAIR_COST + " Dream").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == NAV_IDENTIFICATION) {
            currentMode = ForgeMode.IDENTIFICATION;
            refresh();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        if (slot == NAV_ALLOY) {
            currentMode = ForgeMode.ALLOY;
            refresh();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        if (slot == NAV_REPAIR) {
            currentMode = ForgeMode.REPAIR;
            refresh();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        switch (currentMode) {
            case IDENTIFICATION -> handleIdentificationClick(event, slot);
            case ALLOY -> handleAlloyClick(event, slot);
            case REPAIR -> handleRepairClick(event, slot);
            default -> {
            }
        }
    }

    private void handleIdentificationClick(InventoryClickEvent event, int slot) {
        if (slot == IDENTIFICATION_SLOT) {
            event.setCancelled(false);
            return;
        }

        if (slot == IDENTIFICATION_SLOT + 9) {
            ItemStack oreItem = inventory.getItem(IDENTIFICATION_SLOT);
            if (oreItem == null || oreItem.getType() == Material.LIGHT_BLUE_STAINED_GLASS_PANE) {
                player.sendMessage("§c[대장간] 감정할 미확인 광물을 넣어주세요.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (!isUnidentifiedOre(oreItem)) {
                player.sendMessage("§c[대장간] 이 아이템은 감정할 수 없습니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            performIdentification(oreItem);
        }
    }

    private void performIdentification(ItemStack oreItem) {
        int minerLevel = getMinerLevel();
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

        double levelBonus = minerLevel * LEVEL_BONUS_PER_TIER;
        double roll = ThreadLocalRandom.current().nextDouble();
        roll -= levelBonus;

        ItemStack result = null;
        String message = "§c결과 없음";

        // 확률 테이블 Loop
        for (Map.Entry<String, Double> entry : IDENTIFICATION_CHANCES.entrySet()) {
            if (roll < entry.getValue()) {
                IdentificationResult data = IDENTIFICATION_RESULTS.get(entry.getKey());
                if (data != null) {
                    Material mat = data.material;
                    if (mat == null)
                        mat = Material.COBBLESTONE; // fallback

                    int amount = ThreadLocalRandom.current().nextInt(data.minAmount, data.maxAmount + 1);
                    result = new ItemStack(mat, amount);
                    if (data.customModelData != 0) {
                        // Model data setting logic if needed
                    }
                    message = "§a[성공] " + data.name + " §e" + amount + "개§f를 획득했습니다.";
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    break;
                }
            }
        }

        if (result == null) {
            // Fallback (꽝)
            result = new ItemStack(Material.GRAVEL, 1);
            message = "§7[꽝] 자갈...";
        }

        oreItem.setAmount(oreItem.getAmount() - 1);
        if (oreItem.getAmount() <= 0) {
            inventory.setItem(IDENTIFICATION_SLOT, ItemBuilder.of(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                    .name("§e§l광물 감정 슬롯")
                    .lore("")
                    .lore("§7미확인 광물을 여기에 놓으세요")
                    .build());
        }

        player.getInventory().addItem(result);
        player.sendMessage("§e[감정] " + message);
    }

    private void handleAlloyClick(InventoryClickEvent event, int slot) {
        for (int ingredientSlot : ALLOY_INGREDIENT_SLOTS) {
            if (slot == ingredientSlot) {
                event.setCancelled(false);
                Bukkit.getScheduler().runTaskLater(plugin, this::checkAlloyRecipe, 1L);
                return;
            }
        }

        if (slot == 34) {
            craftAlloy();
        }
    }

    private void checkAlloyRecipe() {
        Map<String, Integer> currentIngredients = new HashMap<>();
        for (int slot : ALLOY_INGREDIENT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item == null)
                continue;

            String key = item.getType().name();
            if (isDreamStone(item))
                key = "DREAM_STONE";
            else if (item.getType() == Material.NETHERITE_SCRAP)
                key = "NETHERITE_SCRAP";

            currentIngredients.put(key, currentIngredients.getOrDefault(key, 0) + item.getAmount());
        }

        for (AlloyRecipe recipe : ALLOY_RECIPES) {
            boolean match = true;
            for (String ing : recipe.ingredients) {
                String[] parts = ing.split(":");
                String matName = parts[0];
                int amount = Integer.parseInt(parts[1]);

                if (currentIngredients.getOrDefault(matName, 0) < amount) {
                    match = false;
                    break;
                }
            }

            if (match) {
                ItemBuilder resultBuilder = ItemBuilder.of(recipe.resultMat)
                        .name(recipe.displayName.replace("&", "§"))
                        .customModelData(recipe.customModelData);
                for (String line : recipe.lore) {
                    resultBuilder.lore(line.replace("&", "§"));
                }
                inventory.setItem(ALLOY_RESULT_SLOT, resultBuilder.build());
                return;
            }
        }

        inventory.setItem(ALLOY_RESULT_SLOT,
                ItemBuilder.of(Material.BARRIER).name("§c결과 없음").lore("§7올바른 재료를 넣으세요").build());
    }

    private void craftAlloy() {
        ItemStack resultPreview = inventory.getItem(ALLOY_RESULT_SLOT);
        if (resultPreview == null || resultPreview.getType() == Material.BARRIER) {
            player.sendMessage("§c[대장간] 올바른 재료를 넣어주세요.");
            return;
        }

        AlloyRecipe matched = null;
        Map<String, Integer> currentIngredients = new HashMap<>();
        for (int slot : ALLOY_INGREDIENT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item == null)
                continue;
            String key = item.getType().name();
            if (isDreamStone(item))
                key = "DREAM_STONE";
            else if (item.getType() == Material.NETHERITE_SCRAP)
                key = "NETHERITE_SCRAP";
            currentIngredients.put(key, currentIngredients.getOrDefault(key, 0) + item.getAmount());
        }

        for (AlloyRecipe recipe : ALLOY_RECIPES) {
            boolean match = true;
            for (String ing : recipe.ingredients) {
                String[] parts = ing.split(":");
                if (currentIngredients.getOrDefault(parts[0], 0) < Integer.parseInt(parts[1])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                matched = recipe;
                break;
            }
        }

        if (matched != null) {
            for (String ing : matched.ingredients) {
                String[] parts = ing.split(":");
                String matName = parts[0];
                int toRemove = Integer.parseInt(parts[1]);

                for (int slot : ALLOY_INGREDIENT_SLOTS) {
                    ItemStack item = inventory.getItem(slot);
                    if (item == null)
                        continue;
                    String key = item.getType().name();
                    if (isDreamStone(item))
                        key = "DREAM_STONE";
                    else if (item.getType() == Material.NETHERITE_SCRAP)
                        key = "NETHERITE_SCRAP";

                    if (key.equals(matName)) {
                        int amount = item.getAmount();
                        int remove = Math.min(amount, toRemove);
                        item.setAmount(amount - remove);
                        toRemove -= remove;
                        if (toRemove <= 0)
                            break;
                    }
                }
            }

            player.getInventory().addItem(resultPreview.clone());
            inventory.setItem(ALLOY_RESULT_SLOT, ItemBuilder.of(Material.BARRIER).name("§c결과 없음").build());
            player.sendMessage("§a[대장간] 합금 제작 완료!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
        }
    }

    private void handleRepairClick(InventoryClickEvent event, int slot) {
        if (slot == 31) {
            player.sendMessage("§e[대장간] 수리 기능은 추후 업데이트 예정입니다.");
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        returnItemToPlayer(IDENTIFICATION_SLOT);
        for (int slot : ALLOY_INGREDIENT_SLOTS) {
            returnItemToPlayer(slot);
        }
    }

    private void returnItemToPlayer(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item != null && !isGuiItem(item)) {
            player.getInventory().addItem(item);
        }
    }

    private boolean isGuiItem(ItemStack item) {
        if (item == null)
            return true;
        Material type = item.getType();
        return type.name().contains("STAINED_GLASS_PANE") || type == Material.BARRIER || type == Material.BOOK;
    }

    @Override
    public boolean isInteractive() {
        return currentMode == ForgeMode.IDENTIFICATION || currentMode == ForgeMode.ALLOY;
    }

    private int getMinerLevel() {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;
        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"miner".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }

    private boolean isUnidentifiedOre(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        NamespacedKey key = new NamespacedKey(plugin, "unidentified_ore");
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private boolean isDreamStone(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        NamespacedKey key = new NamespacedKey(plugin, "dream_stone");
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private String getMaterialName(String key) {
        if ("DREAM_STONE".equals(key)) {
            return "드림 스톤";
        }
        Material mat = Material.matchMaterial(key);
        if (mat != null) {
            return getItemName(mat);
        }
        return key;
    }

    private String getItemName(Material material) {
        return switch (material) {
            case IRON_INGOT -> "철 주괴";
            case COPPER_INGOT -> "구리 주괴";
            case GOLD_INGOT -> "금 주괴";
            case NETHERITE_SCRAP -> "네더라이트 파편";
            default -> material.name();
        };
    }

    private enum ForgeMode {
        MAIN, IDENTIFICATION, ALLOY, REPAIR
    }

    public static void open(Player player, DreamWorkCore plugin) {
        SmartInventory.builder()
                .title("§6§l대장간")
                .size(54)
                .provider(new ForgeProvider(player, plugin))
                .build()
                .open(player);
    }

    private static class AlloyRecipe {
        String displayName;
        Material resultMat;
        int customModelData;
        List<String> lore;
        List<String> ingredients;
        int levelReq;

        public AlloyRecipe(String d, Material r, int c, List<String> l, List<String> i, int lr) {
            this.displayName = d;
            this.resultMat = r;
            this.customModelData = c;
            this.lore = l;
            this.ingredients = i;
            this.levelReq = lr;
        }
    }

    private static class IdentificationResult {
        String name;
        Material material;
        int minAmount, maxAmount;
        boolean broadcast;
        int customModelData;
    }
}
