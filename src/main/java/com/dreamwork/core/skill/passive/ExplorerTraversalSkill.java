package com.dreamwork.core.skill.passive;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * 탐험가 패시브 스킬: 가벼운 발걸음 (Light Step)
 * <p>
 * 이동 속도가 증가하고 낙하 데미지가 감소합니다.
 * 레벨에 따라 효과가 강화됩니다.
 * </p>
 * <p>
 * 레벨별 효과:
 * - Lv.1+: 이동속도 +5%, 낙하 데미지 -10%
 * - Lv.25+: 이동속도 +10%, 낙하 데미지 -25%
 * - Lv.50+: 이동속도 +15%, 낙하 데미지 -50%
 * - Lv.75+: 이동속도 +20%, 낙하 데미지 -75%
 * - Lv.100: 이동속도 +25%, 낙하 데미지 면역
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class ExplorerTraversalSkill implements Listener {

    private final DreamWorkCore plugin;
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("e7a8b9c0-1234-5678-abcd-ef0123456789");
    private static final String SPEED_MODIFIER_NAME = "dreamwork.explorer.speed";

    public ExplorerTraversalSkill(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 접속 시 이동속도 보너스 적용
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updateSpeedBonus(event.getPlayer());
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 퇴장 시 속도 보너스 제거
        removeSpeedBonus(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 플레이어 탐험가 레벨 확인
        UserData userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null) {
            return;
        }

        int explorerLevel = userData.getJobLevel(JobType.EXPLORER);
        double reduction = getFallDamageReduction(explorerLevel);

        if (reduction >= 1.0) {
            // 완전 면역
            event.setCancelled(true);
            player.sendMessage("§a[탐험가] §f낙법!");
        } else if (reduction > 0) {
            // 데미지 감소
            double originalDamage = event.getDamage();
            double reducedDamage = originalDamage * (1.0 - reduction);
            event.setDamage(reducedDamage);
        }
    }

    /**
     * 플레이어의 이동속도 보너스를 업데이트합니다.
     */
    public void updateSpeedBonus(Player player) {
        UserData userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null) {
            return;
        }

        int explorerLevel = userData.getJobLevel(JobType.EXPLORER);
        double speedBonus = getSpeedBonus(explorerLevel);

        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr == null)
            return;

        // 기존 보너스 제거
        speedAttr.getModifiers().stream()
                .filter(m -> m.getName().equals(SPEED_MODIFIER_NAME))
                .forEach(speedAttr::removeModifier);

        // 새 보너스 적용
        if (speedBonus > 0) {
            AttributeModifier modifier = new AttributeModifier(
                    SPEED_MODIFIER_UUID,
                    SPEED_MODIFIER_NAME,
                    speedBonus,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            speedAttr.addModifier(modifier);
        }
    }

    /**
     * 플레이어의 이동속도 보너스를 제거합니다.
     */
    private void removeSpeedBonus(Player player) {
        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr == null)
            return;

        speedAttr.getModifiers().stream()
                .filter(m -> m.getName().equals(SPEED_MODIFIER_NAME))
                .forEach(speedAttr::removeModifier);
    }

    /**
     * 레벨에 따른 이동속도 보너스 반환 (0.05 = 5%)
     */
    private double getSpeedBonus(int level) {
        if (level >= 100)
            return 0.25;
        if (level >= 75)
            return 0.20;
        if (level >= 50)
            return 0.15;
        if (level >= 25)
            return 0.10;
        if (level >= 1)
            return 0.05;
        return 0;
    }

    /**
     * 레벨에 따른 낙하 데미지 감소율 반환 (0.5 = 50%)
     */
    private double getFallDamageReduction(int level) {
        if (level >= 100)
            return 1.0; // 면역
        if (level >= 75)
            return 0.75;
        if (level >= 50)
            return 0.50;
        if (level >= 25)
            return 0.25;
        if (level >= 1)
            return 0.10;
        return 0;
    }
}
