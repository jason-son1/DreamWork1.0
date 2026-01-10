package com.dreamwork.core.job;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * 직업 시스템 인터페이스
 * 
 * <p>
 * 모든 직업은 이 인터페이스를 구현하여 일관된 동작을 제공합니다.
 * YAML 설정 파일에서 직업을 로드하고 이 인터페이스를 통해 시스템에 등록됩니다.
 * </p>
 * 
 * <h2>구현 예시:</h2>
 * 
 * <pre>
 * {
 *     &#64;code
 *     public class MinerJob implements JobProvider {
 *         &#64;Override
 *         public String getId() {
 *             return "miner";
 *         }
 * 
 *         @Override
 *         public String getDisplayName() {
 *             return "광부";
 *         }
 *         // ...
 *     }
 * }
 * </pre>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 * @see com.dreamwork.core.manager.Manager
 */
public interface JobProvider {

    /**
     * 직업 고유 ID를 반환합니다.
     * 
     * <p>
     * 소문자와 언더스코어만 사용 권장 (예: "miner", "fisher", "advanced_miner")
     * </p>
     * 
     * @return 직업 ID
     */
    String getId();

    /**
     * 직업 표시 이름을 반환합니다.
     * 
     * <p>
     * 유저에게 보여지는 이름입니다 (예: "광부", "낚시꾼")
     * </p>
     * 
     * @return 표시 이름
     */
    String getDisplayName();

    /**
     * 직업 설명을 반환합니다.
     * 
     * @return 직업 설명
     */
    String getDescription();

    /**
     * 직업 최대 레벨을 반환합니다.
     * 
     * @return 최대 레벨
     */
    int getMaxLevel();

    /**
     * 특정 레벨에 필요한 경험치를 계산합니다.
     * 
     * <p>
     * 레벨업 공식의 예: {@code baseExp * level * multiplier}
     * </p>
     * 
     * @param level 목표 레벨
     * @return 필요 경험치
     */
    double getExpForLevel(int level);

    /**
     * 특정 행동(트리거)에 대한 경험치를 계산합니다.
     * 
     * @param action 행동 유형 (예: "BREAK_BLOCK", "FISH_CATCH", "MOB_KILL")
     * @param target 대상 (예: "DIAMOND_ORE", "SALMON", "ZOMBIE")
     * @return 획득 경험치 (해당 없으면 0)
     */
    double calculateExp(String action, String target);

    /**
     * 레벨업 시 호출되는 콜백입니다.
     * 
     * <p>
     * 보상 지급, 스탯 부여, 메시지 전송 등을 처리합니다.
     * </p>
     * 
     * @param player   플레이어
     * @param oldLevel 이전 레벨
     * @param newLevel 새 레벨
     */
    void onLevelUp(Player player, int oldLevel, int newLevel);

    /**
     * 직업 선택 시 호출되는 콜백입니다.
     * 
     * @param player 플레이어
     */
    void onJobSelect(Player player);

    /**
     * 직업 해제 시 호출되는 콜백입니다.
     * 
     * @param player 플레이어
     */
    void onJobLeave(Player player);

    /**
     * 직업의 경험치 소스 목록을 반환합니다.
     * 
     * <p>
     * 각 소스는 Map 형태로 type, target, amount 등의 정보를 포함합니다.
     * </p>
     * 
     * @return 경험치 소스 목록
     */
    List<Map<String, Object>> getExpSources();

    /**
     * 특정 레벨의 보상 목록을 반환합니다.
     * 
     * @param level 레벨
     * @return 보상 목록 (명령어, 아이템, 스탯 등)
     */
    List<String> getRewards(int level);

    /**
     * 직업 아이콘 Material을 반환합니다.
     * 
     * <p>
     * GUI에서 직업을 표시할 때 사용됩니다.
     * </p>
     * 
     * @return Material 이름 (예: "IRON_PICKAXE")
     */
    String getIcon();

    /**
     * 직업 레벨업 시 부여되는 스탯을 반환합니다.
     * 
     * @return 스탯 이름과 레벨당 증가량 맵
     */
    Map<String, Double> getStatsPerLevel();
}
