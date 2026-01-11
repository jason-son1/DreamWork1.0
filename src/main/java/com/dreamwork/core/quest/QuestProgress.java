package com.dreamwork.core.quest;

/**
 * 플레이어의 퀘스트 진행 상태
 */
public class QuestProgress {

    private String questId;
    private int currentProgress;
    private QuestStatus status;

    public QuestProgress() {
    }

    public QuestProgress(String questId) {
        this.questId = questId;
        this.currentProgress = 0;
        this.status = QuestStatus.IN_PROGRESS;
    }

    public String getQuestId() {
        return questId;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void addProgress(int amount) {
        this.currentProgress += amount;
    }

    public void setProgress(int progress) {
        this.currentProgress = progress;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public void setStatus(QuestStatus status) {
        this.status = status;
    }

    /**
     * 요구량과 비교하여 완료 여부 확인
     */
    public boolean isComplete(int requiredAmount) {
        return currentProgress >= requiredAmount;
    }

    /**
     * 퀘스트 상태 Enum
     */
    public enum QuestStatus {
        NOT_STARTED, // 아직 시작 안 함
        IN_PROGRESS, // 진행 중
        COMPLETED, // 완료 (보상 미수령)
        REWARDED // 보상 수령 완료
    }
}
