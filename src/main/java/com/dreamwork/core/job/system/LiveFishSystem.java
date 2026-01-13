package com.dreamwork.core.job.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 살아있는 물고기 (Live Fish) 시스템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 살림망 아이템 소지 시 낚은 물고기가 살아있는 상태로 저장
 * - 살아있는 물고기는 아쿠아리움에 2~3배 가격으로 납품 가능
 * - 크기(Size) 정보도 저장
 * </p>
 */
public class LiveFishSystem implements Listener {

    private final DreamWorkCore plugin;

    // PDC 키
    private final NamespacedKey liveKey;
    private final NamespacedKey sizeKey;
    private final NamespacedKey speciesKey;
    private final NamespacedKey fishKeepKey;

    public LiveFishSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.liveKey = new NamespacedKey(plugin, "fish_live");
        this.sizeKey = new NamespacedKey(plugin, "fish_size");
        this.speciesKey = new NamespacedKey(plugin, "fish_species");
        this.fishKeepKey = new NamespacedKey(plugin, "fish_keep");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 낚시 성공 이벤트 처리
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH)
            return;
        if (!(event.getCaught() instanceof Item caughtItem))
            return;

        ItemStack fish = caughtItem.getItemStack();
        if (!isFish(fish.getType()))
            return;

        Player player = event.getPlayer();

        // 살림망 소지 여부 확인
        boolean hasFishKeep = hasFishKeep(player);

        // 물고기 크기 결정
        double size = generateFishSize(player, fish.getType());
        boolean isBigCatch = size >= 80.0; // 월척 기준: 80cm 이상

        // 물고기 메타 데이터 설정
        ItemMeta meta = fish.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 크기 저장
        pdc.set(sizeKey, PersistentDataType.DOUBLE, size);
        pdc.set(speciesKey, PersistentDataType.STRING, fish.getType().name());

        // 살림망 있으면 살아있는 상태
        if (hasFishKeep) {
            pdc.set(liveKey, PersistentDataType.BYTE, (byte) 1);

            meta.setDisplayName(ChatColor.AQUA + "§l[살아있음] " + ChatColor.WHITE + getFishName(fish.getType()));
            meta.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "크기: " + ChatColor.YELLOW + String.format("%.1f", size) + "cm" +
                            (isBigCatch ? ChatColor.GOLD + " [월척!]" : ""),
                    ChatColor.GREEN + "상태: §a살아있음",
                    "",
                    ChatColor.GRAY + "아쿠아리움에 §e2~3배 가격§7으로 납품 가능",
                    "",
                    ChatColor.DARK_GRAY + "[살림망으로 보관됨]"));
        } else {
            meta.setDisplayName(getFishName(fish.getType()));
            meta.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "크기: " + ChatColor.YELLOW + String.format("%.1f", size) + "cm" +
                            (isBigCatch ? ChatColor.GOLD + " [월척!]" : ""),
                    ChatColor.RED + "상태: §7사망",
                    "",
                    ChatColor.DARK_GRAY + "[살림망 없이 낚음]"));
        }

        fish.setItemMeta(meta);
        caughtItem.setItemStack(fish);

        // 월척 알림
        if (isBigCatch) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            player.sendMessage(ChatColor.GOLD + "[월척!] " + ChatColor.WHITE +
                    String.format("%.1f", size) + "cm " + getFishName(fish.getType()) + "을(를) 낚았습니다!");
        }
    }

    /**
     * 물고기 크기 생성 (어부 레벨 보너스 적용)
     */
    private double generateFishSize(Player player, Material fishType) {
        double baseSize = getBaseFishSize(fishType);
        double variance = baseSize * 0.5; // ±50% 변동

        // 기본 랜덤 크기
        double size = baseSize + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * variance;

        // 어부 레벨 보너스 (레벨당 0.5% 크기 증가)
        int fisherLevel = getFisherLevel(player);
        size *= (1.0 + fisherLevel * 0.005);

        return Math.max(10.0, size); // 최소 10cm
    }

    /**
     * 기본 물고기 크기
     */
    private double getBaseFishSize(Material fishType) {
        return switch (fishType) {
            case COD -> 50.0;
            case SALMON -> 60.0;
            case TROPICAL_FISH -> 15.0;
            case PUFFERFISH -> 25.0;
            default -> 40.0;
        };
    }

    /**
     * 살림망(Fish Keep) 생성
     */
    public ItemStack createFishKeep() {
        ItemStack item = new ItemStack(Material.BUCKET);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "살림망");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "낚은 물고기를 살려서 보관합니다.",
                "",
                ChatColor.YELLOW + "효과: " + ChatColor.WHITE + "낚시 시 물고기를 살아있는 상태로 획득",
                "",
                ChatColor.DARK_GRAY + "[인벤토리 소지 시 자동 발동]"));

        // PDC 식별자
        meta.getPersistentDataContainer().set(fishKeepKey, PersistentDataType.BYTE, (byte) 1);
        meta.setCustomModelData(20010);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 살림망 소지 여부 확인
     */
    private boolean hasFishKeep(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                if (item.getItemMeta().getPersistentDataContainer()
                        .has(fishKeepKey, PersistentDataType.BYTE)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 물고기 타입 확인
     */
    private boolean isFish(Material type) {
        return type == Material.COD || type == Material.SALMON ||
                type == Material.TROPICAL_FISH || type == Material.PUFFERFISH;
    }

    /**
     * 물고기 이름 반환
     */
    private String getFishName(Material type) {
        return switch (type) {
            case COD -> "대구";
            case SALMON -> "연어";
            case TROPICAL_FISH -> "열대어";
            case PUFFERFISH -> "복어";
            default -> type.name();
        };
    }

    /**
     * 어부 레벨 조회
     */
    private int getFisherLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"fisher".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }

    // ====================== 유틸리티 ======================

    /**
     * 물고기가 살아있는지 확인
     */
    public boolean isLiveFish(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(liveKey, PersistentDataType.BYTE);
    }

    /**
     * 물고기 크기 조회
     */
    public double getFishSize(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(sizeKey, PersistentDataType.DOUBLE, 0.0);
    }

    /**
     * 월척 여부 확인
     */
    public boolean isBigCatch(ItemStack item) {
        return getFishSize(item) >= 80.0;
    }
}
