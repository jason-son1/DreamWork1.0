package com.dreamwork.core.rank;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.manager.Manager;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 랭킹 시스템 매니저
 * 
 * <p>
 * DB를 주기적으로 스캔하여
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
     * 랭킹 업데이트 로직 (비동기)
     */
    private void updateRanking() {
        if (plugin.getDatabaseManager() == null)
            return;

        List<RankEntry> tempRanking = new ArrayList<>();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // 레벨 내림차순, 경험치 내림차순으로 상위 100명 조회
            // 레벨 내림차순, 경험치 내림차순으로 상위 100명 조회
            String sql = "SELECT u.uuid, u.name, j.job_id, j.level, j.exp FROM dw_jobs j JOIN dw_users u ON j.uuid = u.uuid ORDER BY j.level DESC, j.exp DESC LIMIT 100";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    String name = rs.getString("name");
                    String jobId = rs.getString("job_id");
                    int level = rs.getInt("level");
                    double exp = rs.getDouble("exp");

                    if (jobId == null || jobId.isEmpty())
                        continue;

                    tempRanking.add(new RankEntry(
                            UUID.fromString(uuidStr),
                            name,
                            jobId,
                            level,
                            exp,
                            calculateTotalScore(level, exp)));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("랭킹 업데이트 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }

        // 정렬 (점수 내림차순) - DB에서 정렬해오지만, Score 계산식이 다르면 여기서 다시 정렬
        tempRanking.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 캐시 업데이트
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
        // 레벨 * 10000 + 경험치
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
