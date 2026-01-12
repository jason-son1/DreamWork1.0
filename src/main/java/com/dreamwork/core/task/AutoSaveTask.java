package com.dreamwork.core.task;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.database.StorageManager;
import com.dreamwork.core.model.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 자동 저장 태스크
 * <p>
 * 주기적으로 모든 접속 중인 플레이어의 데이터를 저장합니다.
 * Dirty 플래그가 설정된 데이터만 저장하여 성능을 최적화합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class AutoSaveTask extends BukkitRunnable {

    private final DreamWorkCore plugin;
    private final StorageManager storageManager;
    private final StatManager statManager;

    public AutoSaveTask(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.storageManager = plugin.getStorageManager();
        this.statManager = plugin.getStatManager();
    }

    @Override
    public void run() {
        int savedCount = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (savePlayerData(player)) {
                savedCount++;
            }
        }

        if (plugin.isDebugMode() && savedCount > 0) {
            plugin.getLogger().info("[AutoSave] 자동 저장 완료: " + savedCount + "명");
        }
    }

    /**
     * 플레이어 데이터를 저장합니다.
     * 
     * @param player 플레이어
     * @return 저장 성공 여부
     */
    private boolean savePlayerData(Player player) {
        try {
            // 캐시된 데이터 가져오기
            UserData data = storageManager.getUserData(player.getUniqueId());
            if (data == null) {
                return false;
            }

            // 스탯 데이터 동기화
            StatManager.PlayerStats stats = statManager.getStats(player.getUniqueId());
            if (stats != null) {
                data.setStats(stats);
            }

            // 직업 데이터는 UserData.jobs에 직접 저장되므로 별도 동기화 불필요
            // JobManager.addExp(Player, JobType, double)가 UserData.getJobInfo()를 직접 수정함

            // Dirty 플래그가 설정된 경우에만 저장
            if (data.isDirty()) {
                storageManager.saveUserAsync(data);
                return true;
            }

            return false;

        } catch (Exception e) {
            plugin.getLogger().warning("자동 저장 실패: " + player.getName());
            e.printStackTrace();
            return false;
        }
    }
}
