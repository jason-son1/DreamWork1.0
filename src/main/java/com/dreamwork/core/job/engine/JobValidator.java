package com.dreamwork.core.job.engine;

import com.dreamwork.core.job.JobProvider;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 직업 행동 유효성 검사기
 */
public class JobValidator {

    /**
     * 행동이 유효한지 검사합니다.
     * 
     * @param player   플레이어
     * @param trigger  트리거 타입
     * @param target   대상 오브젝트 (블럭, 몹 등 이름)
     * @param provider 직업 제공자
     * @return 유효 여부
     */
    public boolean isValidAction(Player player, TriggerType trigger, String target, JobProvider provider) {
        // 1. Creative 모드 체크 (일반적으로 경험치 획득 불가)
        if (player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }

        // 2. 타겟 유효성 체크
        if (target == null || target.isEmpty()) {
            return false;
        }

        // 3. 트리거별 추가 조건 체크
        switch (trigger) {
            case BLOCK_BREAK:
                // 실크터치 체크 (설정에 따라 다를 수 있지만 기본적으로 광물은 실크터치 시 경험치 제외 가능)
                // 여기서는 예시로 다이아몬드 광석 등은 실크터치면 무효 처리 등 로직 가능
                // 현재 단순 구현:
                if (hasSilkTouch(player)) {
                    // 광물의 경우 실크터치면 경험치 획득 안됨 (바닐라 마인크래프트 규칙과 유사하게 적용 가능)
                    // 하지만 플러그인 설정에 따라 다름. 일단 패스.
                }
                break;

            case MOB_KILL:
                // 스포너 몹 체크 등
                break;

            default:
                break;
        }

        // 4. JobProvider가 해당 타겟에 대한 경험치 정의를 가지고 있는지 확인
        // (calculateExp가 0보다 크면 유효하다고 볼 수도 있음)

        return true;
    }

    private boolean hasSilkTouch(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return mainHand != null && mainHand.containsEnchantment(Enchantment.SILK_TOUCH);
    }
}
