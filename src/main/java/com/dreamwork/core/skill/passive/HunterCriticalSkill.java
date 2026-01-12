package com.dreamwork.core.skill.passive;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 사냥꾼 패시브 스킬: 급소 공격 (Vital Strike)
 * <p>
 * 몬스터 공격 시 일정 확률로 치명타가 발생합니다.
 * 레벨에 따라 치명타 확률과 데미지가 증가합니다.
 * </p>
 * <p>
 * 레벨별 효과:
 * - Lv.1+: 기본 치명타 확률 5%
 * - Lv.25+: 치명타 확률 10%, 데미지 +50%
 * - Lv.50+: 치명타 확률 15%, 데미지 +75%
 * - Lv.75+: 치명타 확률 20%, 데미지 +100%
 * - Lv.100: 치명타 확률 25%, 데미지 +150%
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class HunterCriticalSkill implements Listener {

    private final DreamWorkCore plugin;

    public HunterCriticalSkill(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 플레이어가 공격자인지 확인
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        // 생물체만 대상
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        // 다른 플레이어는 제외 (PvP)
        if (target instanceof Player) {
            return;
        }

        // 플레이어 사냥꾼 레벨 확인
        UserData userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null) {
            return;
        }

        int hunterLevel = userData.getJobLevel(JobType.HUNTER);

        // 치명타 계산
        double critChance = getCritChance(hunterLevel);
        double critMultiplier = getCritMultiplier(hunterLevel);

        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (random.nextDouble() < critChance) {
            // 치명타 발동!
            double originalDamage = event.getDamage();
            double critDamage = originalDamage * critMultiplier;

            event.setDamage(critDamage);

            // 치명타 이펙트
            player.sendMessage(String.format("§c[사냥꾼] §4치명타! §f%.1f → §c%.1f",
                    originalDamage, critDamage));

            // 파티클/사운드 효과
            target.getWorld().spawnParticle(org.bukkit.Particle.CRIT,
                    target.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
        }
    }

    /**
     * 레벨에 따른 치명타 확률 반환
     */
    private double getCritChance(int level) {
        if (level >= 100)
            return 0.25;
        if (level >= 75)
            return 0.20;
        if (level >= 50)
            return 0.15;
        if (level >= 25)
            return 0.10;
        return 0.05;
    }

    /**
     * 레벨에 따른 치명타 배율 반환
     */
    private double getCritMultiplier(int level) {
        if (level >= 100)
            return 2.5;
        if (level >= 75)
            return 2.0;
        if (level >= 50)
            return 1.75;
        if (level >= 25)
            return 1.5;
        return 1.25;
    }
}
