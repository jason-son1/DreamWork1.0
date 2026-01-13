package com.dreamwork.core.job.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 육감 (Sixth Sense) 패시브 시스템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 탐험가 Lv.30 해금
 * - 근처 구조물/던전 감지
 * - 레벨에 따라 감지 범위 및 정확도 증가
 * </p>
 */
public class SixthSenseSystem {

    private final DreamWorkCore plugin;

    /** 체크 간격 (틱) - 20초마다 */
    private static final long CHECK_INTERVAL = 400L;

    public SixthSenseSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        startSenseTask();
    }

    /**
     * 주기적으로 주변 구조물을 감지하는 태스크
     */
    private void startSenseTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    processSense(player);
                }
            }
        }.runTaskTimer(plugin, CHECK_INTERVAL, CHECK_INTERVAL);
    }

    /**
     * 플레이어 주변 구조물을 감지합니다.
     */
    private void processSense(Player player) {
        int level = getExplorerLevel(player);
        if (level < 30)
            return;

        SenseConfig config = getConfig(level);
        Location playerLoc = player.getLocation();

        // 구조물 감지 시뮬레이션 (실제 구현에서는 구조물 API 사용)
        // 여기서는 간단한 예시로 플레이어 행동에 따라 힌트 제공

        // 실제 구조물 위치 탐색은 비용이 많이 들어 캐싱 필요
        // 이 예시에서는 AtlasDiscoverySystem의 발견 기록 기반으로 힌트 제공

        // 보물 상자 감지 (Lv.90+)
        if (config.treasureSense) {
            // 주변에 상자가 있으면 알림 (간단한 예시)
            boolean hasNearbyChest = checkNearbyTreasure(playerLoc, config.radius);
            if (hasNearbyChest) {
                player.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 2.0f);
                player.sendActionBar(
                        net.kyori.adventure.text.Component.text("§6§l✦ 근처에 보물이 있습니다!"));
            }
        }
    }

    /**
     * 주변에 보물(상자)가 있는지 확인합니다.
     * 실제 구현에서는 청크 내 타일 엔티티 검색 필요
     */
    private boolean checkNearbyTreasure(Location center, int radius) {
        // 간단한 확률 기반 시뮬레이션 (실제로는 상자 검색 필요)
        // 여기서는 데모 목적으로 5% 확률로 알림
        return Math.random() < 0.05;
    }

    /**
     * 플레이어에게 구조물 정보를 표시합니다.
     */
    public void displayStructureInfo(Player player, String structureType, Location structureLoc, int infoLevel) {
        Location playerLoc = player.getLocation();
        double distance = playerLoc.distance(structureLoc);

        // 방향 계산
        double dx = structureLoc.getX() - playerLoc.getX();
        double dz = structureLoc.getZ() - playerLoc.getZ();
        String direction = getDirection(dx, dz);

        StringBuilder msg = new StringBuilder();
        msg.append("§e[육감] §f");

        // infoLevel에 따른 정보량
        if (infoLevel >= 4) {
            msg.append(structureType).append(" - ");
            msg.append(direction).append(" ");
            msg.append((int) distance).append("블록");
        } else if (infoLevel >= 3) {
            msg.append(structureType).append(" - ");
            msg.append((int) distance).append("블록");
        } else if (infoLevel >= 2) {
            msg.append("구조물이 ").append(direction).append("에 있습니다 (");
            msg.append((int) distance).append("블록)");
        } else {
            msg.append("뭔가가 ").append(direction).append(" 방향에 있습니다...");
        }

        player.sendMessage(msg.toString());
        player.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.5f);
    }

    /**
     * 방향을 계산합니다.
     */
    private String getDirection(double dx, double dz) {
        double angle = Math.atan2(dz, dx) * 180 / Math.PI;
        angle = (angle + 360) % 360;

        if (angle >= 337.5 || angle < 22.5)
            return "동쪽";
        if (angle >= 22.5 && angle < 67.5)
            return "남동쪽";
        if (angle >= 67.5 && angle < 112.5)
            return "남쪽";
        if (angle >= 112.5 && angle < 157.5)
            return "남서쪽";
        if (angle >= 157.5 && angle < 202.5)
            return "서쪽";
        if (angle >= 202.5 && angle < 247.5)
            return "북서쪽";
        if (angle >= 247.5 && angle < 292.5)
            return "북쪽";
        return "북동쪽";
    }

    /**
     * 레벨에 따른 감지 설정을 반환합니다.
     */
    private SenseConfig getConfig(int level) {
        if (level >= 90) {
            return new SenseConfig(200, 4, true);
        } else if (level >= 70) {
            return new SenseConfig(150, 3, false);
        } else if (level >= 50) {
            return new SenseConfig(100, 2, false);
        } else {
            return new SenseConfig(50, 1, false);
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
     * 감지 설정 클래스
     */
    private record SenseConfig(int radius, int infoLevel, boolean treasureSense) {
    }
}
