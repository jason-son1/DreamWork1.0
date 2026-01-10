package com.dreamwork.core.job;

import java.util.UUID;

/**
 * 유저의 직업 데이터 DTO
 * 
 * <p>
 * 플레이어의 직업, 레벨, 경험치 정보를 담는 데이터 클래스입니다.
 * JSON 직렬화/역직렬화에 사용됩니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class UserJobData {

    /** 플레이어 UUID */
    private UUID uuid;

    /** 현재 직업 ID */
    private String jobId;

    /** 현재 레벨 */
    private int level;

    /** 현재 경험치 (레벨업에 필요한 경험치 기준) */
    private double currentExp;

    /** 누적 총 경험치 (랭킹용) */
    private double totalExp;

    /**
     * 기본 생성자 (Gson 역직렬화용)
     */
    public UserJobData() {
        this.level = 1;
        this.currentExp = 0;
        this.totalExp = 0;
    }

    /**
     * UUID로 초기화하는 생성자
     * 
     * @param uuid 플레이어 UUID
     */
    public UserJobData(UUID uuid) {
        this.uuid = uuid;
        this.jobId = null;
        this.level = 1;
        this.currentExp = 0;
        this.totalExp = 0;
    }

    /**
     * 모든 필드를 초기화하는 생성자
     * 
     * @param uuid       플레이어 UUID
     * @param jobId      직업 ID
     * @param level      레벨
     * @param currentExp 현재 경험치
     * @param totalExp   총 경험치
     */
    public UserJobData(UUID uuid, String jobId, int level, double currentExp, double totalExp) {
        this.uuid = uuid;
        this.jobId = jobId;
        this.level = level;
        this.currentExp = currentExp;
        this.totalExp = totalExp;
    }

    // ==================== Getters ====================

    public UUID getUuid() {
        return uuid;
    }

    public String getJobId() {
        return jobId;
    }

    public int getLevel() {
        return level;
    }

    public double getCurrentExp() {
        return currentExp;
    }

    public double getTotalExp() {
        return totalExp;
    }

    // ==================== Setters ====================

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setCurrentExp(double currentExp) {
        this.currentExp = currentExp;
    }

    public void setTotalExp(double totalExp) {
        this.totalExp = totalExp;
    }

    // ==================== Utility Methods ====================

    /**
     * 경험치를 추가합니다.
     * 
     * @param amount 추가할 경험치
     */
    public void addExp(double amount) {
        this.currentExp += amount;
        this.totalExp += amount;
    }

    /**
     * 레벨업 처리 후 현재 경험치를 조정합니다.
     * 
     * @param requiredExp 레벨업에 필요했던 경험치
     */
    public void levelUp(double requiredExp) {
        this.level++;
        this.currentExp -= requiredExp;
        if (this.currentExp < 0) {
            this.currentExp = 0;
        }
    }

    /**
     * 직업이 설정되어 있는지 확인합니다.
     * 
     * @return 직업 설정 여부
     */
    public boolean hasJob() {
        return jobId != null && !jobId.isEmpty();
    }

    /**
     * 데이터를 복사합니다.
     * 
     * @return 복사된 UserJobData
     */
    public UserJobData copy() {
        return new UserJobData(uuid, jobId, level, currentExp, totalExp);
    }

    @Override
    public String toString() {
        return "UserJobData{" +
                "uuid=" + uuid +
                ", jobId='" + jobId + '\'' +
                ", level=" + level +
                ", currentExp=" + currentExp +
                ", totalExp=" + totalExp +
                '}';
    }
}
