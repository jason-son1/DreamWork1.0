package com.dreamwork.core.job.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 약점 간파 (Vital Strike) 패시브 시스템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 사냥꾼 Lv.10 해금
 * - 공격 시 치명타 확률 및 데미지 증가
 * - Lv.90+ 시 치명타 때 이동속도 증가
 * </p>
 */
public class VitalStrikeSystem implements Listener {

    private final DreamWorkCore plugin;

    public VitalStrikeSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 공격 이벤트 처리 - 치명타 확률 적용
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player))
            return;
        if (!(event.getEntity() instanceof LivingEntity))
            return;

        int hunterLevel = getHunterLevel(player);
        if (hunterLevel < 10)
            return;

        VitalStrikeConfig config = getConfig(hunterLevel);

        // 치명타 확률 체크
        if (ThreadLocalRandom.current().nextDouble() > config.critChance)
            return;

        // 치명타 데미지 적용
        double baseDamage = event.getDamage();
        double critDamage = baseDamage * config.critDamage;
        event.setDamage(critDamage);

        // 효과 표시
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
        event.getEntity().getWorld().spawnParticle(
                Particle.CRIT,
                event.getEntity().getLocation().add(0, 1, 0),
                15, 0.3, 0.5, 0.3, 0.1);

        // 액션바 표시
        double bonusDamage = critDamage - baseDamage;
        player.sendActionBar(
                net.kyori.adventure.text.Component.text(
                        "§c§l★ 치명타! §f+" + String.format("%.1f", bonusDamage) + " 피해"));

        // Lv.90+ : 이동속도 증가
        if (config.speedBoostOnCrit) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, 20, 1, true, false, false));
        }
    }

    /**
     * 레벨에 따른 치명타 설정을 반환합니다.
     */
    private VitalStrikeConfig getConfig(int level) {
        if (level >= 90) {
            return new VitalStrikeConfig(0.30, 2.5, true);
        } else if (level >= 70) {
            return new VitalStrikeConfig(0.25, 2.0, false);
        } else if (level >= 50) {
            return new VitalStrikeConfig(0.20, 1.9, false);
        } else if (level >= 30) {
            return new VitalStrikeConfig(0.15, 1.7, false);
        } else {
            return new VitalStrikeConfig(0.10, 1.5, false);
        }
    }

    /**
     * 플레이어의 사냥꾼 레벨을 반환합니다.
     */
    private int getHunterLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"hunter".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }

    /**
     * 치명타 설정 클래스
     */
    private record VitalStrikeConfig(double critChance, double critDamage, boolean speedBoostOnCrit) {
    }
}
