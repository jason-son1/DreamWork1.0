package com.dreamwork.core.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사냥꾼 처치 지식 시스템 (Slayer's Knowledge)
 * <p>
 * 특정 몬스터를 많이 처치할수록 해당 몬스터에 대한 추가 데미지와
 * 드롭률 보너스를 획득합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class SlayerKnowledgeSystem implements Listener {

    private final DreamWorkCore plugin;

    /** 플레이어별 처치 카운트 (UUID -> (몬스터타입 -> 카운트)) */
    private final Map<UUID, Map<String, Integer>> killCounts = new ConcurrentHashMap<>();

    /** 티어별 필요 처치 수 */
    private static final int[] TIER_THRESHOLDS = { 0, 50, 150, 400, 1000 };

    /** 티어별 데미지 보너스 (%) */
    private static final double[] TIER_DAMAGE_BONUS = { 0, 5, 10, 20, 35 };

    /** 티어별 드롭 보너스 (%) */
    private static final double[] TIER_DROP_BONUS = { 0, 10, 20, 35, 50 };

    public SlayerKnowledgeSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (!(entity instanceof Monster))
            return;

        Player killer = entity.getKiller();
        if (killer == null)
            return;

        // 사냥꾼 레벨 확인
        UserData userData = plugin.getStorageManager().getUserData(killer.getUniqueId());
        if (userData == null)
            return;

        int hunterLevel = userData.getJobLevel(JobType.HUNTER);
        if (hunterLevel < 10)
            return; // Lv.10부터 활성화

        String mobType = entity.getType().name();

        // 처치 카운트 증가
        Map<String, Integer> playerKills = killCounts.computeIfAbsent(
                killer.getUniqueId(), k -> new ConcurrentHashMap<>());
        int newCount = playerKills.merge(mobType, 1, Integer::sum);

        // 티어 업 체크
        int oldTier = getTier(newCount - 1);
        int newTier = getTier(newCount);

        if (newTier > oldTier) {
            String tierName = getTierName(newTier);
            killer.sendMessage("§c[사냥꾼] §e" + getDisplayName(mobType) + " §f처치 지식이 §6" + tierName + "§f 등급으로 상승했습니다!");
            killer.sendMessage("§7  → 데미지 +" + (int) TIER_DAMAGE_BONUS[newTier] + "%, 드롭률 +"
                    + (int) TIER_DROP_BONUS[newTier] + "%");
        }

        // 드롭 보너스 적용
        int tier = getTier(newCount);
        if (tier > 0) {
            double dropBonus = TIER_DROP_BONUS[tier] / 100.0;

            // 드롭 아이템 증가 (확률적)
            if (Math.random() < dropBonus) {
                event.getDrops().forEach(item -> {
                    if (Math.random() < 0.5) { // 50% 확률로 개별 아이템 +1
                        item.setAmount(item.getAmount() + 1);
                    }
                });
            }

            // 추가 경험치
            double bonusExp = tier * 2.0;
            event.setDroppedExp((int) (event.getDroppedExp() * (1 + dropBonus)));
        }
    }

    /**
     * 몬스터에 대한 데미지 보너스를 반환합니다.
     */
    public double getDamageBonus(Player player, String mobType) {
        Map<String, Integer> playerKills = killCounts.get(player.getUniqueId());
        if (playerKills == null)
            return 0;

        int kills = playerKills.getOrDefault(mobType, 0);
        int tier = getTier(kills);
        return TIER_DAMAGE_BONUS[tier];
    }

    /**
     * 처치 수에 따른 티어를 반환합니다.
     */
    private int getTier(int kills) {
        for (int i = TIER_THRESHOLDS.length - 1; i >= 0; i--) {
            if (kills >= TIER_THRESHOLDS[i]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 티어 이름을 반환합니다.
     */
    private String getTierName(int tier) {
        return switch (tier) {
            case 1 -> "§a초급";
            case 2 -> "§e중급";
            case 3 -> "§6고급";
            case 4 -> "§c마스터";
            default -> "§7없음";
        };
    }

    /**
     * 몬스터 표시 이름을 반환합니다.
     */
    private String getDisplayName(String mobType) {
        return switch (mobType) {
            case "ZOMBIE" -> "좀비";
            case "SKELETON" -> "스켈레톤";
            case "CREEPER" -> "크리퍼";
            case "SPIDER" -> "거미";
            case "ENDERMAN" -> "엔더맨";
            case "WITCH" -> "마녀";
            case "PILLAGER" -> "약탈자";
            case "WARDEN" -> "워든";
            case "WITHER" -> "위더";
            case "ENDER_DRAGON" -> "엔더 드래곤";
            default -> mobType;
        };
    }

    /**
     * 플레이어의 처치 카운트를 반환합니다.
     */
    public int getKillCount(Player player, String mobType) {
        Map<String, Integer> playerKills = killCounts.get(player.getUniqueId());
        if (playerKills == null)
            return 0;
        return playerKills.getOrDefault(mobType, 0);
    }
}
