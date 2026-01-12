package com.dreamwork.core.job.engine;

/**
 * 직업 활동 트리거 타입 (이벤트 유형)
 * <p>
 * 플레이어의 행동을 감지하여 해당 직업에 경험치를 부여하는 기준입니다.
 * 각 트리거는 특정 직업(들)과 연관됩니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public enum TriggerType {

    // ==================== 광부 (Miner) ====================
    /**
     * 블록 파괴
     */
    BLOCK_BREAK,

    // ==================== 농부 (Farmer) ====================
    /**
     * 작물 수확
     */
    HARVEST,

    /**
     * 동물 번식
     */
    BREED_ANIMAL,

    // ==================== 어부 (Fisher) ====================
    /**
     * 물고기 낚기
     */
    FISH_CATCH,

    // ==================== 사냥꾼 (Hunter) ====================
    /**
     * 엔티티 사냥
     */
    ENTITY_KILL,

    /**
     * @deprecated ENTITY_KILL 사용
     */
    @Deprecated
    MOB_KILL,

    // ==================== 탐험가 (Explorer) ====================
    /**
     * 새로운 청크 발견
     */
    DISCOVER_CHUNK,

    /**
     * 새로운 바이옴 발견
     */
    DISCOVER_BIOME,

    /**
     * 구조물 발견 (마을, 사원, 유적 등)
     */
    DISCOVER_STRUCTURE,

    /**
     * 이동 거리
     */
    TRAVEL_DISTANCE,

    /**
     * @deprecated TRAVEL_DISTANCE 사용
     */
    @Deprecated
    TRAVEL,

    // ==================== 공통 ====================
    /**
     * 아이템 제작
     */
    CRAFT_ITEM,

    /**
     * 음식 섭취
     */
    EAT_FOOD,

    /**
     * 퀘스트 완료
     */
    QUEST_COMPLETE;

    /**
     * 문자열로부터 트리거 타입을 반환합니다. 대소문자 무시.
     * 
     * @param name 트리거 이름
     * @return TriggerType 또는 null
     */
    public static TriggerType fromString(String name) {
        if (name == null)
            return null;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 별칭 처리
            return switch (name.toUpperCase()) {
                case "BREAK_BLOCK" -> BLOCK_BREAK;
                case "KILL_ENTITY", "KILL_MOB" -> ENTITY_KILL;
                case "FISH_CAUGHT", "FISHING" -> FISH_CATCH;
                case "MOVE_REGION", "MOVE" -> TRAVEL_DISTANCE;
                case "CHUNK_DISCOVER" -> DISCOVER_CHUNK;
                case "BIOME_DISCOVER" -> DISCOVER_BIOME;
                case "STRUCTURE_DISCOVER" -> DISCOVER_STRUCTURE;
                default -> null;
            };
        }
    }
}
