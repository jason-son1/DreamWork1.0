package com.dreamwork.core.ui;

import com.dreamwork.core.DreamWorkCore;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ActionBar 메시지 매니저
 * 
 * <p>
 * 우선순위 기반 메시지 큐를 관리합니다.
 * </p>
 */
public class ActionBarManager {

    private final DreamWorkCore plugin;

    /** 플레이어별 메시지 큐 (UUID -> PriorityQueue) */
    private final Map<UUID, PriorityQueue<ActionBarMessage>> messageQueues = new ConcurrentHashMap<>();

    /** 현재 표시 중인 메시지 종료 시간 */
    private final Map<UUID, Long> messageEndTimes = new ConcurrentHashMap<>();

    public ActionBarManager(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 메시지 표시 태스크를 시작합니다.
     */
    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    processPlayer(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // 0.25초마다

        plugin.getLogger().info("ActionBarManager 시작됨");
    }

    private void processPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        PriorityQueue<ActionBarMessage> queue = messageQueues.get(uuid);

        // 현재 메시지가 아직 표시 중인지 확인
        Long endTime = messageEndTimes.get(uuid);
        if (endTime != null && System.currentTimeMillis() < endTime) {
            return; // 아직 표시 중
        }

        if (queue == null || queue.isEmpty()) {
            return;
        }

        // 다음 메시지 표시
        ActionBarMessage msg = queue.poll();
        if (msg != null) {
            player.sendActionBar(Component.text(msg.message));
            messageEndTimes.put(uuid, System.currentTimeMillis() + msg.durationMs);
        }
    }

    /**
     * 메시지를 큐에 추가합니다.
     * 
     * @param player     플레이어
     * @param message    메시지
     * @param priority   우선순위 (높을수록 먼저 표시)
     * @param durationMs 표시 시간 (밀리초)
     */
    public void showMessage(Player player, String message, int priority, long durationMs) {
        UUID uuid = player.getUniqueId();
        PriorityQueue<ActionBarMessage> queue = messageQueues.computeIfAbsent(
                uuid, k -> new PriorityQueue<>(Comparator.comparingInt(m -> -m.priority)));
        queue.add(new ActionBarMessage(message, priority, durationMs));
    }

    /**
     * 즉시 메시지를 표시합니다. (큐 무시)
     */
    public void showImmediate(Player player, String message, long durationMs) {
        player.sendActionBar(Component.text(message));
        messageEndTimes.put(player.getUniqueId(), System.currentTimeMillis() + durationMs);
    }

    /**
     * 경험치 획득 메시지
     */
    public void showExpGain(Player player, double amount, String jobName) {
        String msg = "§a+" + String.format("%.1f", amount) + " EXP §7(" + jobName + ")";
        showMessage(player, msg, 1, 2000);
    }

    /**
     * 스킬 쿨타임 메시지
     */
    public void showSkillCooldown(Player player, String skillName, long remainingSeconds) {
        String msg = "§c[" + skillName + "] §f쿨타임: §e" + remainingSeconds + "초";
        showImmediate(player, msg, 1000);
    }

    /**
     * 마나 상태 표시
     */
    public void showManaStatus(Player player, double current, double max) {
        int percent = (int) ((current / max) * 100);
        String bar = createProgressBar(percent, 10);
        String msg = "§b기력 " + bar + " §f" + (int) current + "/" + (int) max;
        showImmediate(player, msg, 500);
    }

    private String createProgressBar(int percent, int length) {
        int filled = percent * length / 100;
        StringBuilder bar = new StringBuilder("§b");
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("§7░");
            }
        }
        return bar.toString();
    }

    /**
     * ActionBar 메시지 데이터
     */
    private record ActionBarMessage(String message, int priority, long durationMs) {
    }
}
