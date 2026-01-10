package com.dreamwork.core.listener;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.stat.mechanic.CombatMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 전투 데미지 계산 리스너 (CombatMechanic 통합 버전)
 * 
 * <p>
 * STR, DEX, LUCK, CON 스탯을 전투에 적용합니다.
 * </p>
 */
public class CombatListener implements Listener {

    private final DreamWorkCore plugin;
    private final CombatMechanic combatMechanic;

    public CombatListener(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.combatMechanic = new CombatMechanic(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player attacker = getAttacker(event);
        double damage = event.getDamage();
        boolean isCritical = false;

        // 공격자가 플레이어인 경우 - 데미지 증가 및 치명타
        if (attacker != null) {
            damage = combatMechanic.calculateDamage(attacker, damage);

            // 치명타 체크
            if (combatMechanic.checkCritical(attacker)) {
                damage *= combatMechanic.getCritDamageMultiplier();
                isCritical = true;

                // 치명타 이펙트
                combatMechanic.playCriticalEffect(attacker, event.getEntity());
            }

            if (plugin.isDebugMode()) {
                String critStr = isCritical ? " §c(CRITICAL!)" : "";
                plugin.getLogger().info("[Combat] " + attacker.getName() +
                        " -> " + event.getEntity().getName() +
                        " DMG: " + String.format("%.1f", damage) + critStr);
            }
        }

        // 피해자가 플레이어인 경우 - 방어력 적용
        if (event.getEntity() instanceof Player victim) {
            damage = combatMechanic.applyDefense(victim, damage);

            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Combat] " + victim.getName() +
                        " DEF Applied -> Final DMG: " + String.format("%.1f", damage));
            }
        }

        event.setDamage(damage);
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

    /**
     * CombatMechanic 인스턴스를 반환합니다.
     */
    public CombatMechanic getCombatMechanic() {
        return combatMechanic;
    }
}
