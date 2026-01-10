package com.dreamwork.core.job.engine;

/**
 * 직업 활동 트리거 타입 (이벤트 유형)
 */
public enum TriggerType {
    BLOCK_BREAK,
    FISH_CATCH,
    MOB_KILL,
    HARVEST;

    /**
     * 문자열로부터 트리거 타입을 반환합니다. 대소문자 무시.
     */
    public static TriggerType fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
