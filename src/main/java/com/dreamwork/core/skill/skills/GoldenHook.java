package com.dreamwork.core.skill.skills;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.skill.SkillEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 낚시꾼 30레벨 스킬: 월척 (Golden Hook)
 * 
 * <p>
 * 1분간 희귀 물고기 낚을 확률이 2배 증가합니다.
 * </p>
 */
public class GoldenHook implements SkillEffect {

    private final DreamWorkCore plugin;

    /** 버프가 활성화된 플레이어 */
    private static final Set<UUID> activeBuffs = ConcurrentHashMap.newKeySet();

    public GoldenHook(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        UUID uuid = player.getUniqueId();

        // 이미 활성화 중인 경우
        if (activeBuffs.contains(uuid)) {
            player.sendMessage("§c[스킬] 이미 월척이 활성화 중입니다!");
            return;
        }

        // 버프 활성화
        activeBuffs.add(uuid);

        // 이펙트
        player.getWorld().spawnParticle(Particle.FISHING, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 0.8f);

        // 행운(LUCK) 효과 부여 (낚시 확률 증가) - 1분
        player.addPotionEffect(
                new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LUCK, 60 * 20, 4, false, true));

        player.sendMessage("§b[스킬] §f월척 발동! 1분간 행운이 따릅니다.");

        // 1분 후 버프 해제
        new BukkitRunnable() {
            @Override
            public void run() {
                activeBuffs.remove(uuid);
                if (player.isOnline()) {
                    player.sendMessage("§7[스킬] 월척 효과가 종료되었습니다.");
                }
            }
        }.runTaskLater(plugin, 60 * 20L); // 60초
    }

    /**
     * 플레이어의 월척 버프 활성화 여부를 확인합니다.
     */
    public static boolean isActive(UUID uuid) {
        return activeBuffs.contains(uuid);
    }

    @Override
    public String getId() {
        return "golden_hook";
    }

    @Override
    public String getName() {
        return "월척";
    }

    @Override
    public String getDescription() {
        return "1분간 희귀 물고기 낚을 확률이 2배 증가합니다.";
    }

    @Override
    public int getCooldown() {
        return 600; // 10분
    }

    @Override
    public int getManaCost() {
        return 40;
    }

    @Override
    public int getRequiredLevel() {
        return 30;
    }

    @Override
    public String getRequiredJob() {
        return "fisher";
    }
}
