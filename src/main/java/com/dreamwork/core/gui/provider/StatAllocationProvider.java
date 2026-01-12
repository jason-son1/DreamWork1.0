package com.dreamwork.core.gui.provider;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.InventoryProvider;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.model.UserData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 스탯 포인트 할당 GUI 제공자
 * <p>
 * 플레이어가 스탯 포인트를 원하는 능력치에 배분합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class StatAllocationProvider extends InventoryProvider {

    private final DreamWorkCore plugin;

    private static final int[] STAT_SLOTS = { 10, 12, 14, 16, 22 };
    private static final String[] STAT_NAMES = { "STR", "DEX", "CON", "INT", "LUK" };
    private static final String[] STAT_DISPLAY = { "힘", "민첩", "체력", "지능", "행운" };
    private static final Material[] STAT_ICONS = {
            Material.IRON_SWORD, Material.BOW, Material.SHIELD,
            Material.BOOK, Material.RABBIT_FOOT
    };
    private static final String[] STAT_COLORS = { "§c", "§a", "§6", "§b", "§e" };

    public StatAllocationProvider(Player player, DreamWorkCore plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public void init(Inventory inventory) {
        UserData userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null)
            return;

        int availablePoints = userData.getStatPoints();
        int[] stats = { userData.getStr(), userData.getDex(), userData.getCon(),
                userData.getIntel(), userData.getLuk() };

        // 스탯 아이템 배치
        for (int i = 0; i < STAT_NAMES.length; i++) {
            ItemStack item = createStatItem(i, stats[i], availablePoints);
            inventory.setItem(STAT_SLOTS[i], item);
        }

        // 정보 패널
        ItemStack info = ItemBuilder.of(Material.NETHER_STAR)
                .name("§e§l스탯 포인트")
                .lore("")
                .lore("§7사용 가능: §e" + availablePoints + " §7포인트")
                .lore("")
                .lore("§7레벨업 시 스탯 포인트를 획득합니다.")
                .lore("§7스탯을 클릭하여 포인트를 배분하세요.")
                .lore("")
                .lore("§a좌클릭: §f+1 포인트")
                .lore("§aShift+좌클릭: §f+5 포인트")
                .build();
        inventory.setItem(4, info);

        // 초기화 버튼
        ItemStack reset = ItemBuilder.of(Material.BARRIER)
                .name("§c§l스탯 초기화")
                .lore("")
                .lore("§7모든 스탯을 초기화합니다.")
                .lore("§7(보류 - 추후 구현)")
                .build();
        inventory.setItem(40, reset);

        // 테두리
        ItemStack border = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        for (int i = 0; i < 45; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, border);
            }
        }
    }

    /**
     * 스탯 아이템을 생성합니다.
     */
    private ItemStack createStatItem(int statIndex, int currentValue, int availablePoints) {
        String color = STAT_COLORS[statIndex];
        String name = STAT_DISPLAY[statIndex];
        String bonus = getStatBonus(statIndex);

        return ItemBuilder.of(STAT_ICONS[statIndex])
                .name(color + "§l" + name + " (" + STAT_NAMES[statIndex] + ")")
                .lore("")
                .lore("§7현재 값: " + color + currentValue)
                .lore("")
                .lore("§7효과: " + bonus)
                .lore("")
                .lore(availablePoints > 0 ? "§a[클릭] 포인트 투자" : "§c포인트 부족")
                .build();
    }

    /**
     * 스탯별 효과 설명을 반환합니다.
     */
    private String getStatBonus(int statIndex) {
        return switch (statIndex) {
            case 0 -> "§c공격력 +0.5 per point";
            case 1 -> "§a공격속도/이동속도 +0.5% per point";
            case 2 -> "§6최대 체력 +0.5 per point";
            case 3 -> "§b최대 마나 +2 per point";
            case 4 -> "§e드롭률/치명타 확률 +0.1% per point";
            default -> "";
        };
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getSlot();
        int statIndex = -1;

        for (int i = 0; i < STAT_SLOTS.length; i++) {
            if (STAT_SLOTS[i] == slot) {
                statIndex = i;
                break;
            }
        }

        if (statIndex < 0)
            return;

        UserData userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null)
            return;

        int availablePoints = userData.getStatPoints();
        if (availablePoints <= 0) {
            player.sendMessage("§c[스탯] 사용 가능한 포인트가 없습니다.");
            return;
        }

        int pointsToAdd = event.isShiftClick() ? Math.min(5, availablePoints) : 1;

        // 스탯 증가
        switch (statIndex) {
            case 0 -> userData.setStr(userData.getStr() + pointsToAdd);
            case 1 -> userData.setDex(userData.getDex() + pointsToAdd);
            case 2 -> userData.setCon(userData.getCon() + pointsToAdd);
            case 3 -> userData.setIntel(userData.getIntel() + pointsToAdd);
            case 4 -> userData.setLuk(userData.getLuk() + pointsToAdd);
        }

        userData.setStatPoints(availablePoints - pointsToAdd);
        userData.markDirty();

        player.sendMessage("§a[스탯] §f" + STAT_DISPLAY[statIndex] + " §7+" + pointsToAdd +
                " §7(남은 포인트: §e" + (availablePoints - pointsToAdd) + "§7)");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);

        // GUI 갱신
        refresh();
    }
}
