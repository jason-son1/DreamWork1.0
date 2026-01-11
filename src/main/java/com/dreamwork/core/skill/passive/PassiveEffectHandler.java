package com.dreamwork.core.skill.passive;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.stat.StatManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 패시브 효과 핸들러
 * 
 * <p>
 * AttributeModifier를 사용하여 패시브 효과를 적용합니다.
 * </p>
 */
public class PassiveEffectHandler {

    private final DreamWorkCore plugin;

    // 고정 UUID (같은 효과는 같은 UUID 사용)
    private static final UUID SPEED_UUID = UUID.fromString("8c7f5a7e-1b2c-4d3e-9f0a-1b2c3d4e5f6a");
    private static final UUID ARMOR_UUID = UUID.fromString("8c7f5a7e-1b2c-4d3e-9f0a-1b2c3d4e5f6b");
    private static final UUID MINING_UUID = UUID.fromString("8c7f5a7e-1b2c-4d3e-9f0a-1b2c3d4e5f6c");
    private static final UUID ATTACK_UUID = UUID.fromString("8c7f5a7e-1b2c-4d3e-9f0a-1b2c3d4e5f6d");

    public PassiveEffectHandler(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 스탯 기반 패시브 효과를 적용합니다.
     */
    public void applyPassiveEffects(Player player) {
        StatManager.PlayerStats stats = plugin.getStatManager().getStats(player);

        // DEX -> 이동 속도 증가
        applySpeedBonus(player, stats.getDex());

        // CON -> 방어력 증가
        applyArmorBonus(player, stats.getCon());

        // STR -> 공격력 증가
        applyAttackBonus(player, stats.getStr());

        // 직업별 패시브 효과 적용
        applyJobPassives(player);
    }

    /**
     * 직업별 패시브 효과를 적용합니다.
     * 
     * <ul>
     * <li>광부 10레벨: 단단한 피부 - 받는 물리 데미지 5% 감소</li>
     * <li>낚시꾼 10레벨: 인내심 - 낚시 대기시간 감소 (Luck 스탯 증가)</li>
     * <li>사냥꾼 10레벨: 약점 포착 - 치명타 확률 +5%</li>
     * </ul>
     */
    private void applyJobPassives(Player player) {
        JobManager jobManager = plugin.getJobManager();
        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());

        if (!jobData.hasJob()) {
            return;
        }

        String jobId = jobData.getJobId();
        int level = jobData.getLevel();

        // 광부 패시브: 10레벨 이상 - 방어력 보너스 (이미 CON 기반으로 적용하므로 추가 없음)
        // 실제 데미지 감소는 CombatListener에서 처리

        // 사냥꾼 패시브: 10레벨 이상 - 치명타 확률 보너스
        // 실제 치명타 처리는 StatManager에서 추가 계산
    }

    /**
     * 이동 속도 보너스 적용 (DEX 기반)
     */
    public void applySpeedBonus(Player player, int dex) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (attr == null)
            return;

        // 기존 모디파이어 제거
        removeModifier(attr, SPEED_UUID);

        // DEX당 0.001 속도 증가 (100 DEX = 10% 속도 증가)
        double speedBonus = dex * 0.001;
        if (speedBonus > 0) {
            attr.addModifier(new AttributeModifier(
                    SPEED_UUID,
                    "DreamWork_Speed",
                    speedBonus,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    /**
     * 방어력 보너스 적용 (CON 기반)
     */
    public void applyArmorBonus(Player player, int con) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_ARMOR);
        if (attr == null)
            return;

        removeModifier(attr, ARMOR_UUID);

        // CON당 0.2 방어력 증가
        double armorBonus = con * 0.2;
        if (armorBonus > 0) {
            attr.addModifier(new AttributeModifier(
                    ARMOR_UUID,
                    "DreamWork_Armor",
                    armorBonus,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    /**
     * 공격력 보너스 적용 (STR 기반)
     */
    public void applyAttackBonus(Player player, int str) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attr == null)
            return;

        removeModifier(attr, ATTACK_UUID);

        // STR당 0.1 공격력 증가
        double attackBonus = str * 0.1;
        if (attackBonus > 0) {
            attr.addModifier(new AttributeModifier(
                    ATTACK_UUID,
                    "DreamWork_Attack",
                    attackBonus,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    /**
     * 모든 패시브 효과를 제거합니다.
     */
    public void clearPassiveEffects(Player player) {
        AttributeInstance speed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        AttributeInstance armor = player.getAttribute(Attribute.GENERIC_ARMOR);
        AttributeInstance attack = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);

        if (speed != null)
            removeModifier(speed, SPEED_UUID);
        if (armor != null)
            removeModifier(armor, ARMOR_UUID);
        if (attack != null)
            removeModifier(attack, ATTACK_UUID);
    }

    private void removeModifier(AttributeInstance attr, UUID uuid) {
        for (AttributeModifier mod : attr.getModifiers()) {
            if (mod.getUniqueId().equals(uuid)) {
                attr.removeModifier(mod);
                break;
            }
        }
    }
}
