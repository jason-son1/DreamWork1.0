package com.dreamwork.core.job.engine;

import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

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
        } else if (event instanceof PlayerFishEvent fishEvent) {
            if (fishEvent.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                return TriggerType.FISH_CATCH;
            }
        } else if (event instanceof EntityDeathEvent) {
            return TriggerType.MOB_KILL;
        } else if (event instanceof PlayerHarvestBlockEvent) {
            return TriggerType.HARVEST;
        } else if (event instanceof CraftItemEvent) {
            return TriggerType.CRAFT_ITEM;
        } else if (event instanceof PlayerItemConsumeEvent) {
            return TriggerType.EAT_FOOD;
        }

        return null; // 매핑되지 않은 이벤트
    }

    /**
     * 이벤트로부터 대상(target) 정보를 추출합니다.
     * 
     * @param event 발생한 이벤트
     * @return 대상 식별자 (Material 이름, Entity 타입 등)
     */
    public String getTarget(Event event) {
        if (event instanceof BlockBreakEvent blockEvent) {
            return blockEvent.getBlock().getType().name();
        } else if (event instanceof PlayerFishEvent fishEvent) {
            if (fishEvent.getCaught() != null) {
                return fishEvent.getCaught().getType().name();
            }
            return "FISH";
        } else if (event instanceof EntityDeathEvent deathEvent) {
            return deathEvent.getEntity().getType().name();
        } else if (event instanceof PlayerHarvestBlockEvent harvestEvent) {
            return harvestEvent.getHarvestedBlock().getType().name();
        } else if (event instanceof CraftItemEvent craftEvent) {
            if (craftEvent.getRecipe().getResult() != null) {
                return craftEvent.getRecipe().getResult().getType().name();
            }
        } else if (event instanceof PlayerItemConsumeEvent consumeEvent) {
            return consumeEvent.getItem().getType().name();
        }

        return null;
    }
}
