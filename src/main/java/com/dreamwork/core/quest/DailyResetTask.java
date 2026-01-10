package com.dreamwork.core.quest;

import com.dreamwork.core.DreamWorkCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 일일 퀘스트 리셋 태스크
 * 
 * <p>
 * 매일 00:00에 일일 퀘스트를 초기화합니다.
 * </p>
 */
public class DailyResetTask extends BukkitRunnable {

    private final DreamWorkCore plugin;
    private final QuestManager questManager;
    private LocalDate lastResetDate;

    public DailyResetTask(DreamWorkCore plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.lastResetDate = LocalDate.now();
    }

    @Override
    public void run() {
        LocalDate today = LocalDate.now();

        // 날짜가 변경되었는지 체크
        if (!today.equals(lastResetDate)) {
            performDailyReset();
            lastResetDate = today;
        }
    }

    /**
     * 일일 리셋을 수행합니다.
     */
    private void performDailyReset() {
        plugin.getLogger().info("[DailyReset] 일일 퀘스트 리셋 시작...");

        // 모든 온라인 플레이어에게 새 퀘스트 할당
        for (Player player : Bukkit.getOnlinePlayers()) {
            questManager.assignDailyQuests(player);
            player.sendMessage("§a[DreamWork] §f새로운 일일 퀘스트가 할당되었습니다!");
        }

        plugin.getLogger().info("[DailyReset] 일일 퀘스트 리셋 완료!");
    }

    /**
     * 자정까지 남은 틱을 계산합니다.
     */
    public static long getTicksUntilMidnight() {
        LocalTime now = LocalTime.now();
        LocalTime midnight = LocalTime.MIDNIGHT;

        long secondsUntilMidnight;
        if (now.isBefore(midnight)) {
            secondsUntilMidnight = now.until(midnight, ChronoUnit.SECONDS);
        } else {
            // 다음 날 자정
            secondsUntilMidnight = (24 * 60 * 60) - now.toSecondOfDay();
        }

        return secondsUntilMidnight * 20; // 초 -> 틱
    }

    /**
     * 태스크를 시작합니다.
     */
    public void start() {
        // 5분마다 날짜 변경 체크
        this.runTaskTimer(plugin, 0L, 6000L);

        plugin.getLogger().info("DailyResetTask 시작됨");
    }
}
