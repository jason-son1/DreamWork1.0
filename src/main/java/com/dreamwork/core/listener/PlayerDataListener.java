package com.dreamwork.core.listener;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.storage.StorageManager;
import com.dreamwork.core.storage.UserData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * 플레이어 데이터 로드/저장 리스너
 * 
 * <p>
 * 플레이어 접속 시 데이터를 비동기로 로드하고,
 * 퇴장 시 비동기로 저장합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class PlayerDataListener implements Listener {

    private final DreamWorkCore plugin;
    private final StorageManager storageManager;
    private final JobManager jobManager;
    private final StatManager statManager;

    /**
     * PlayerDataListener 생성자
     * 
     * @param plugin 플러그인 인스턴스
     */
    public PlayerDataListener(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.storageManager = plugin.getStorageManager();
        this.jobManager = plugin.getJobManager();
        this.statManager = plugin.getStatManager();
    }

    /**
     * 플레이어 접속 시 데이터 로드
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 비동기로 데이터 로드
        storageManager.loadUserJsonAsync(uuid, UserData.class).thenAccept(optData -> {
            // 메인 스레드에서 데이터 적용
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                UserData data;

                if (optData.isPresent()) {
                    // 기존 데이터 로드
                    data = optData.get();
                    data.setPlayerName(player.getName());
                    data.updateLastLogin();

                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] 플레이어 데이터 로드: " + player.getName());
                    }
                } else {
                    // 신규 플레이어 - 새 데이터 생성
                    data = new UserData(uuid, player.getName());

                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Debug] 신규 플레이어 데이터 생성: " + player.getName());
                    }
                }

                // JobManager에 직업 데이터 등록
                if (data.getJobData() != null) {
                    jobManager.setUserJobData(uuid, data.getJobData());
                } else {
                    jobManager.setUserJobData(uuid, new UserJobData(uuid));
                }

                // StatManager에 스탯 데이터 등록
                if (data.getStats() != null) {
                    statManager.setStats(uuid, data.getStats());
                }

                // 스탯 재계산 (직업 보너스 적용)
                statManager.recalculateStats(player);
            });
        }).exceptionally(ex -> {
            plugin.getLogger().severe("플레이어 데이터 로드 실패: " + player.getName());
            ex.printStackTrace();
            return null;
        });
    }

    /**
     * 플레이어 퇴장 시 데이터 저장
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 현재 데이터 수집
        UserData data = new UserData(uuid, player.getName());

        // 직업 데이터
        UserJobData jobData = jobManager.getUserJob(uuid);
        data.setJobData(jobData.copy());

        // 스탯 데이터
        StatManager.PlayerStats stats = statManager.getStats(uuid);
        data.setStats(stats);

        // 비동기로 저장
        storageManager.saveUserJsonAsync(uuid, data).thenRun(() -> {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] 플레이어 데이터 저장: " + player.getName());
            }
        }).exceptionally(ex -> {
            plugin.getLogger().severe("플레이어 데이터 저장 실패: " + player.getName());
            ex.printStackTrace();
            return null;
        });

        // 캐시에서 제거
        jobManager.unloadUserJob(uuid);
        statManager.unloadStats(uuid);
    }
}
