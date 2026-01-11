package com.dreamwork.core.skill.skills;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.skill.SkillEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 사냥꾼 30레벨 스킬: 아드레날린
 * 
 * <p>
 * 10초간 이동 속도와 공격 속도가 대폭 증가합니다.
 * </p>
 */
public class Adrenaline implements SkillEffect {

    private final DreamWorkCore plugin;

    public Adrenaline(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        // 이펙트
        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3,
                0);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);

        // 버프 부여 (10초)
        int duration = 10 * 20; // 10초 = 200틱

        // 이동 속도 증가 (Speed II)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1, false, true));

        // 공격 속도 증가 (Haste II)
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, 1, false, true));

        // 힘 증가 (Strength I)
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 0, false, true));

        player.sendMessage("§c[스킬] §f아드레날린 발동! 10초간 신체 능력이 강화됩니다!");
    }

    @Override
    public String getId() {
        return "adrenaline";
    }

    @Override
    public String getName() {
        return "아드레날린";
    }

    @Override
    public String getDescription() {
        return "10초간 이동 속도, 공격 속도, 공격력이 증가합니다.";
    }

    @Override
    public int getCooldown() {
        return 180; // 3분
    }

    @Override
    public int getManaCost() {
        return 35;
    }

    @Override
    public int getRequiredLevel() {
        return 30;
    }

    @Override
    public String getRequiredJob() {
        return "hunter";
    }
}
