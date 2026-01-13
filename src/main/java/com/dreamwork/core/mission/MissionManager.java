package com.dreamwork.core.mission;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 미션 시스템 매니저
 * 
 * <p>
 * Plan 2.0 기준:
 * - 직업별 승급 미션 관리
 * - 일일/주간 미션 시스템
 * - 미션 진행도 추적 및 보상 지급
 * </p>
 */
public class MissionManager extends Manager {

    private final DreamWorkCore plugin;

    /** 플레이어별 미션 진행도 (UUID -> 미션ID -> 진행도) */
    private final Map<UUID, Map<String, Integer>> missionProgress = new ConcurrentHashMap<>();

    /** 완료된 미션 (UUID -> 미션ID Set) */
    private final Map<UUID, Set<String>> completedMissions = new ConcurrentHashMap<>();

    /** 미션 정의 목록 */
    private final Map<String, MissionDefinition> missions = new LinkedHashMap<>();

    public MissionManager(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        loadMissionDefinitions();
        enabled = true;
        plugin.getLogger().info("MissionManager 활성화 완료 (" + missions.size() + "개 미션 로드)");
    }

    @Override
    public void onDisable() {
        missionProgress.clear();
        completedMissions.clear();
        enabled = false;
    }

    @Override
    public void reload() {
        missions.clear();
        loadMissionDefinitions();
    }

    /**
     * 미션 정의 로드
     */
    private void loadMissionDefinitions() {
        // 광부 승급 미션
        missions.put("miner_rank_10", new MissionDefinition(
                "miner_rank_10", "miner", "석탄 100개 채굴",
                MissionType.MINE_BLOCK, "COAL_ORE", 100, 10));
        missions.put("miner_rank_30", new MissionDefinition(
                "miner_rank_30", "miner", "철광석 500개 채굴",
                MissionType.MINE_BLOCK, "IRON_ORE", 500, 30));
        missions.put("miner_rank_50", new MissionDefinition(
                "miner_rank_50", "miner", "다이아몬드 원석 100개 채굴",
                MissionType.MINE_BLOCK, "DIAMOND_ORE", 100, 50));

        // 농부 승급 미션
        missions.put("farmer_rank_10", new MissionDefinition(
                "farmer_rank_10", "farmer", "밀 200개 수확",
                MissionType.HARVEST_CROP, "WHEAT", 200, 10));
        missions.put("farmer_rank_30", new MissionDefinition(
                "farmer_rank_30", "farmer", "당근 1000개 수확",
                MissionType.HARVEST_CROP, "CARROT", 1000, 30));

        // 어부 승급 미션
        missions.put("fisher_rank_10", new MissionDefinition(
                "fisher_rank_10", "fisher", "물고기 50마리 낚기",
                MissionType.CATCH_FISH, "ANY", 50, 10));
        missions.put("fisher_rank_30", new MissionDefinition(
                "fisher_rank_30", "fisher", "연어 100마리 낚기",
                MissionType.CATCH_FISH, "SALMON", 100, 30));

        // 탐험가 승급 미션
        missions.put("explorer_rank_10", new MissionDefinition(
                "explorer_rank_10", "explorer", "10개 바이옴 발견",
                MissionType.DISCOVER_BIOME, "ANY", 10, 10));
        missions.put("explorer_rank_30", new MissionDefinition(
                "explorer_rank_30", "explorer", "10개 구조물 발견",
                MissionType.DISCOVER_STRUCTURE, "ANY", 10, 30));

        // 사냥꾼 승급 미션
        missions.put("hunter_rank_10", new MissionDefinition(
                "hunter_rank_10", "hunter", "좀비 100마리 처치",
                MissionType.KILL_MOB, "ZOMBIE", 100, 10));
        missions.put("hunter_rank_30", new MissionDefinition(
                "hunter_rank_30", "hunter", "몬스터 1000마리 처치",
                MissionType.KILL_MOB, "ANY", 1000, 30));
    }

