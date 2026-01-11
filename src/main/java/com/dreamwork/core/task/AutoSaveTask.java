package com.dreamwork.core.task;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.database.StorageManager;
import com.dreamwork.core.model.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 자동 저장 태스크
 * 
 * <p>
 * 주기적으로 모든 접속 중인 플레이어의 데이터를 저장합니다.
 * </p>
 */
public class AutoSaveTask extends BukkitRunnable {

    private final DreamWorkCore plugin;
    private final StorageManager storageManager;
    private final JobManager jobManager;
    private final StatManager statManager;

    public AutoSaveTask(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.storageManager = plugin.getStorageManager();
        this.jobManager = plugin.getJobManager();
        this.statManager = plugin.getStatManager();
    }

    @Override
    public void run() {
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[AutoSave] 자동 저장 시작...");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerData(player);
        }
    }

    private void savePlayerData(Player player) {
        try {
            // 캐시된 데이터 가져오기 (데이터 손실 방지)
            UserData data = storageManager.getUserData(player.getUniqueId());
            if (data == null) {
                // 캐시에 없으면 새로 생성 (비상시)
                data = new UserData(player.getUniqueId(), player.getName());
            }

            // 직업 데이터 동기화
            UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
            if (jobData != null) {
                data.setJobData(jobData.copy());
            }

            // 스탯 데이터 동기화
            StatManager.PlayerStats stats = statManager.getStats(player.getUniqueId());
            if (stats != null) {
                data.setStats(stats);
            }

            // 비동기 저장
            data.markDirty();
            storageManager.saveUserAsync(data);

        } catch (Exception e) {
            plugin.getLogger().warning("자동 저장 실패: " + player.getName());
            e.printStackTrace();
        }
    }
}
