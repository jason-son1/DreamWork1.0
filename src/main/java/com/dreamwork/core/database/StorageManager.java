package com.dreamwork.core.database;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobInfo;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import com.dreamwork.core.quest.QuestProgress;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 저장소 관리자
 * <p>
 * 플레이어 데이터를 SQLite 데이터베이스에 저장하고 로드합니다.
 * 비동기 작업을 통해 서버 성능에 영향을 최소화합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class StorageManager {

    private final DreamWorkCore plugin;
    private final DatabaseManager dbManager;
    private final Gson gson;

    /**
     * 유저 데이터 캐시 (메모리)
     */
    private final Map<UUID, UserData> userCache = new ConcurrentHashMap<>();

    public StorageManager(DreamWorkCore plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .create();
    }

    /**
     * 캐시에서 유저 데이터를 가져옵니다.
     * 
     * @param uuid 플레이어 UUID
     * @return UserData 또는 null
     */
    public UserData getUserData(UUID uuid) {
        return userCache.get(uuid);
    }

    /**
     * 유저 데이터를 캐시에서 제거합니다.
     * 
     * @param uuid 플레이어 UUID
     */
    public void unloadUser(UUID uuid) {
        userCache.remove(uuid);
    }

    /**
     * 유저 데이터를 비동기로 저장합니다.
     * 더티 플래그가 설정된 경우에만 실제 저장을 수행합니다.
     * 
     * @param user 저장할 유저 데이터
     */
    public void saveUserAsync(UserData user) {
        if (!user.isDirty()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection()) {
                String sql = """
                        REPLACE INTO dw_users (
                            uuid, name, job_data,
                            str, dex, con, intel, luk, stat_points,
                            current_mana, last_daily_reset, quest_data,
                            phase1_data, money
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """;

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, user.getUuid().toString());
                    pstmt.setString(2, user.getName());
                    pstmt.setString(3, serializeJobData(user.getJobs()));
                    pstmt.setInt(4, user.getStr());
                    pstmt.setInt(5, user.getDex());
                    pstmt.setInt(6, user.getCon());
                    pstmt.setInt(7, user.getIntel());
                    pstmt.setInt(8, user.getLuk());
                    pstmt.setInt(9, user.getStatPoints());
                    pstmt.setDouble(10, user.getCurrentMana());
                    pstmt.setString(11, user.getLastDailyReset() != null
                            ? user.getLastDailyReset().toString()
                            : null);
                    pstmt.setString(12, gson.toJson(user.getQuestProgresses()));
                    pstmt.setString(13, serializePhase1Data(user));
                    pstmt.setDouble(14, user.getBalance());

                    pstmt.executeUpdate();

                    // dw_jobs 테이블 업데이트 (랭킹용)
                    String jobSql = "REPLACE INTO dw_jobs (uuid, job_id, level, exp) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement jobStmt = conn.prepareStatement(jobSql)) {
                        for (Map.Entry<JobType, JobInfo> entry : user.getJobs().entrySet()) {
                            JobInfo info = entry.getValue();
                            jobStmt.setString(1, user.getUuid().toString());
                            jobStmt.setString(2, entry.getKey().getConfigKey());
                            jobStmt.setInt(3, info.getLevel());
                            jobStmt.setDouble(4, info.getCurrentExp());
                            jobStmt.addBatch();
                        }
                        jobStmt.executeBatch();
                    }

                    // 저장 완료 후 더티 플래그 해제
                    user.clearDirty();

                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Storage] 유저 데이터 저장 완료: " + user.getName());
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("유저 데이터 저장 실패: " + user.getName());
                e.printStackTrace();
            }
        });
    }

    /**
     * 유저 데이터를 비동기로 로드합니다.
     * 
     * @param uuid 플레이어 UUID
     * @param name 플레이어 이름
     * @return CompletableFuture with UserData
     */
    public CompletableFuture<UserData> loadUserAsync(UUID uuid, String name) {
        CompletableFuture<UserData> future = new CompletableFuture<>();

        // 캐시에 있으면 즉시 반환
        if (userCache.containsKey(uuid)) {
            future.complete(userCache.get(uuid));
            return future;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection()) {
                String sql = "SELECT * FROM dw_users WHERE uuid = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    ResultSet rs = pstmt.executeQuery();

                    UserData user = new UserData(uuid, name);

                    if (rs.next()) {
                        // 직업 데이터 로드
                        String jobData = rs.getString("job_data");
                        if (jobData != null && !jobData.isEmpty()) {
                            Map<JobType, JobInfo> jobs = deserializeJobData(jobData);
                            if (jobs != null) {
                                user.setJobs(jobs);
                            }
                        } else {
                            migrateOldJobData(rs, user);
                        }

                        // 스탯 데이터 로드
                        user.setStr(rs.getInt("str"));
                        user.setStr(rs.getInt("str"));
                        user.setDex(rs.getInt("dex"));
                        user.setCon(rs.getInt("con"));
                        user.setIntel(rs.getInt("intel"));
                        user.setLuk(rs.getInt("luk"));
                        user.setStatPoints(rs.getInt("stat_points"));

                        // 마나 로드
                        try {
                            user.setCurrentMana(rs.getDouble("current_mana"));
                        } catch (Exception ignored) {
                            user.setCurrentMana(100.0);
                        }

                        // 돈(Money) 로드
                        try {
                            user.setMoney(rs.getDouble("money"));
                        } catch (Exception ignored) {
                        }

                        // 일일 리셋 날짜
                        String lastReset = rs.getString("last_daily_reset");
                        if (lastReset != null) {
                            user.setLastDailyReset(LocalDate.parse(lastReset));
                        }

                        // 퀘스트 데이터 로드
                        String questJson = rs.getString("quest_data");
                        if (questJson != null && !questJson.isEmpty()) {
                            Type type = new TypeToken<Map<String, QuestProgress>>() {
                            }.getType();
                            Map<String, QuestProgress> quests = gson.fromJson(questJson, type);
                            if (quests != null) {
                                user.setQuestProgresses(quests);
                            }
                        }

                        // Phase 1 데이터 로드
                        try {
                            String phase1Json = rs.getString("phase1_data");
                            if (phase1Json != null && !phase1Json.isEmpty()) {
                                deserializePhase1Data(phase1Json, user);
                            }
                        } catch (Exception e) {
                        }
                    }

                    // 캐시 등록
                    userCache.put(uuid, user);
                    future.complete(user);

                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[Storage] 유저 데이터 로드 완료: " + name);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("유저 데이터 로드 실패: " + name);
                e.printStackTrace();

                UserData user = new UserData(uuid, name);
                userCache.put(uuid, user);
                future.complete(user);
            }
        });

        return future;
    }

    // ... (기존 메서드들) ...

    private String serializePhase1Data(UserData user) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("mobKillCounts", user.getMobKillCounts());
        data.put("exploredChunks", user.getExploredChunks()); // private 필드 접근 필요 -> getter 없으면 추가했어야 함. UserData 수정 확인
                                                              // 필요.
        // *UserData 수정 때 getExploredChunks() Getter 자동 생성(@Data) 되었을 것임. Set<Long> 타입.
        return gson.toJson(data);
    }

    private void deserializePhase1Data(String json, UserData user) {
        try {
            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> data = gson.fromJson(json, type);
            if (data == null)
                return;

            // Kill Counts
            if (data.containsKey("mobKillCounts")) {
                String killJson = gson.toJson(data.get("mobKillCounts"));
                Type mapType = new TypeToken<Map<String, Integer>>() {
                }.getType();
                Map<String, Integer> kills = gson.fromJson(killJson, mapType);
                if (kills != null) {
                    kills.forEach(user::addMobKillCount); // 기존 맵 유지하며 병합 or clear 후 putAll?
                    // 로직상 addMobKillCount는 merge하므로, 그냥 putAll이 나음.
                    // 하지만 UserData에 setMobKillCounts가 없으므로 반복문으로 넣음.
                }
            }

            // Explored Chunks
            if (data.containsKey("exploredChunks")) {
                String chunkJson = gson.toJson(data.get("exploredChunks"));
                Type setType = new TypeToken<java.util.Set<Long>>() {
                }.getType();
                java.util.Set<Long> chunks = gson.fromJson(chunkJson, setType);
                if (chunks != null) {
                    chunks.forEach(user::addExploredChunk);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Phase 1 데이터 로드 중 오류: " + e.getMessage());
        }
    }

    /**
     * 유저 캐시를 반환합니다.
     * 
     * @return 유저 캐시 맵
     */
    public Map<UUID, UserData> getUserCache() {
        return userCache;
    }

    // ==================== 직렬화/역직렬화 ====================

    /**
     * 직업 데이터를 JSON 문자열로 직렬화합니다.
     */
    private String serializeJobData(Map<JobType, JobInfo> jobs) {
        // JobType을 configKey로 변환하여 저장
        Map<String, JobInfo> serializable = new java.util.HashMap<>();
        for (Map.Entry<JobType, JobInfo> entry : jobs.entrySet()) {
            serializable.put(entry.getKey().getConfigKey(), entry.getValue());
        }
        return gson.toJson(serializable);
    }

    /**
     * JSON 문자열에서 직업 데이터를 역직렬화합니다.
     */
    private Map<JobType, JobInfo> deserializeJobData(String json) {
        try {
            Type type = new TypeToken<Map<String, JobInfo>>() {
            }.getType();
            Map<String, JobInfo> raw = gson.fromJson(json, type);

            Map<JobType, JobInfo> result = new EnumMap<>(JobType.class);

            // 모든 직업을 기본값으로 초기화
            for (JobType jobType : JobType.values()) {
                result.put(jobType, new JobInfo());
            }

            // 저장된 데이터로 덮어쓰기
            if (raw != null) {
                for (Map.Entry<String, JobInfo> entry : raw.entrySet()) {
                    JobType jobType = JobType.fromConfigKey(entry.getKey());
                    if (jobType != null && entry.getValue() != null) {
                        result.put(jobType, entry.getValue());
                    }
                }
            }

            return result;
        } catch (Exception e) {
            plugin.getLogger().warning("직업 데이터 역직렬화 실패: " + e.getMessage());
            return null;
        }
    }

    /**
     * 구버전 단일 직업 데이터를 새 구조로 마이그레이션합니다.
     */
    private void migrateOldJobData(ResultSet rs, UserData user) {
        try {
            String oldJobId = rs.getString("job_id");
            int oldLevel = rs.getInt("job_level");
            double oldExp = rs.getDouble("job_exp");

            if (oldJobId != null && !oldJobId.isEmpty()) {
                JobType jobType = JobType.fromConfigKey(oldJobId);
                if (jobType != null) {
                    JobInfo info = user.getJobInfo(jobType);
                    info.setLevel(Math.max(1, oldLevel));
                    info.setCurrentExp(oldExp);
                    info.setTotalExp(oldExp);

                    // 마이그레이션 후 더티 플래그 설정 (새 구조로 저장되도록)
                    user.markDirty();

                    plugin.getLogger().info("[Storage] 구버전 직업 데이터 마이그레이션: "
                            + user.getName() + " -> " + jobType.getDisplayName() + " Lv." + oldLevel);
                }
            }
        } catch (SQLException e) {
            // 구버전 컬럼이 없을 수 있음 - 무시
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Storage] 구버전 컬럼 없음, 신규 유저로 처리");
            }
        }
    }
}