    /**
     * 미션 진행도 업데이트
     */
    public void updateProgress(UUID uuid, MissionType type, String target, int amount) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null)
            return;

        // 해당 타입의 미션들 확인
        for (MissionDefinition mission : missions.values()) {
            if (mission.type != type)
                continue;

            // 대상 확인
            if (!mission.target.equals("ANY") && !mission.target.equals(target))
                continue;

            // 이미 완료된 미션 건너뛰기
            if (isMissionCompleted(uuid, mission.id))
                continue;

            // 직업 확인
            String playerJob = getPlayerJob(uuid);
            if (!mission.jobId.equals(playerJob))
                continue;

            // 레벨 요구사항 확인
            int playerLevel = getPlayerLevel(uuid);
            if (playerLevel < mission.requiredLevel - 10)
                continue; // 미션을 받을 수 있는 레벨

            // 진행도 업데이트
            int currentProgress = getProgress(uuid, mission.id);
            int newProgress = currentProgress + amount;
            setProgress(uuid, mission.id, newProgress);

            // 완료 확인
            if (newProgress >= mission.goalAmount) {
                completeMission(player, mission);
            }
        }
    }

    /**
     * 미션 완료 처리
     */
    private void completeMission(Player player, MissionDefinition mission) {
        UUID uuid = player.getUniqueId();

        // 완료 표시
        completedMissions.computeIfAbsent(uuid, k -> new HashSet<>()).add(mission.id);

        // 보상 지급 (경험치/돈)
        JobManager jobManager = plugin.getJobManager();
        if (jobManager != null) {
            jobManager.addExperience(uuid, mission.goalAmount * 0.5);
        }

        // 효과
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.sendTitle(
                "§a§l미션 완료!",
                "§f" + mission.displayName,
                10, 40, 10);
        player.sendMessage("§a[미션] §f" + mission.displayName + " §7미션을 완료했습니다!");

        // 승급 가능 여부 확인
        checkRankUp(player, mission);
    }

    /**
     * 승급 가능 여부 확인
     */
    private void checkRankUp(Player player, MissionDefinition mission) {
        int playerLevel = getPlayerLevel(player.getUniqueId());

        if (playerLevel >= mission.requiredLevel) {
            player.sendMessage("§e[승급] §f새로운 등급으로 승급할 수 있습니다! /job rankup");
        }
    }

    /**
     * 진행도 조회
     */
    public int getProgress(UUID uuid, String missionId) {
        return missionProgress
                .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .getOrDefault(missionId, 0);
    }

    /**
     * 진행도 설정
     */
    public void setProgress(UUID uuid, String missionId, int progress) {
        missionProgress
                .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(missionId, progress);
    }

    /**
     * 미션 완료 여부
     */
    public boolean isMissionCompleted(UUID uuid, String missionId) {
        return completedMissions.getOrDefault(uuid, Set.of()).contains(missionId);
    }

    /**
     * 플레이어의 활성 미션 목록
     */
    public List<MissionDefinition> getActiveMissions(UUID uuid) {
        List<MissionDefinition> active = new ArrayList<>();
        String playerJob = getPlayerJob(uuid);

        for (MissionDefinition mission : missions.values()) {
            if (!mission.jobId.equals(playerJob))
                continue;
            if (isMissionCompleted(uuid, mission.id))
                continue;
            active.add(mission);
        }

        return active;
    }

    /**
     * 플레이어 직업 조회
     */
    private String getPlayerJob(UUID uuid) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return "";

        UserJobData jobData = jobManager.getUserJob(uuid);
        return jobData.hasJob() ? jobData.getJobId() : "";
    }

    /**
     * 플레이어 레벨 조회
     */
    private int getPlayerLevel(UUID uuid) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(uuid);
        return jobData.hasJob() ? jobData.getLevel() : 0;
    }

    /**
     * 미션 타입
     */
    public enum MissionType {
        MINE_BLOCK,
        HARVEST_CROP,
        CATCH_FISH,
        KILL_MOB,
        DISCOVER_BIOME,
        DISCOVER_STRUCTURE,
        CRAFT_ITEM,
        DELIVER_ITEM
    }

    /**
     * 미션 정의 클래스
     */
    public static class MissionDefinition {
        public final String id;
        public final String jobId;
        public final String displayName;
        public final MissionType type;
        public final String target;
        public final int goalAmount;
        public final int requiredLevel;

        public MissionDefinition(String id, String jobId, String displayName,
                MissionType type, String target, int goalAmount, int requiredLevel) {
            this.id = id;
            this.jobId = jobId;
            this.displayName = displayName;
            this.type = type;
            this.target = target;
            this.goalAmount = goalAmount;
            this.requiredLevel = requiredLevel;
        }
    }
}
