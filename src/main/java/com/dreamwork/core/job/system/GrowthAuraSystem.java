package com.dreamwork.core.job.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 대지의 기운 (Growth Aura) 패시브 시스템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 농부 Lv.30 해금
 * - 주변 반경 N블록 내 작물이 천천히 성장
 * - 레벨에 따라 반경 및 성장 확률 증가
 * </p>
 */
public class GrowthAuraSystem {

    private final DreamWorkCore plugin;

    /** 체크 간격 (틱) - 10초마다 */
    private static final long CHECK_INTERVAL_TICKS = 200L;

    public GrowthAuraSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        startAuraTask();
    }

    /**
     * 주기적으로 모든 온라인 농부의 오라를 처리하는 태스크
     */
    private void startAuraTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    processPlayerAura(player);
                }
            }
        }.runTaskTimer(plugin, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    /**
     * 플레이어의 오라 효과를 처리합니다.
     */
    private void processPlayerAura(Player player) {
        // 농부인지 확인
        int farmerLevel = getFarmerLevel(player);
        if (farmerLevel < 30)
            return;

        // 레벨별 설정
        AuraConfig config = getAuraConfig(farmerLevel);

        Location center = player.getLocation();
        int radius = config.radius;
        int cropsGrown = 0;
        int maxCropsPerTick = 5; // 한 번에 최대 성장시킬 작물 수

        // 범위 내 작물 검색 및 성장
        for (int x = -radius; x <= radius && cropsGrown < maxCropsPerTick; x++) {
            for (int y = -3; y <= 3 && cropsGrown < maxCropsPerTick; y++) {
                for (int z = -radius; z <= radius && cropsGrown < maxCropsPerTick; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();

                    if (!isCrop(block.getType()))
                        continue;
                    if (!(block.getBlockData() instanceof Ageable ageable))
                        continue;

                    // 이미 다 자란 경우 스킵
                    if (ageable.getAge() >= ageable.getMaximumAge())
                        continue;

                    // 확률 체크
                    if (ThreadLocalRandom.current().nextDouble() > config.growthChance)
                        continue;

                    // 성장
                    ageable.setAge(ageable.getAge() + 1);
                    block.setBlockData(ageable);
                    cropsGrown++;

                    // 파티클 효과 (가끔)
                    if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                        block.getWorld().spawnParticle(
                                Particle.HAPPY_VILLAGER,
                                block.getLocation().add(0.5, 0.5, 0.5),
                                3, 0.3, 0.3, 0.3, 0);
                    }
                }
            }
        }

        // 액션바 표시 (작물이 성장했을 때만)
        if (cropsGrown > 0) {
            player.sendActionBar(
                    net.kyori.adventure.text.Component.text(
                            "§a§l⚘ §f대지의 기운이 작물을 성장시켰습니다! §7(+" + cropsGrown + ")"));
        }
    }

    /**
     * 블록이 작물인지 확인합니다.
     */
    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS,
                    NETHER_WART, SWEET_BERRY_BUSH, COCOA,
                    TORCHFLOWER_CROP, PITCHER_CROP ->
                true;
            default -> false;
        };
    }

    /**
     * 플레이어의 농부 레벨을 반환합니다.
     */
    private int getFarmerLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"farmer".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }

    /**
     * 레벨에 따른 오라 설정을 반환합니다.
     */
    private AuraConfig getAuraConfig(int level) {
        if (level >= 90) {
            return new AuraConfig(10, 0.25, 0.05);
        } else if (level >= 70) {
            return new AuraConfig(7, 0.20, 0);
        } else if (level >= 50) {
            return new AuraConfig(5, 0.15, 0);
        } else {
            return new AuraConfig(3, 0.10, 0);
        }
    }

    /**
     * 오라 설정 클래스
     */
    private record AuraConfig(int radius, double growthChance, double qualityBoost) {
    }
}
