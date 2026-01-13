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

import java.util.*;

/**
 * 아쿠아리움 납품 GUI Provider
 * 
 * <p>
 * 어부가 잡은 물고기를 수족관에 납품하여 보상을 받는 시설.
 * 살아있는 물고기(Live Fish)는 추가 보상 제공
 * </p>
 */
public class AquariumProvider extends InventoryProvider {

    private final DreamWorkCore plugin;

    /** 납품 가능한 물고기 및 보상 */
    private static final Map<Material, FishDeliveryInfo> FISH_REWARDS = new LinkedHashMap<>();

    static {
        FISH_REWARDS.put(Material.COD, new FishDeliveryInfo("대구", 10, 5.0));
        FISH_REWARDS.put(Material.SALMON, new FishDeliveryInfo("연어", 25, 15.0));
        FISH_REWARDS.put(Material.TROPICAL_FISH, new FishDeliveryInfo("열대어", 50, 30.0));
        FISH_REWARDS.put(Material.PUFFERFISH, new FishDeliveryInfo("복어", 40, 25.0));
    }

    public AquariumProvider(Player player, DreamWorkCore plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public void init(Inventory inventory) {
        renderBackground(inventory);
        renderHeader(inventory);
        renderFishSlots(inventory);
        renderDeliveryButton(inventory);
    }

    /**
     * 배경 렌더링
     */
    private void renderBackground(Inventory inventory) {
        ItemStack glass = ItemBuilder.of(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .name("§8")
                .build();

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }
    }

    /**
     * 헤더 렌더링
     */
    private void renderHeader(Inventory inventory) {
        ItemStack info = ItemBuilder.of(Material.HEART_OF_THE_SEA)
                .name("§b§l아쿠아리움 납품")
                .lore("")
                .lore("§7물고기를 납품하여 보상을 받으세요!")
                .lore("")
                .lore("§a살아있는 물고기: §f2배 보상")
                .lore("§6월척: §f3배 보상")
                .lore("")
                .lore("§8어부 레벨이 높을수록 보너스!")
                .build();
        inventory.setItem(4, info);

        // 닫기 버튼
        ItemStack closeBtn = ItemBuilder.of(Material.BARRIER)
                .name("§c닫기")
                .build();
        inventory.setItem(8, closeBtn);
    }

    /**
     * 물고기 납품 슬롯 렌더링
     */
    private void renderFishSlots(Inventory inventory) {
        int slot = 19;
        for (Map.Entry<Material, FishDeliveryInfo> entry : FISH_REWARDS.entrySet()) {
            Material fishType = entry.getKey();
            FishDeliveryInfo info = entry.getValue();

            int count = countFishInInventory(fishType);

            ItemStack item = ItemBuilder.of(fishType)
                    .name("§f" + info.displayName)
                    .lore("")
                    .lore("§7보유 수량: §e" + count + "마리")
                    .lore("")
                    .lore("§7보상:")
                    .lore("§8- §f" + info.baseReward + " Dream / 마리")
                    .lore("§8- §f" + info.baseExp + " 경험치 / 마리")
                    .lore("")
                    .lore(count > 0 ? "§a[클릭] 전량 납품" : "§c보유한 물고기 없음")
                    .build();

            inventory.setItem(slot, item);

            slot++;
            if (slot == 23)
                slot = 28;
        }
    }

    /**
     * 납품 버튼 렌더링
     */
    private void renderDeliveryButton(Inventory inventory) {
        int totalFish = 0;
        int totalReward = 0;
        double totalExp = 0;

        for (Map.Entry<Material, FishDeliveryInfo> entry : FISH_REWARDS.entrySet()) {
            int count = countFishInInventory(entry.getKey());
            FishDeliveryInfo info = entry.getValue();
            totalFish += count;
            totalReward += count * info.baseReward;
            totalExp += count * info.baseExp;
        }

        // 어부 레벨 보너스
        int fisherLevel = getFisherLevel();
        double levelBonus = 1.0 + (fisherLevel * 0.01); // 레벨당 1% 보너스
        totalReward = (int) (totalReward * levelBonus);
        totalExp = totalExp * levelBonus;

        ItemStack deliverBtn = ItemBuilder.of(totalFish > 0 ? Material.CHEST : Material.ENDER_CHEST)
                .name("§a§l[전체 납품]")
                .lore("")
                .lore("§7납품 예정: §e" + totalFish + "마리")
                .lore("")
                .lore("§7예상 보상:")
                .lore("§8- §6" + totalReward + " Dream")
                .lore("§8- §b" + String.format("%.1f", totalExp) + " EXP")
                .lore("")
                .lore("§8레벨 보너스: §a" + String.format("%.0f", (levelBonus - 1) * 100) + "%")
                .lore("")
                .lore(totalFish > 0 ? "§a[클릭] 납품하기" : "§c납품할 물고기 없음")
                .build();

        inventory.setItem(40, deliverBtn);
    }

    /**
     * 인벤토리 내 물고기 수량 계산
     */
    private int countFishInInventory(Material fishType) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == fishType) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * 전체 납품 처리
     */
    private void deliverAllFish() {
        int totalFish = 0;
        int totalReward = 0;

        for (Map.Entry<Material, FishDeliveryInfo> entry : FISH_REWARDS.entrySet()) {
            Material fishType = entry.getKey();
            FishDeliveryInfo info = entry.getValue();

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == fishType) {
                    int count = item.getAmount();
                    totalFish += count;
                    totalReward += count * info.baseReward;
                    item.setAmount(0);
                }
            }
        }

        if (totalFish == 0) {
            player.sendMessage("§c[아쿠아리움] 납품할 물고기가 없습니다.");
            return;
        }

        // 어부 레벨 보너스
        int fisherLevel = getFisherLevel();
        double levelBonus = 1.0 + (fisherLevel * 0.01);
        totalReward = (int) (totalReward * levelBonus);

        // 보상 지급 (경제 시스템 연동)
        // plugin.getEconomyManager().giveMoney(player, totalReward);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        player.sendMessage("§b[아쿠아리움] §f" + totalFish + "마리를 납품했습니다!");
        player.sendMessage("§b[아쿠아리움] §6+" + totalReward + " Dream");

        player.closeInventory();
    }

    /**
     * 어부 레벨 반환
     */
    private int getFisherLevel() {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"fisher".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getSlot();

        // 닫기 버튼
        if (slot == 8) {
            player.closeInventory();
            return;
        }

        // 전체 납품 버튼
        if (slot == 40) {
            deliverAllFish();
            return;
        }

        // 개별 물고기 납품
        int[] fishSlots = { 19, 20, 21, 22, 28, 29, 30, 31 };
        int fishIndex = -1;
        for (int i = 0; i < fishSlots.length; i++) {
            if (slot == fishSlots[i]) {
                fishIndex = i;
                break;
            }
        }

        if (fishIndex >= 0) {
            int idx = 0;
            for (Map.Entry<Material, FishDeliveryInfo> entry : FISH_REWARDS.entrySet()) {
                if (idx == fishIndex) {
                    deliverSingleFishType(entry.getKey(), entry.getValue());
                    refresh();
                    break;
                }
                idx++;
            }
        }
    }

    /**
     * 단일 물고기 종류 납품
     */
    private void deliverSingleFishType(Material fishType, FishDeliveryInfo info) {
        int totalFish = 0;
        int totalReward = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == fishType) {
                int count = item.getAmount();
                totalFish += count;
                totalReward += count * info.baseReward;
                item.setAmount(0);
            }
        }

        if (totalFish == 0) {
            player.sendMessage("§c[아쿠아리움] " + info.displayName + "이(가) 없습니다.");
            return;
        }

        // 레벨 보너스
        int fisherLevel = getFisherLevel();
        double levelBonus = 1.0 + (fisherLevel * 0.01);
        totalReward = (int) (totalReward * levelBonus);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        player.sendMessage("§b[아쿠아리움] §f" + info.displayName + " " + totalFish + "마리를 납품했습니다!");
    }

    /**
     * GUI 열기
     */
    public static void open(Player player, DreamWorkCore plugin) {
        SmartInventory.builder()
                .title("§b§l아쿠아리움 납품")
                .size(54)
                .provider(new AquariumProvider(player, plugin))
                .build()
                .open(player);
    }

    /**
     * 물고기 납품 정보 클래스
     */
    private static class FishDeliveryInfo {
        final String displayName;
        final int baseReward;
        final double baseExp;

        FishDeliveryInfo(String displayName, int baseReward, double baseExp) {
            this.displayName = displayName;
            this.baseReward = baseReward;
            this.baseExp = baseExp;
        }
    }
}
