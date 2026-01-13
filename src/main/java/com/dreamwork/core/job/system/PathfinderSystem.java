package com.dreamwork.core.job.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

/**
 * 험지 주파 (Pathfinder) 패시브 시스템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 탐험가 Lv.10 해금
 * - 지형에 따라 이동 속도 증가
 * - 레벨에 따라 낙하 피해 감소
 * </p>
 */
public class PathfinderSystem implements Listener {

    private final DreamWorkCore plugin;

    /** 험지 블록 목록 */
    private static final Set<Material> ROUGH_TERRAIN = Set.of(
            Material.SAND, Material.RED_SAND, Material.GRAVEL,
            Material.SOUL_SAND, Material.SOUL_SOIL,
            Material.SNOW, Material.SNOW_BLOCK, Material.POWDER_SNOW,
            Material.MUD, Material.MUDDY_MANGROVE_ROOTS);

    public PathfinderSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startSpeedTask();
    }

    /**
     * 주기적으로 탐험가에게 속도 버프를 적용하는 태스크
     */
    private void startSpeedTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    applySpeedBonus(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // 1초마다
    }

    /**
     * 플레이어에게 속도 버프를 적용합니다.
     */
    private void applySpeedBonus(Player player) {
        int level = getExplorerLevel(player);
        if (level < 10)
            return;

        // 레벨별 속도 보너스 계산
        PathfinderConfig config = getConfig(level);
        int speedAmplifier = (int) (config.speedBonus * 10); // 0.1 = 레벨 1

        // 험지에 있으면 추가 보너스
        Block below = player.getLocation().clone().subtract(0, 1, 0).getBlock();
        if (ROUGH_TERRAIN.contains(below.getType())) {
            speedAmplifier += (int) (config.terrainBonus * 10);
        }

        // 수중 속도 (Lv.90+)
        if (player.isInWater() && config.waterSpeed > 0) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.DOLPHINS_GRACE, 40, 0, true, false, false));
        }

        // 속도 효과 적용
        if (speedAmplifier > 0) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, 40, Math.min(speedAmplifier, 5), true, false, false));
        }
    }

    /**
     * 낙하 피해 감소 처리
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL)
            return;
        if (!(event.getEntity() instanceof Player player))
            return;

        int level = getExplorerLevel(player);
        if (level < 30)
            return; // Lv.30부터 낙하 감소

        PathfinderConfig config = getConfig(level);
        double reduction = config.fallReduction;

        double newDamage = event.getDamage() * (1.0 - reduction);
        event.setDamage(Math.max(0, newDamage));

        // 완전 무효화 시 메시지
        if (newDamage <= 0) {
            player.sendActionBar(
                    net.kyori.adventure.text.Component.text("§a§l⬇ 험지 주파로 낙하 피해 무효화!"));
        }
    }

    /**
     * 레벨에 따른 설정을 반환합니다.
     */
    private PathfinderConfig getConfig(int level) {
        if (level >= 90) {
            return new PathfinderConfig(0.30, 0.25, 0.75, 0.50);
        } else if (level >= 70) {
            return new PathfinderConfig(0.25, 0.20, 0.50, 0);
        } else if (level >= 50) {
            return new PathfinderConfig(0.20, 0.15, 0.35, 0);
        } else if (level >= 30) {
            return new PathfinderConfig(0.15, 0.10, 0.20, 0);
        } else {
            return new PathfinderConfig(0.10, 0.05, 0, 0);
        }
    }

    /**
     * 플레이어의 탐험가 레벨을 반환합니다.
     */
    private int getExplorerLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"explorer".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }

    /**
     * Pathfinder 설정 클래스
     */
    private record PathfinderConfig(double speedBonus, double terrainBonus, double fallReduction, double waterSpeed) {
    }
}
