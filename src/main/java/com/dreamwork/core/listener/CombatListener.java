package com.dreamwork.core.listener;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.stat.StatManager.PlayerStats;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 전투 데미지 계산 리스너
 * 
 * <p>
 * STR 스탯을 기반으로 물리 데미지를 증가시킵니다.
 * </p>
 * 
 * <h2>공식:</h2>
 * {@code finalDamage = baseDamage + (STR * strengthMultiplier)}
 */
public class CombatListener implements Listener {

    private final DreamWorkCore plugin;
    private final StatManager statManager;

    // 설정 값
    private double strengthMultiplier;

    public CombatListener(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.statManager = plugin.getStatManager();
        loadConfig();
    }

    /**
     * 설정을 로드합니다.
     */
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.strengthMultiplier = config.getDouble("combat.strength-multiplier", 0.5);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player attacker = getAttacker(event);
        if (attacker == null)
            return;

        PlayerStats stats = statManager.getStats(attacker);
        int str = stats.getStr();

        // 데미지 증가 공식
        double bonusDamage = str * strengthMultiplier;

        if (bonusDamage > 0) {
            double newDamage = event.getDamage() + bonusDamage;
            event.setDamage(newDamage);

            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Combat] " + attacker.getName() +
                        " STR Bonus: +" + String.format("%.1f", bonusDamage) +
                        " (Total: " + String.format("%.1f", newDamage) + ")");
            }
        }
    }

    /**
     * 이벤트에서 공격자(플레이어)를 추출합니다.
     */
    private Player getAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
