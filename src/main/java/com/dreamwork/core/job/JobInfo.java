package com.dreamwork.core.job;

import lombok.Data;

/**
 * 개별 직업의 레벨/경험치 데이터
 * <p>
 * 각 플레이어가 가진 5개 직업 각각의 성장 정보를 저장합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
@Data
public class JobInfo {

    /**
     * 직업 레벨 (1 ~ 100)
     */
    private int level;

    /**
     * 현재 레벨에서의 경험치
     */
    private double currentExp;

    /**
     * 누적 총 경험치
     */
    private double totalExp;

    /**
     * 기본 생성자 - 레벨 1, 경험치 0으로 시작
     */
    public JobInfo() {
        this.level = 1;
        this.currentExp = 0;
        this.totalExp = 0;
    }

    /**
     * 값 지정 생성자
     * 
     * @param level      레벨
     * @param currentExp 현재 경험치
     * @param totalExp   총 경험치
     */
    public JobInfo(int level, double currentExp, double totalExp) {
        this.level = Math.max(1, level);
        this.currentExp = Math.max(0, currentExp);
        this.totalExp = Math.max(0, totalExp);
    }

    /**
     * 경험치를 추가합니다.
     * 
     * @param amount 추가할 경험치량
     */
    public void addExp(double amount) {
        if (amount > 0) {
            this.currentExp += amount;
            this.totalExp += amount;
        }
    }

    /**
     * 레벨업 시 현재 경험치를 차감합니다.
     * 
     * @param requiredExp 레벨업에 필요했던 경험치
     */
    public void levelUp(double requiredExp) {
        this.level++;
        this.currentExp = Math.max(0, this.currentExp - requiredExp);
    }

    /**
     * 레벨업이 가능한지 확인합니다.
     * 
     * @param requiredExp 필요 경험치
     * @return 레벨업 가능 여부
     */
    public boolean canLevelUp(double requiredExp) {
        return this.currentExp >= requiredExp;
    }

    /**
     * 경험치 진행률을 반환합니다. (0.0 ~ 1.0)
     * 
     * @param requiredExp 필요 경험치
     * @return 진행률
     */
    public double getProgress(double requiredExp) {
        if (requiredExp <= 0)
            return 1.0;
        return Math.min(1.0, this.currentExp / requiredExp);
    }

    /**
     * 복사본을 생성합니다.
     * 
     * @return 새 JobInfo 인스턴스
     */
    public JobInfo copy() {
        return new JobInfo(this.level, this.currentExp, this.totalExp);
    }
}
