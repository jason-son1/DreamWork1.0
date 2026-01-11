package com.dreamwork.core.listener;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.quest.QuestManager;
import com.dreamwork.core.quest.QuestProgress;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.database.StorageManager;
import com.dreamwork.core.model.UserData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
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
    private final QuestManager questManager;

    public PlayerDataListener(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.storageManager = plugin.getStorageManager();
        this.jobManager = plugin.getJobManager();
        this.statManager = plugin.getStatManager();
        this.questManager = plugin.getQuestManager();
    }

    /**
     * 플레이어 접속 시 데이터 로드
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 비동기로 데이터 로드
        storageManager.loadUserAsync(uuid, player.getName()).thenAccept(data -> {
            // 메인 스레드에서 데이터 적용
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                data.setName(player.getName());

                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] 플레이어 데이터 로드: " + player.getName());
                }

                // 1. JobManager에 직업 데이터 등록
                UserJobData jobData = new UserJobData(uuid);
                if (data.getJobId() != null) {
                    jobData.setJobId(data.getJobId());
                    jobData.setLevel(data.getJobLevel());
                    jobData.setCurrentExp(data.getJobExp());
                    // TotalExp는 현재 저장하지 않고 있음 (UserData에 추가 필요 혹은 계산)
                    jobData.setTotalExp(data.getJobExp());
                }
                jobManager.setUserJobData(uuid, jobData);

                // 2. StatManager에 스탯 데이터 등록
                StatManager.PlayerStats stats = new StatManager.PlayerStats(uuid);
                stats.setStr(data.getStr());
                stats.setDex(data.getDex());
                stats.setCon(data.getCon());
                stats.setInt(data.getIntel());
                stats.setLuck(data.getLuk());
                stats.setStatPoints(data.getStatPoints());
                statManager.setStats(uuid, stats);

                // 스탯 재계산 (직업 보너스 등 적용)
                statManager.recalculateStats(player);

                // 3. QuestManager에 퀘스트 데이터 등록
                questManager.loadQuestProgress(uuid, data.getQuestProgresses());

                // 일일 퀘스트 할당 (없으면)
                questManager.assignDailyQuests(player);
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

        // 캐시된 데이터 가져오기 (없으면 새로 생성하지 않고 무시)
        UserData data = storageManager.getUserData(uuid);
        if (data == null) {
            // 로드가 완료되지 않은 상태에서 나가는 경우 등
            return;
        }

        // 최신 상태 동기화
        // 1. 직업 데이터
        UserJobData jobData = jobManager.getUserJob(uuid);
        if (jobData != null) {
            data.setJobId(jobData.getJobId());
            data.setJobLevel(jobData.getLevel());
            data.setJobExp(jobData.getCurrentExp());
        }

        // 2. 스탯 데이터
        StatManager.PlayerStats stats = statManager.getStats(uuid);
        if (stats != null) {
            data.setStr(stats.getBaseStr());
            data.setDex(stats.getBaseDex());
            data.setCon(stats.getBaseCon());
            data.setIntel(stats.getBaseInt());
            data.setLuk(stats.getBaseLuck());
            data.setStatPoints(stats.getStatPoints());
        }

        // 3. 퀘스트 데이터
        // QuestManager의 맵이 UserData 내의 Map과 동일한 객체라면 자동 동기화됨 (Ref reference)
        // 하지만 확실히 하기위해 명시적 할당
        Map<String, QuestProgress> quests = questManager.getPlayerProgress(uuid);
        if (quests != null) {
            data.setQuestProgresses(quests);
        }

        // 변경 사항 표시 및 저장 요청
        data.markDirty();

        // 메모리 해제는 저장 완료 후 또는 즉시 가능.
        // 비동기 저장을 위해 데이터 객체는 남겨두되, 매니저 캐시는 정리
        storageManager.saveUserAsync(data);

        // 매니저 캐시 정리
        jobManager.unloadUserJob(uuid);
        statManager.unloadStats(uuid);
        questManager.loadQuestProgress(uuid, null); // remove from quest manager
        // StorageManager 캐시는 저장 후 자동제거되지 않음. 명시적으로 제거 필요.
        // 하지만 저장이 비동기이므로, 저장이 끝날 때까지 객체는 유효해야 함.
        // saveUserAsync 내부에서 참조하므로 여기서는 remove 해도 됨.
        storageManager.unloadUser(uuid);

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] 플레이어 데이터 저장 및 언로드: " + player.getName());
        }
    }
}
