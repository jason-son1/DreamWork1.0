package com.dreamwork.core.storage;

import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.stat.StatManager.PlayerStats;

import java.util.UUID;

/**
 * 유저 전체 데이터 DTO
 * 
 * <p>
 * 플레이어의 모든 데이터를 담는 통합 클래스입니다.
 * JSON 파일로 저장/로드할 때 이 클래스를 사용합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class UserData {

    /** 플레이어 UUID */
    private UUID uuid;

    /** 플레이어 이름 (동기화용) */
    private String playerName;

    /** 직업 데이터 */
    private UserJobData jobData;

    /** 스탯 데이터 */
    private PlayerStats stats;

    /** 마지막 접속 시간 */
    private long lastLogin;

    /** 최초 접속 시간 */
    private long firstJoin;

    /**
     * 기본 생성자 (Gson 역직렬화용)
     */
    public UserData() {
    }

    /**
     * UUID로 초기화하는 생성자
     * 
     * @param uuid 플레이어 UUID
     */
    public UserData(UUID uuid) {
        this.uuid = uuid;
        this.jobData = new UserJobData(uuid);
        this.stats = new PlayerStats(uuid);
        this.lastLogin = System.currentTimeMillis();
        this.firstJoin = System.currentTimeMillis();
    }

    /**
     * 플레이어 이름과 함께 초기화하는 생성자
     * 
     * @param uuid       플레이어 UUID
     * @param playerName 플레이어 이름
     */
    public UserData(UUID uuid, String playerName) {
        this(uuid);
        this.playerName = playerName;
    }

    // ==================== Getters ====================

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public UserJobData getJobData() {
        return jobData;
    }

    public PlayerStats getStats() {
        return stats;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public long getFirstJoin() {
        return firstJoin;
    }

    // ==================== Setters ====================

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setJobData(UserJobData jobData) {
        this.jobData = jobData;
    }

    public void setStats(PlayerStats stats) {
        this.stats = stats;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void setFirstJoin(long firstJoin) {
        this.firstJoin = firstJoin;
    }

    /**
     * 접속 시간을 현재 시간으로 업데이트합니다.
     */
    public void updateLastLogin() {
        this.lastLogin = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "UserData{" +
                "uuid=" + uuid +
                ", playerName='" + playerName + '\'' +
                ", jobData=" + jobData +
                ", lastLogin=" + lastLogin +
                '}';
    }
}
