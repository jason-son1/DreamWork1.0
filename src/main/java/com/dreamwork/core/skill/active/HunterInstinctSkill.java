package com.dreamwork.core.skill.active;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * 사냥꾼 액티브 스킬: 사냥꾼의 본능 (Hunter's Instinct)
 * <p>
 * 활성화 시 주변 적들이 발광하고 공격력이 증가합니다.
 * </p>
 * <p>
 * 레벨별 효과:
 * - Lv.25+: 15초, 15블록 범위, 공격력 +20%
 * - Lv.50+: 20초, 20블록 범위, 공격력 +35%
 * - Lv.75+: 25초, 25블록 범위, 공격력 +50%
 * - Lv.100: 30초, 30블록 범위, 공격력 +75%, 슬로우 부여
 * </p>
 * <p>
 * 쿨다운: 120초
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class HunterInstinctSkill {

    private final DreamWorkCore plugin;
    private final Set<UUID> activeSkill = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Double> damageBonus = new HashMap<>();

    private static final long COOLDOWN_MS = 120_000; // 2분

    public HunterInstinctSkill(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 스킬을 활성화합니다.
     */
    public boolean activate(Player player) {
        UUID uuid = player.getUniqueId();

        // 쿨다운 확인
        if (cooldowns.containsKey(uuid)) {
            long remaining = (cooldowns.get(uuid) + COOLDOWN_MS - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                player.sendMessage("§c[사냥꾼] 사냥꾼의 본능 쿨다운: §e" + remaining + "초");
                return false;
            }
        }

        // 레벨 확인
        UserData userData = plugin.getStorageManager().getUserData(uuid);
        if (userData == null)
            return false;

        int hunterLevel = userData.getJobLevel(JobType.HUNTER);
        if (hunterLevel < 25) {
            player.sendMessage("§c[사냥꾼] 사냥꾼의 본능은 레벨 25 이상에서 사용 가능합니다.");
            return false;
        }

        // 스킬 활성화
        activeSkill.add(uuid);
        cooldowns.put(uuid, System.currentTimeMillis());

        int duration = getDuration(hunterLevel);
        int range = getRange(hunterLevel);
        double bonus = getDamageBonus(hunterLevel);
        boolean applySlow = hunterLevel >= 100;

        damageBonus.put(uuid, bonus);

        // 효과 메시지
        player.sendMessage("§c[사냥꾼] §l사냥꾼의 본능 §r§c활성화! §e" + duration + "초, 공격력 +" + (int) (bonus * 100) + "%");
        player.playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.0f, 1.2f);

        // 주변 몬스터에게 발광 효과
        applyGlowingEffect(player, range, duration, applySlow);

        // 힘 버프
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,
                duration * 20, bonus > 0.5 ? 1 : 0, false, false, true));

        // 자동 비활성화
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeSkill.remove(uuid)) {
                damageBonus.remove(uuid);
                player.sendMessage("§7[사냥꾼] 사냥꾼의 본능이 종료되었습니다.");
            }
        }, duration * 20L);

        return true;
    }

    /**
     * 주변 몬스터에게 발광 효과를 적용합니다.
     */
    private void applyGlowingEffect(Player player, int range, int duration, boolean applySlow) {
        Collection<Entity> nearbyEntities = player.getWorld().getNearbyEntities(
                player.getLocation(), range, range, range);

        for (Entity entity : nearbyEntities) {
            if (entity instanceof Monster monster) {
                // 발광 효과
                monster.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,
                        duration * 20, 0, false, false));

                // Lv.100 슬로우 효과
                if (applySlow) {
                    monster.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                            duration * 20, 0, false, false));
                }
            }
        }

        // 범위 내 몬스터 수 알림
        long monsterCount = nearbyEntities.stream().filter(e -> e instanceof Monster).count();
        if (monsterCount > 0) {
            player.sendMessage("§e" + monsterCount + "§f마리의 적을 감지했습니다.");
        }
    }

    /**
     * 플레이어의 현재 데미지 보너스를 반환합니다.
     */
    public double getPlayerDamageBonus(Player player) {
        return damageBonus.getOrDefault(player.getUniqueId(), 0.0);
    }

    public boolean isActive(Player player) {
        return activeSkill.contains(player.getUniqueId());
    }

    private int getDuration(int level) {
        if (level >= 100)
            return 30;
        if (level >= 75)
            return 25;
        if (level >= 50)
            return 20;
        return 15;
    }

    private int getRange(int level) {
        if (level >= 100)
            return 30;
        if (level >= 75)
            return 25;
        if (level >= 50)
            return 20;
        return 15;
    }

    private double getDamageBonus(int level) {
        if (level >= 100)
            return 0.75;
        if (level >= 75)
            return 0.50;
        if (level >= 50)
            return 0.35;
        return 0.20;
    }
}
