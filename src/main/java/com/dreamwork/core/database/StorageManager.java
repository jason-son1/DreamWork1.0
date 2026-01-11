package com.dreamwork.core.database;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.model.UserData;
import com.dreamwork.core.quest.QuestProgress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StorageManager {

    private final DreamWorkCore plugin;
    private final DatabaseManager dbManager;
    private final Gson gson;

    private final Map<UUID, UserData> userCache = new java.util.concurrent.ConcurrentHashMap<>();

    public StorageManager(DreamWorkCore plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.gson = new Gson();
    }

    public UserData getUserData(UUID uuid) {
        return userCache.get(uuid);
    }

    public void unloadUser(UUID uuid) {
        userCache.remove(uuid);
    }

    /**
     * 유저 데이터를 비동기로 저장합니다.
     */
    public void saveUserAsync(UserData user) {
        if (!user.isDirty())
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection()) {
                String sql = "REPLACE INTO dw_users (uuid, name, job_id, job_level, job_exp, str, dex, con, intel, luk, stat_points, last_daily_reset, quest_data, current_mana) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, user.getUuid().toString());
                    pstmt.setString(2, user.getName());
                    pstmt.setString(3, user.getJobId());
                    pstmt.setInt(4, user.getJobLevel());
                    pstmt.setDouble(5, user.getJobExp());
                    pstmt.setInt(6, user.getStr());
                    pstmt.setInt(7, user.getDex());
                    pstmt.setInt(8, user.getCon());
                    pstmt.setInt(9, user.getIntel());
                    pstmt.setInt(10, user.getLuk());
                    pstmt.setInt(11, user.getStatPoints());
                    pstmt.setString(12, user.getLastDailyReset() != null ? user.getLastDailyReset().toString() : null);
                    pstmt.setString(13, gson.toJson(user.getQuestProgresses()));
                    pstmt.setDouble(14, user.getCurrentMana());

                    pstmt.executeUpdate();

                    // 저장 완료 후 더티 플래그 해제 (여기서 해제해도 다음 틱에 변경 생기면 다시 true 되므로 안전)
                    user.setDirty(false);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("유저 데이터 저장 실패: " + user.getName());
                e.printStackTrace();
            }
        });
    }

    /**
     * 유저 데이터를 비동기로 로드합니다.
     */
    public CompletableFuture<UserData> loadUserAsync(UUID uuid, String name) {
        CompletableFuture<UserData> future = new CompletableFuture<>();

        // 캐시에 있으면 그걸 사용
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
                        user.setJobId(rs.getString("job_id"));
                        user.setJobLevel(rs.getInt("job_level"));
                        user.setJobExp(rs.getDouble("job_exp"));
                        user.setStr(rs.getInt("str"));
                        user.setDex(rs.getInt("dex"));
                        user.setCon(rs.getInt("con"));
                        user.setIntel(rs.getInt("intel"));
                        user.setLuk(rs.getInt("luk"));
                        user.setStatPoints(rs.getInt("stat_points"));
                        try {
                            user.setCurrentMana(rs.getDouble("current_mana"));
                        } catch (Exception ignored) {
                        }

                        String lastReset = rs.getString("last_daily_reset");
                        if (lastReset != null) {
                            user.setLastDailyReset(LocalDate.parse(lastReset));
                        }

                        String questJson = rs.getString("quest_data");
                        if (questJson != null && !questJson.isEmpty()) {
                            Type type = new TypeToken<Map<String, QuestProgress>>() {
                            }.getType();
                            Map<String, QuestProgress> quests = gson.fromJson(questJson, type);
                            if (quests != null) {
                                user.setQuestProgresses(quests);
                            }
                        }
                    }
                    // 캐시 등록 (메인스레드 동기화가 안전하지만, Map이 Concurrent라 일단 Put)
                    userCache.put(uuid, user);
                    future.complete(user);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("유저 데이터 로드 실패: " + name);
                e.printStackTrace();
                // 에러 시 빈 데이터 반환 (접속 허용)
                UserData user = new UserData(uuid, name);
                userCache.put(uuid, user);
                future.complete(user);
            }
        });

        return future;
    }

    public Map<UUID, UserData> getUserCache() {
        return userCache;
    }
}
