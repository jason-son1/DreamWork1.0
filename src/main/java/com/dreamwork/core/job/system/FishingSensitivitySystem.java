package com.dreamwork.core.job.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * 예민한 감각 (Sensitivity) 패시브 시스템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 어부 Lv.10 해금
 * - 물고기가 미끼를 무는 대기 시간 감소
 * - Lv.90+ 시 입질 시 사운드 알림
 * </p>
 */
public class FishingSensitivitySystem implements Listener {

    private final DreamWorkCore plugin;

    public FishingSensitivitySystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 낚시 이벤트 처리 - 입질 시 알림 (Lv.90+)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        // 물고기가 미끼를 물었을 때
        if (event.getState() != PlayerFishEvent.State.BITE)
            return;

        int fisherLevel = getFisherLevel(player);
        if (fisherLevel < 90)
            return;

        // Lv.90+ : 입질 시 사운드 알림
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
        player.sendActionBar(
                net.kyori.adventure.text.Component.text("§b§l⚡ 입질! ⚡"));
    }

    /**
     * 플레이어의 어부 레벨에 따른 대기 시간 감소 비율을 반환합니다.
     * (실제 대기 시간 감소는 Bukkit/Paper에서 직접 지원하지 않으므로,
     * 별도의 커스텀 낚시 시스템이나 Lure 인챈트 수준 조절 필요)
     */
    public double getWaitReductionPercent(Player player) {
        int level = getFisherLevel(player);

        if (level < 10)
            return 0;
        if (level >= 90)
            return 0.50;
        if (level >= 70)
            return 0.45;
        if (level >= 50)
            return 0.35;
        if (level >= 30)
            return 0.25;
        return 0.15;
    }

    /**
     * 플레이어의 어부 레벨을 반환합니다.
     */
    private int getFisherLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"fisher".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }
}
