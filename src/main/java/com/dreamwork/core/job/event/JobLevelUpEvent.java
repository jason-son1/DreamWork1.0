package com.dreamwork.core.job.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 직업 레벨업 이벤트
 * 
 * <p>
 * 플레이어가 직업 레벨업을 할 때 호출됩니다.
 * 이 이벤트를 통해 추가 보상을 지급하거나 효과를 줄 수 있습니다.
 * </p>
 */
public class JobLevelUpEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final String jobId;
    private final int oldLevel;
    private final int newLevel;

    public JobLevelUpEvent(Player player, String jobId, int oldLevel, int newLevel) {
        this.player = player;
        this.jobId = jobId;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public String getJobId() {
        return jobId;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
