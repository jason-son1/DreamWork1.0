package com.dreamwork.core.gui.provider;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.InventoryProvider;
import com.dreamwork.core.gui.SmartInventory;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.item.custom.UnidentifiedOreItem;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

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

    public ForgeProvider(Player player, DreamWorkCore plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public void init(Inventory inv) {
        // 테두리 배치
        ItemStack border = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build();

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }

        // 네비게이션 버튼
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
                .lore("§7(광부 레벨 30 이상 필요)")
                .lore("")
                .lore("§e[클릭] 수리 메뉴 열기")
                .build());

        // 현재 모드에 따른 UI 표시
        switch (currentMode) {
            case IDENTIFICATION -> renderIdentificationUI(inv);
            case ALLOY -> renderAlloyUI(inv);
            case REPAIR -> renderRepairUI(inv);
            default -> {
            }
        }
    }

    /**
     * 감정 UI 렌더링
     */
    private void renderIdentificationUI(Inventory inv) {
        // 감정 슬롯 (빈 슬롯)
        inv.setItem(IDENTIFICATION_SLOT, ItemBuilder.of(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .name("§e§l광물 감정 슬롯")
                .lore("")
                .lore("§7미확인 광물을 여기에 놓으세요")
                .lore("")
                .lore("§a감정 비용: §f100 Dream")
                .build());

        // 감정 버튼
        inv.setItem(IDENTIFICATION_SLOT + 9, ItemBuilder.of(Material.EMERALD_BLOCK)
                .name("§a§l감정하기")
                .lore("")
                .lore("§7위에 놓인 미확인 광물을 감정합니다.")
                .build());

        // 감정 결과 확률 안내
        inv.setItem(IDENTIFICATION_SLOT + 2, ItemBuilder.of(Material.BOOK)
                .name("§e감정 결과 확률")
                .lore("")
                .lore("§840% §7자갈/부싯돌 (꽝)")
                .lore("§a25% §7철/금/구리 주괴")
                .lore("§b15% §7다이아몬드/에메랄드")
                .lore("§610% §7네더라이트 파편")
                .lore("§55% §7고대 화석")
                .lore("")
                .lore("§7광부 레벨이 높을수록")
                .lore("§7좋은 결과가 나올 확률이 상승합니다.")
                .build());
    }

    /**
     * 합금 UI 렌더링
     */
    private void renderAlloyUI(Inventory inv) {
        // 재료 슬롯들
        for (int slot : ALLOY_INGREDIENT_SLOTS) {
            inv.setItem(slot, ItemBuilder.of(Material.WHITE_STAINED_GLASS_PANE)
                    .name("§e재료 슬롯")
                    .lore("§7주괴를 넣으세요")
                    .build());
        }

        // 결과 미리보기
        inv.setItem(ALLOY_RESULT_SLOT, ItemBuilder.of(Material.BARRIER)
                .name("§c결과 없음")
                .lore("§7올바른 재료를 넣으면")
                .lore("§7합금을 제작할 수 있습니다.")
                .build());

        // 레시피 안내
        inv.setItem(16, ItemBuilder.of(Material.CRAFTING_TABLE)
                .name("§6합금 레시피")
                .lore("")
                .lore("§a강화된 강철:")
                .lore("  §7철 주괴 10개")
                .lore("  §7구리 주괴 10개")
                .lore("  §7금 주괴 5개")
                .lore("")
                .lore("§5드림워크 합금:")
                .lore("  §7철 주괴 20개")
                .lore("  §7금 주괴 10개")
                .lore("  §7네더라이트 파편 2개")
                .lore("  §7드림스톤 1개")
                .build());

        // 제작 버튼
        inv.setItem(34, ItemBuilder.of(Material.SMITHING_TABLE)
                .name("§a§l합금 제작")
                .lore("")
                .lore("§7재료를 넣고 클릭하세요")
                .build());
    }

    /**
     * 수리 UI 렌더링
     */
    private void renderRepairUI(Inventory inv) {
        int minerLevel = getMinerLevel();

        if (minerLevel < 30) {
            inv.setItem(22, ItemBuilder.of(Material.BARRIER)
                    .name("§c§l잠금")
                    .lore("")
                    .lore("§7광부 레벨 30 이상 필요")
                    .lore("§7현재 레벨: §e" + minerLevel)
                    .build());
            return;
        }

        // 수리 슬롯
        inv.setItem(20, ItemBuilder.of(Material.PURPLE_STAINED_GLASS_PANE)
                .name("§e수리할 장비")
                .lore("§7손상된 장비를 넣으세요")
                .build());

        // 재료 슬롯
        inv.setItem(24, ItemBuilder.of(Material.YELLOW_STAINED_GLASS_PANE)
                .name("§e수리 재료")
                .lore("§7수리에 필요한 재료를 넣으세요")
                .build());

        // 수리 버튼
        inv.setItem(31, ItemBuilder.of(Material.ANVIL)
                .name("§a§l수리하기")
                .lore("")
                .lore("§7비용: §f500 Dream")
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        // 네비게이션
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

        // 모드별 처리
        switch (currentMode) {
            case IDENTIFICATION -> handleIdentificationClick(event, slot);
            case ALLOY -> handleAlloyClick(event, slot);
            case REPAIR -> handleRepairClick(event, slot);
            default -> {
            }
        }
    }

    /**
     * 감정 모드 클릭 처리
     */
    private void handleIdentificationClick(InventoryClickEvent event, int slot) {
        // 감정 슬롯 클릭 - 아이템 넣기 허용
        if (slot == IDENTIFICATION_SLOT) {
            event.setCancelled(false);
            return;
        }

        // 감정 버튼 클릭
        if (slot == IDENTIFICATION_SLOT + 9) {
            ItemStack oreItem = inventory.getItem(IDENTIFICATION_SLOT);

            if (oreItem == null || oreItem.getType() == Material.LIGHT_BLUE_STAINED_GLASS_PANE) {
                player.sendMessage("§c[대장간] 감정할 미확인 광물을 넣어주세요.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // 미확인 광물인지 확인
            if (!isUnidentifiedOre(oreItem)) {
                player.sendMessage("§c[대장간] 이 아이템은 감정할 수 없습니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // 감정 실행
            performIdentification(oreItem);
        }
    }

    /**
     * 감정 실행
     */
    private void performIdentification(ItemStack oreItem) {
        int minerLevel = getMinerLevel();

        // 감정 애니메이션
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.ENCHANT,
                player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

        // 레벨 보너스 적용
        double levelBonus = minerLevel * 0.005; // 레벨당 0.5% 보너스

        // 확률 계산 (Plan 2.0 테이블 기반)
        double roll = ThreadLocalRandom.current().nextDouble();
        roll -= levelBonus; // 레벨 보너스로 좋은 결과 확률 증가

        ItemStack result;
        String message;

        if (roll < 0.05) {
            // 5% - 고대 화석
            result = ItemBuilder.of(Material.BONE)
                    .name("§5§l고대 화석")
                    .lore("")
                    .lore("§7고대 생물의 화석입니다.")
                    .lore("§7박물관에 기증하면 보상을 받습니다.")
                    .customModelData(10020)
                    .build();
            message = "§d§l[대박!] §5고대 화석§f을 발견했습니다!";
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        } else if (roll < 0.15) {
            // 10% - 네더라이트 파편
            int amount = ThreadLocalRandom.current().nextInt(1, 3);
            result = new ItemStack(Material.NETHERITE_SCRAP, amount);
            message = "§6[희귀!] §f네더라이트 파편 §e" + amount + "개§f를 획득했습니다!";
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        } else if (roll < 0.30) {
            // 15% - 다이아/에메랄드
            boolean isDiamond = ThreadLocalRandom.current().nextBoolean();
            Material material = isDiamond ? Material.DIAMOND : Material.EMERALD;
            int amount = ThreadLocalRandom.current().nextInt(1, 4);
            result = new ItemStack(material, amount);
            String name = isDiamond ? "다이아몬드" : "에메랄드";
            message = "§b[희귀!] §f" + name + " §e" + amount + "개§f를 획득했습니다!";
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

        } else if (roll < 0.55) {
            // 25% - 주괴
            Material material = switch (ThreadLocalRandom.current().nextInt(3)) {
                case 0 -> Material.IRON_INGOT;
                case 1 -> Material.GOLD_INGOT;
                default -> Material.COPPER_INGOT;
            };
            int amount = ThreadLocalRandom.current().nextInt(3, 9);
            result = new ItemStack(material, amount);
            String name = getItemName(material);
            message = "§a[성공] §f" + name + " §e" + amount + "개§f를 획득했습니다.";
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

        } else {
            // 45% - 꽝
            boolean isGravel = ThreadLocalRandom.current().nextBoolean();
            Material material = isGravel ? Material.GRAVEL : Material.FLINT;
            int amount = ThreadLocalRandom.current().nextInt(1, 4);
            result = new ItemStack(material, amount);
            String name = isGravel ? "자갈" : "부싯돌";
            message = "§7[꽝] §f" + name + " §e" + amount + "개§f... 아쉽네요.";
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
        }

        // 광물 소모
        oreItem.setAmount(oreItem.getAmount() - 1);
        if (oreItem.getAmount() <= 0) {
            inventory.setItem(IDENTIFICATION_SLOT, ItemBuilder.of(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                    .name("§e§l광물 감정 슬롯")
                    .lore("")
                    .lore("§7미확인 광물을 여기에 놓으세요")
                    .build());
        }

        // 결과 지급
        player.getInventory().addItem(result);
        player.sendMessage("§e[감정] " + message);
    }

    /**
     * 합금 모드 클릭 처리
     */
    private void handleAlloyClick(InventoryClickEvent event, int slot) {
        // 재료 슬롯들 - 아이템 넣기 허용
        for (int ingredientSlot : ALLOY_INGREDIENT_SLOTS) {
            if (slot == ingredientSlot) {
                event.setCancelled(false);
                // 다음 틱에 레시피 확인
                Bukkit.getScheduler().runTaskLater(plugin, this::checkAlloyRecipe, 1L);
                return;
            }
        }

        // 제작 버튼
        if (slot == 34) {
            craftAlloy();
        }
    }

    /**
     * 합금 레시피 확인
     */
    private void checkAlloyRecipe() {
        // 재료 수집
        int ironCount = 0, copperCount = 0, goldCount = 0, netheriteCount = 0, dreamstoneCount = 0;

        for (int slot : ALLOY_INGREDIENT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item == null)
                continue;

            switch (item.getType()) {
                case IRON_INGOT -> ironCount += item.getAmount();
                case COPPER_INGOT -> copperCount += item.getAmount();
                case GOLD_INGOT -> goldCount += item.getAmount();
                case NETHERITE_SCRAP -> netheriteCount += item.getAmount();
                default -> {
                }
            }

            // 드림스톤 확인 (커스텀 아이템)
            if (isDreamStone(item)) {
                dreamstoneCount += item.getAmount();
            }
        }

        // 강화된 강철 레시피 확인
        if (ironCount >= 10 && copperCount >= 10 && goldCount >= 5) {
            inventory.setItem(ALLOY_RESULT_SLOT, ItemBuilder.of(Material.IRON_INGOT)
                    .name("§a§l강화된 강철")
                    .lore("")
                    .lore("§7건축가의 보호 구역 설정에 사용됩니다.")
                    .customModelData(10030)
                    .build());
            return;
        }

        // 드림워크 합금 레시피 확인
        if (ironCount >= 20 && goldCount >= 10 && netheriteCount >= 2 && dreamstoneCount >= 1) {
            inventory.setItem(ALLOY_RESULT_SLOT, ItemBuilder.of(Material.NETHERITE_INGOT)
                    .name("§5§l드림워크 합금")
                    .lore("")
                    .lore("§7전설 곡괭이 제작에 필요합니다.")
                    .customModelData(10031)
                    .build());
            return;
        }

        // 매칭되는 레시피 없음
        inventory.setItem(ALLOY_RESULT_SLOT, ItemBuilder.of(Material.BARRIER)
                .name("§c결과 없음")
                .lore("§7올바른 재료를 넣으세요")
                .build());
    }

    /**
     * 합금 제작
     */
    private void craftAlloy() {
        ItemStack resultPreview = inventory.getItem(ALLOY_RESULT_SLOT);
        if (resultPreview == null || resultPreview.getType() == Material.BARRIER) {
            player.sendMessage("§c[대장간] 올바른 재료를 넣어주세요.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 재료 소모 (간단히 전부 삭제)
        for (int slot : ALLOY_INGREDIENT_SLOTS) {
            inventory.setItem(slot, ItemBuilder.of(Material.WHITE_STAINED_GLASS_PANE)
                    .name("§e재료 슬롯")
                    .lore("§7주괴를 넣으세요")
                    .build());
        }

        // 결과 지급
        player.getInventory().addItem(resultPreview.clone());

        // 초기화
        inventory.setItem(ALLOY_RESULT_SLOT, ItemBuilder.of(Material.BARRIER)
                .name("§c결과 없음")
                .build());

        player.sendMessage("§a[대장간] 합금 제작 완료!");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
    }

    /**
     * 수리 모드 클릭 처리
     */
    private void handleRepairClick(InventoryClickEvent event, int slot) {
        // TODO: 수리 기능 구현
        if (slot == 31) {
            player.sendMessage("§e[대장간] 수리 기능은 추후 업데이트 예정입니다.");
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        // 슬롯에 남은 아이템 반환
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
        return type.name().contains("STAINED_GLASS_PANE") ||
                type == Material.BARRIER ||
                type == Material.BOOK;
    }

    @Override
    public boolean isInteractive() {
        // 감정/합금 모드에서는 아이템 이동 허용
        return currentMode == ForgeMode.IDENTIFICATION || currentMode == ForgeMode.ALLOY;
    }

    // ==================== 유틸리티 ====================

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

    private String getItemName(Material material) {
        return switch (material) {
            case IRON_INGOT -> "철 주괴";
            case COPPER_INGOT -> "구리 주괴";
            case GOLD_INGOT -> "금 주괴";
            default -> material.name();
        };
    }

    /**
     * 대장간 모드
     */
    private enum ForgeMode {
        MAIN,
        IDENTIFICATION,
        ALLOY,
        REPAIR
    }

    // ==================== Static 팩토리 ====================

    /**
     * 대장간 GUI를 엽니다.
     */
    public static void open(Player player, DreamWorkCore plugin) {
        SmartInventory.builder()
                .title("§6§l대장간")
                .size(54)
                .provider(new ForgeProvider(player, plugin))
                .build()
                .open(player);
    }
}
