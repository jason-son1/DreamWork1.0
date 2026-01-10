package com.dreamwork.core.job.engine;

import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.JobProvider;
import org.bukkit.entity.Player;

/**
 * 보상 처리 엔진
 */
public class RewardProcessor {

    private final JobManager jobManager;

    public RewardProcessor(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    /**
     * 보상을 처리합니다. (경험치 지급 등)
     * 
     * @param provider 직업 제공자
     * @param player   플레이어
     * @param trigger  트리거 타입
     * @param exp      획득할 경험치
     */
    public void process(JobProvider provider, Player player, TriggerType trigger, double exp) {
        if (exp <= 0)
            return;

        // 1. 경험치 추가 (JobManager 위임 -> 레벨업 체크 자동 수행)
        jobManager.addExp(player, exp);

        // 2. 추가 보상 (확률형 아이템 드롭, 돈 지급 등)
        // 현재 JobProvider 인터페이스에는 getRewards(level)만 있지만,
        // 추후 행동별 보상(action rewards)이 추가될 수 있음.

        // 예: 낚시 성공 시 1% 확률로 추가 아이템
        if (trigger == TriggerType.FISH_CATCH) {
            // ...
        }
    }
}
