package com.dreamwork.core.gui.provider;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.InventoryProvider;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.job.JobInfo;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 직업 현황 GUI 제공자 (/내정보)
 * <p>
 * 플레이어의 5개 직업 레벨, 경험치, 스탯을 표시합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class JobStatusProvider extends InventoryProvider {

    private final DreamWorkCore plugin;

    public JobStatusProvider(Player player, DreamWorkCore plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public void init(Inventory inventory) {
        UserData userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null) {
            return;
        }

        JobManager jobManager = plugin.getJobManager();

        // 직업 아이콘 위치 (상단 중앙 정렬)
        int[] slots = { 10, 12, 14, 16, 22 };
        Material[] icons = {
                Material.IRON_PICKAXE, // 광부
                Material.WHEAT, // 농부
                Material.FISHING_ROD, // 어부
                Material.BOW, // 사냥꾼
                Material.FILLED_MAP // 탐험가
        };

        JobType[] jobTypes = JobType.values();

        for (int i = 0; i < jobTypes.length; i++) {
            JobType jobType = jobTypes[i];
            JobInfo jobInfo = userData.getJobInfo(jobType);

            int level = jobInfo.getLevel();
            double currentExp = jobInfo.getCurrentExp();
            double requiredExp = jobManager.getRequiredExp(level + 1);
            double progress = (requiredExp > 0) ? (currentExp / requiredExp) : 1.0;
            int progressPercent = (int) (progress * 100);

            // 경험치 바 생성
            String expBar = createExpBar(progress);

            ItemStack item = ItemBuilder.of(icons[i])
                    .name("§6§l" + jobType.getIcon() + " " + jobType.getDisplayName())
                    .lore("")
                    .lore("§7레벨: §e" + level)
                    .lore("§7경험치: §f" + String.format("%.0f", currentExp) + " §7/ §f"
                            + String.format("%.0f", requiredExp))
                    .lore("§7진행률: " + expBar + " §e" + progressPercent + "%")
                    .lore("")
                    .lore("§7총 경험치: §f" + String.format("%.0f", jobInfo.getTotalExp()))
                    .build();

            inventory.setItem(slots[i], item);
        }

        // 총 레벨 표시 (하단 중앙)
        int totalLevel = userData.getTotalJobLevel();
        String rank = getRank(totalLevel);

        ItemBuilder totalBuilder = ItemBuilder.of(Material.NETHER_STAR)
                .name("§e§l종합 정보")
                .lore("")
                .lore("§7총 직업 레벨: §e" + totalLevel)
                .lore("§7현재 등급: §6" + rank)
                .lore("");

        for (JobType jobType : JobType.values()) {
            int lvl = userData.getJobLevel(jobType);
            totalBuilder.lore("§7" + jobType.getIcon() + " " + jobType.getDisplayName() + ": §f" + lvl);
        }

        inventory.setItem(40, totalBuilder.build());

        // 테두리 채우기
        ItemStack border = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        for (int i = 0; i < 45; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, border);
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    /**
     * 경험치 바를 생성합니다. (20칸)
     */
    private String createExpBar(double progress) {
        int filled = (int) (progress * 20);
        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < 20; i++) {
            if (i == filled)
                bar.append("§7");
            bar.append("|");
        }
        return bar.toString();
    }

    /**
     * 총 레벨에 따른 등급을 반환합니다.
     */
    private String getRank(int totalLevel) {
        if (totalLevel >= 500)
            return "§c§l전설";
        if (totalLevel >= 400)
            return "§5§l영웅";
        if (totalLevel >= 300)
            return "§6§l숙련자";
        if (totalLevel >= 200)
            return "§e숙련공";
        if (totalLevel >= 100)
            return "§a견습생";
        return "§7초보자";
    }
}
