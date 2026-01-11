package com.dreamwork.core.database;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.model.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dirty-Check 기반 자동 저장 스케줄러
 * 
 * <p>
 * 주기적으로 변경된 데이터만 저장합니다.
 * </p>
 */
public class AutoSaveScheduler extends BukkitRunnable {

    private final DreamWorkCore plugin;
    private final StorageManager storageManager;

    /** 저장 주기 (틱 단위) */
    private final long interval;

    public AutoSaveScheduler(DreamWorkCore plugin, long intervalTicks) {
        this.plugin = plugin;
        this.storageManager = plugin.getStorageManager();
        this.interval = intervalTicks;
    }

    /**
     * 스케줄러를 시작합니다.
     */
    public void start() {
        this.runTaskTimer(plugin, interval, interval); // runTaskTimer for sync iteration, saveAsync for async IO
        plugin.getLogger().info("AutoSaveScheduler 시작됨 (주기: " + (interval / 20) + "초)");
    }

    @Override
    public void run() {
        AtomicInteger savedCount = new AtomicInteger(0);

        // Iterate online players (Sync safety)
        for (Player player : Bukkit.getOnlinePlayers()) {
            UserData data = storageManager.getUserData(player.getUniqueId());

            if (data != null && data.isDirty()) {
                storageManager.saveUserAsync(data); // This is async internally
                savedCount.incrementAndGet();
            }
        }

        if (plugin.isDebugMode() && savedCount.get() > 0) {
            plugin.getLogger().info("[AutoSave] " + savedCount.get() + "명 데이터 저장됨");
        }
    }
}
