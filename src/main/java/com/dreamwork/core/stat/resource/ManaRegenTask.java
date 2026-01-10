package com.dreamwork.core.stat.resource;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.storage.StorageManager;
import com.dreamwork.core.storage.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 마나 재생 태스크
 * 
 * <p>
 * 매 초 플레이어의 마나를 재생합니다.
 * </p>
 */
public class ManaRegenTask extends BukkitRunnable {

    private final DreamWorkCore plugin;
    private final StorageManager storageManager;

    public ManaRegenTask(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.storageManager = plugin.getStorageManager();
    }

    /**
     * 태스크를 시작합니다. (1초마다 실행)
     */
    public void start() {
        this.runTaskTimer(plugin, 20L, 20L);
        plugin.getLogger().info("ManaRegenTask 시작됨");
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UserData data = storageManager.getUserData(player.getUniqueId());

            double current = data.getCurrentMana();
            double max = data.getMaxMana();
            double regen = data.getManaRegen();

            if (current < max) {
                double newMana = Math.min(current + regen, max);
                data.setCurrentMana(newMana);
            }
        }
    }
}
