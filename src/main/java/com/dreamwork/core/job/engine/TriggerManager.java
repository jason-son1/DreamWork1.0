package com.dreamwork.core.job.engine;

import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;

/**
 * 이벤트를 TriggerType으로 매핑하는 매니저
 */
public class TriggerManager {

    /**
     * 이벤트로부터 트리거 타입을 반환합니다.
     * 
     * @param event 발생한 이벤트
     * @return 매핑된 TriggerType (매핑 불가시 null)
     */
    public TriggerType getTriggerFromEvent(Event event) {
        if (event instanceof BlockBreakEvent) {
            return TriggerType.BLOCK_BREAK;
        } else if (event instanceof PlayerFishEvent) {
            PlayerFishEvent fishEvent = (PlayerFishEvent) event;
            if (fishEvent.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                return TriggerType.FISH_CATCH;
            }
        } else if (event instanceof EntityDeathEvent) {
            return TriggerType.MOB_KILL;
        } else if (event instanceof PlayerHarvestBlockEvent) {
            return TriggerType.HARVEST;
        }

        return null; // 매핑되지 않은 이벤트
    }
}
