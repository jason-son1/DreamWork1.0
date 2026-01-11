package com.dreamwork.core.stat.mechanic;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.stat.StatManager.PlayerStats;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * 전투 메카닉 계산기
 * 
 * <p>
 * STR, DEX, CON 등 스탯을 전투에 적용합니다.
 * </p>
 * 
 * <h2>공식:</h2>
 * <ul>
 * <li>공격력: baseDamage + (STR × strMultiplier)</li>
 * <li>치명타: (DEX × 0.2 + LUCK × 0.3)%</li>
 * <li>방어력: damage × (100 / (100 + DEF))</li>
 * </ul>
 */
public class CombatMechanic {

    private final DreamWorkCore plugin;
    private final StatManager statManager;
    private final Random random = new Random();

    // 설정값
    private double strMultiplier = 0.5;
    private double critDexFactor = 0.2;
    private double critLuckFactor = 0.3;
    private double critDamageMultiplier = 1.5;
    private double maxCritChance = 75.0;

    public CombatMechanic(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.statManager = plugin.getStatManager();
        loadConfig();
    }

    public void loadConfig() {
        this.strMultiplier = plugin.getConfig().getDouble("combat.str-multiplier", 0.5);
        this.critDexFactor = plugin.getConfig().getDouble("combat.crit-dex-factor", 0.2);
        this.critLuckFactor = plugin.getConfig().getDouble("combat.crit-luck-factor", 0.3);
        this.critDamageMultiplier = plugin.getConfig().getDouble("combat.crit-damage-multiplier", 1.5);
        this.maxCritChance = plugin.getConfig().getDouble("combat.max-crit-chance", 75.0);
    }

    /**
     * 최종 데미지를 계산합니다.
     * 
     * @param attacker   공격자
     * @param baseDamage 기본 데미지
     * @return 계산된 최종 데미지
     */
    public double calculateDamage(Player attacker, double baseDamage) {
        PlayerStats stats = statManager.getStats(attacker);
        int str = stats.getStr();

        double bonusDamage = str * strMultiplier;
        return baseDamage + bonusDamage;
    }

    /**
     * 치명타 발동 여부를 확인합니다.
     * 
     * @param attacker 공격자
     * @return 치명타 발동 여부
     */
    public boolean checkCritical(Player attacker) {
        PlayerStats stats = statManager.getStats(attacker);

        double critChance = (stats.getDex() * critDexFactor) + (stats.getLuck() * critLuckFactor);
        critChance = Math.min(critChance, maxCritChance);

        return random.nextDouble() * 100 < critChance;
    }

    /**
     * 치명타 데미지 배율을 반환합니다.
     */
    public double getCritDamageMultiplier() {
        return critDamageMultiplier;
    }

    /**
     * 치명타 이펙트를 재생합니다.
     */
    public void playCriticalEffect(Player attacker, org.bukkit.entity.Entity target) {
        // 파티클
        target.getWorld().spawnParticle(
                Particle.CRIT,
                target.getLocation().add(0, 1, 0),
                20, 0.5, 0.5, 0.5, 0.1);

        // 사운드
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
    }

    /**
     * 방어력을 적용하여 데미지를 감소시킵니다.
     * 
     * @param victim 피해자
     * @param damage 원본 데미지
     * @return 방어력 적용 후 데미지
     */
    public double applyDefense(Player victim, double damage) {
        PlayerStats stats = statManager.getStats(victim);
        int con = stats.getCon();

        // 방어력 공식: damage × (100 / (100 + DEF))
        // DEF = CON × 2 (간단한 변환)
        double defense = con * 2;
        double reduction = 100.0 / (100.0 + defense);

        double finalDamage = damage * reduction;

        // [광부] 단단한 피부 (Passive): 받는 물리 데미지 5% 감소
        if (plugin.getSkillManager().hasSkill(victim, "tough_skin")) {
            finalDamage *= 0.95; // 5% 감소
        }

        return finalDamage;
    }

    /**
     * 치명타 확률을 계산합니다. (표시용)
     */
    public double getCriticalChance(Player player) {
        PlayerStats stats = statManager.getStats(player);
        double critChance = (stats.getDex() * critDexFactor) + (stats.getLuck() * critLuckFactor);
        return Math.min(critChance, maxCritChance);
    }

    /**
     * 방어력 수치를 계산합니다. (표시용)
     */
    public double getDefenseValue(Player player) {
        PlayerStats stats = statManager.getStats(player);
        return stats.getCon() * 2;
    }
}
