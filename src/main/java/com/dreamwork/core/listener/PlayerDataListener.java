package com.dreamwork.core.listener;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.JobType;
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
 * <p>
 * 플레이어 접속 시 데이터를 비동기로 로드하고,
 * 퇴장 시 비동기로 저장합니다.
 * </p>
 * <p>
 * 다중 직업 시스템: 플레이어는 5개 직업(광부/농부/어부/사냥꾼/탐험가)을
 * 동시에 레벨업할 수 있습니다.
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

                    // 직업 레벨 로그
                    for (JobType jobType : JobType.values()) {
                        int level = data.getJobLevel(jobType);
                        double exp = data.getJobExp(jobType);
                        plugin.getLogger().info("[Debug]   " + jobType.getDisplayName() +
                                " Lv." + level + " (Exp: " + String.format("%.0f", exp) + ")");
                    }
                }

                // 1. StatManager에 스탯 데이터 등록
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

                // 2. QuestManager에 퀘스트 데이터 등록
                questManager.loadQuestProgress(uuid, data.getQuestProgresses());

                // 일일 퀘스트 할당 (없으면)
                questManager.assignDailyQuests(player);

                // 3. 환영 메시지 - 총 레벨 표시
                int totalLevel = data.getTotalJobLevel();
                player.sendMessage("§a[DreamWork] §f환영합니다! 총 직업 레벨: §e" + totalLevel);
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

        // 캐시된 데이터 가져오기 (없으면 무시)
        UserData data = storageManager.getUserData(uuid);
        if (data == null) {
            // 로드가 완료되지 않은 상태에서 나가는 경우 등
            return;
        }

        // 최신 상태 동기화
        // 1. 스탯 데이터
        StatManager.PlayerStats stats = statManager.getStats(uuid);
        if (stats != null) {
            data.setStr(stats.getBaseStr());
            data.setDex(stats.getBaseDex());
            data.setCon(stats.getBaseCon());
            data.setIntel(stats.getBaseInt());
            data.setLuk(stats.getBaseLuck());
            data.setStatPoints(stats.getStatPoints());
        }

        // 2. 퀘스트 데이터
        Map<String, QuestProgress> quests = questManager.getPlayerProgress(uuid);
        if (quests != null) {
            data.setQuestProgresses(quests);
        }

        // 3. 직업 데이터는 UserData.jobs에 직접 저장되므로 별도 동기화 불필요
        // (JobManager.addExp가 UserData.getJobInfo를 직접 수정함)

        // 변경 사항 표시 및 저장 요청
        data.markDirty();
        storageManager.saveUserAsync(data);

        // 매니저 캐시 정리
        jobManager.unloadUserJob(uuid);
        statManager.unloadStats(uuid);
        questManager.loadQuestProgress(uuid, null); // 퀘스트 매니저에서 제거
        storageManager.unloadUser(uuid);

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] 플레이어 데이터 저장 및 언로드: " + player.getName());
        }
    }
}
