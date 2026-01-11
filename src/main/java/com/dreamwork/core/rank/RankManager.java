package com.dreamwork.core.rank;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.manager.Manager;
import com.dreamwork.core.storage.UserData;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 랭킹 시스템 매니저
 * 
 * <p>
 * 유저 데이터 파일들을 주기적으로 스캔하여
 * 종합 레벨과 경험치 기준으로 순위를 산정합니다.
 * </p>
 */
public class RankManager extends Manager {

    private final DreamWorkCore plugin;
    private final List<RankEntry> cachedRanking = new ArrayList<>();
    private final Map<UUID, Integer> playerRankCache = new ConcurrentHashMap<>();
    private long lastUpdateTime = 0;

    // 랭킹 업데이트 주기 (5분)
    private static final long UPDATE_INTERVAL = 5 * 60 * 1000L;

    public RankManager(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        // 초기 랭킹 업데이트
        updateRankingAsync();

        // 주기적 업데이트 (10분마다)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateRanking, 20 * 60 * 10, 20 * 60 * 10);

        enabled = true;
        plugin.getLogger().info("RankManager 활성화 완료");
    }

    @Override
    public void onDisable() {
        cachedRanking.clear();
        playerRankCache.clear();
        enabled = false;
    }

    @Override
    public void reload() {
        // 랭킹 리로드: 즉시 업데이트 수행
        updateRankingAsync();
    }

    /**
     * 비동기로 랭킹을 업데이트합니다.
     */
    public CompletableFuture<Void> updateRankingAsync() {
        return CompletableFuture.runAsync(this::updateRanking);
    }

    /**
     * 랭킹 업데이트 로직 (비동기 권장)
     */
    private void updateRanking() {
        if (plugin.getStorageManager() == null)
            return;

        File folder = plugin.getStorageManager().getUserdataFolder();
        if (folder == null || !folder.exists())
            return;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null)
            return;

        List<RankEntry> tempRanking = new ArrayList<>();

        for (File file : files) {
            try {
                // UserData를 직접 로드하지 않고 필요한 필드만 읽으면 좋겠지만,
                // StorageManager 구조상 readFile 사용이 어려우므로 전체 로드 (비동기라 괜찮음)
                // Gson 인스턴스 접근 필요
                UserData data = plugin.getStorageManager().getGson().fromJson(
                        java.nio.file.Files.readString(file.toPath()), UserData.class);

                if (data != null && data.getJobData() != null && data.getJobData().hasJob()) {
                    UserJobData jobData = data.getJobData();
                    tempRanking.add(new RankEntry(
                            data.getUuid(),
                            data.getPlayerName(),
                            jobData.getJobId(),
                            jobData.getLevel(),
                            jobData.getCurrentExp(), // 수정됨
                            calculateTotalScore(jobData.getLevel(), jobData.getCurrentExp()) // 수정됨
                    ));
                }
            } catch (Exception e) {
                // 파일 읽기 오류 무시
            }
        }

        // 정렬 (점수 내림차순)
        tempRanking.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 캐시 업데이트 (동기화 필요없음, 리스트 교체 방식)
        synchronized (cachedRanking) {
            cachedRanking.clear();
            cachedRanking.addAll(tempRanking);
        }

        // 플레이어별 순위 맵 업데이트
        playerRankCache.clear();
        for (int i = 0; i < tempRanking.size(); i++) {
            playerRankCache.put(tempRanking.get(i).getUuid(), i + 1);
        }

        lastUpdateTime = System.currentTimeMillis();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[RankManager] 랭킹 업데이트 완료 (" + tempRanking.size() + "명)");
        }
    }

    private double calculateTotalScore(int level, double exp) {
        // 레벨 * 1000 + 경험치 (간단한 스코어링)
        return (level * 100000.0) + exp;
    }

    /**
     * Top N 랭킹을 가져옵니다.
     * 
     * @param limit 가져올 인원 수
     */
    public List<RankEntry> getTopRankers(int limit) {
        synchronized (cachedRanking) {
            return cachedRanking.stream().limit(limit).collect(Collectors.toList());
        }
    }

    /**
     * 특정 플레이어의 순위를 가져옵니다.
     * 
     * @return 1부터 시작하는 순위 (데이터 없으면 -1)
     */
    public int getPlayerRank(UUID uuid) {
        return playerRankCache.getOrDefault(uuid, -1);
    }

    /**
     * 랭킹 엔트리 데이터 클래스
     */
    public static class RankEntry {
        private final UUID uuid;
        private final String name;
        private final String job;
        private final int level;
        private final double exp;
        private final double score;

        public RankEntry(UUID uuid, String name, String job, int level, double exp, double score) {
            this.uuid = uuid;
            this.name = name != null ? name : "Unknown";
            this.job = job;
            this.level = level;
            this.exp = exp;
            this.score = score;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public String getJob() {
            return job;
        }

        public int getLevel() {
            return level;
        }

        public double getExp() {
            return exp;
        }

        public double getScore() {
            return score;
        }
    }
}
