package com.dreamwork.core.mission;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 랭크 칭호 시스템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 직업별 승급에 따른 칭호 부여
 * - LuckPerms 연동 (선택적)
 * - 플레이어 이름 앞에 칭호 표시
 * </p>
 */
public class RankTitleSystem implements Listener {

    private final DreamWorkCore plugin;

    /** 플레이어별 현재 칭호 */
    private final Map<UUID, String> playerTitles = new HashMap<>();

    /** 직업별 랭크 칭호 정의 */
    private static final Map<String, Map<Integer, String>> RANK_TITLES = new HashMap<>();

    static {
        // 광부 칭호
        Map<Integer, String> minerTitles = new HashMap<>();
        minerTitles.put(0, "§7[수습 광부]");
        minerTitles.put(10, "§7[견습 광부]");
        minerTitles.put(30, "§f[숙련 광부]");
        minerTitles.put(50, "§b[전문 광부]");
        minerTitles.put(70, "§6[대장장이]");
        minerTitles.put(100, "§d[전설의 광부]");
        RANK_TITLES.put("miner", minerTitles);

        // 농부 칭호
        Map<Integer, String> farmerTitles = new HashMap<>();
        farmerTitles.put(0, "§7[수습 농부]");
        farmerTitles.put(10, "§7[견습 농부]");
        farmerTitles.put(30, "§f[숙련 농부]");
        farmerTitles.put(50, "§a[전문 농부]");
        farmerTitles.put(70, "§6[농업 장인]");
        farmerTitles.put(100, "§d[대지의 현자]");
        RANK_TITLES.put("farmer", farmerTitles);

        // 어부 칭호
        Map<Integer, String> fisherTitles = new HashMap<>();
        fisherTitles.put(0, "§7[수습 어부]");
        fisherTitles.put(10, "§7[견습 어부]");
        fisherTitles.put(30, "§3[숙련 어부]");
        fisherTitles.put(50, "§b[강태공]");
        fisherTitles.put(70, "§6[바다의 지배자]");
        fisherTitles.put(100, "§d[해신의 사도]");
        RANK_TITLES.put("fisher", fisherTitles);

        // 탐험가 칭호
        Map<Integer, String> explorerTitles = new HashMap<>();
        explorerTitles.put(0, "§7[수습 탐험가]");
        explorerTitles.put(10, "§7[견습 탐험가]");
        explorerTitles.put(30, "§e[지평선 개척자]");
        explorerTitles.put(50, "§6[대륙의 기록자]");
        explorerTitles.put(70, "§6[세계의 방랑자]");
        explorerTitles.put(100, "§d[전설의 탐험가]");
        RANK_TITLES.put("explorer", explorerTitles);

        // 사냥꾼 칭호
        Map<Integer, String> hunterTitles = new HashMap<>();
        hunterTitles.put(0, "§7[수습 사냥꾼]");
        hunterTitles.put(10, "§7[견습 사냥꾼]");
        hunterTitles.put(30, "§c[숙련 사냥꾼]");
        hunterTitles.put(50, "§4[엘리트 슬레이어]");
        hunterTitles.put(70, "§6[보스 헌터]");
        hunterTitles.put(100, "§d[전설의 용병]");
        RANK_TITLES.put("hunter", hunterTitles);
    }

    public RankTitleSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 플레이어 접속 시 칭호 갱신
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerTitle(event.getPlayer());
    }

    /**
     * 플레이어 칭호 갱신
     */
    public void updatePlayerTitle(Player player) {
        UUID uuid = player.getUniqueId();
        String title = calculateTitle(uuid);
        playerTitles.put(uuid, title);

        // 플레이어 표시 이름 갱신
        updateDisplayName(player, title);
    }

    /**
     * 칭호 계산
     */
    public String calculateTitle(UUID uuid) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return "";

        UserJobData jobData = jobManager.getUserJob(uuid);
        if (!jobData.hasJob())
            return "";

        String jobId = jobData.getJobId();
        int level = jobData.getLevel();

        Map<Integer, String> titles = RANK_TITLES.get(jobId);
        if (titles == null)
            return "";

        // 현재 레벨에 맞는 가장 높은 칭호 찾기
        String title = "";
        int highestLevel = 0;
        for (Map.Entry<Integer, String> entry : titles.entrySet()) {
            if (level >= entry.getKey() && entry.getKey() >= highestLevel) {
                highestLevel = entry.getKey();
                title = entry.getValue();
            }
        }

        return title;
    }

    /**
     * 플레이어 표시 이름 갱신
     */
    private void updateDisplayName(Player player, String title) {
        if (title.isEmpty()) {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
        } else {
            String displayName = title + " §f" + player.getName();
            player.setDisplayName(displayName);
            player.setPlayerListName(displayName);
        }
    }

    /**
     * 승급 처리
     */
    public void promotePlayer(Player player) {
        UUID uuid = player.getUniqueId();

        // 칭호 갱신
        updatePlayerTitle(player);

        // LuckPerms 연동 (선택적)
        tryUpdateLuckPerms(player);

        // 서버 알림
        String title = playerTitles.getOrDefault(uuid, "");
        if (!title.isEmpty()) {
            Bukkit.broadcastMessage("§6[승급] §f" + player.getName() + "님이 " + title + " §f등급이 되었습니다!");
        }
    }

    /**
     * LuckPerms 연동 (선택적)
     */
    private void tryUpdateLuckPerms(Player player) {
        // LuckPerms API가 있을 경우 prefix 업데이트
        try {
            if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
                String title = playerTitles.getOrDefault(player.getUniqueId(), "");
                // LuckPerms API를 통한 prefix 설정
                // net.luckperms.api.LuckPermsProvider.get() ...
                plugin.getLogger().info("[RankTitle] LuckPerms 연동 시도: " + player.getName() + " -> " + title);
            }
        } catch (Exception e) {
            // LuckPerms 없으면 무시
        }
    }

    /**
     * 플레이어의 현재 칭호 조회
     */
    public String getTitle(UUID uuid) {
        return playerTitles.getOrDefault(uuid, "");
    }

    /**
     * 특정 직업/레벨의 칭호 조회
     */
    public String getTitleForJobLevel(String jobId, int level) {
        Map<Integer, String> titles = RANK_TITLES.get(jobId);
        if (titles == null)
            return "";

        String title = "";
        int highestLevel = 0;
        for (Map.Entry<Integer, String> entry : titles.entrySet()) {
            if (level >= entry.getKey() && entry.getKey() >= highestLevel) {
                highestLevel = entry.getKey();
                title = entry.getValue();
            }
        }
        return title;
    }
}
