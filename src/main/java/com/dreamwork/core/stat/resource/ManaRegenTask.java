package com.dreamwork.core.stat.resource;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.database.StorageManager;
import com.dreamwork.core.model.UserData;
import com.dreamwork.core.stat.StatManager;
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
            if (data == null)
                continue;

            double current = data.getCurrentMana();

            // Use StatManager to calculate derived stats
            StatManager statManager = plugin.getStatManager();
            if (statManager == null)
                continue;

            double max = statManager.calculateMaxMana(player);
            double regen = statManager.calculateManaRegen(player);

            if (current < max) {
                double newMana = Math.min(current + regen, max);
                data.setCurrentMana(newMana);
            }
        }
    }
}
