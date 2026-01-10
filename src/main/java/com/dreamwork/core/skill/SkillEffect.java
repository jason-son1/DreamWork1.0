package com.dreamwork.core.skill;

import org.bukkit.entity.Player;

/**
 * 스킬 효과 인터페이스
 * 
 * <p>
 * 모든 스킬은 이 인터페이스를 구현합니다.
 * </p>
 */
public interface SkillEffect {

    /**
     * 스킬을 실행합니다.
     * 
     * @param player 스킬 사용자
     */
    void execute(Player player);

    /**
     * 스킬 ID를 반환합니다.
     */
    String getId();

    /**
     * 스킬 이름을 반환합니다.
     */
    String getName();

    /**
     * 스킬 설명을 반환합니다.
     */
    String getDescription();

    /**
     * 쿨타임(초)을 반환합니다.
     */
    int getCooldown();

    /**
     * 마나/기력 소모량을 반환합니다.
     */
    int getManaCost();

    /**
     * 필요 레벨을 반환합니다.
     */
    default int getRequiredLevel() {
        return 1;
    }

    /**
     * 필요 직업 ID를 반환합니다. (null이면 모든 직업)
     */
    default String getRequiredJob() {
        return null;
    }
}
