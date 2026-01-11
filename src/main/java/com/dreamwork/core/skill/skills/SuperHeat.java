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
 * 광부 30레벨 스킬: 슈퍼 히트
 * 
 * <p>
 * 30초간 채광한 광물을 즉시 제련된 상태로 획득합니다.
 * </p>
 */
public class SuperHeat implements SkillEffect {

    private final DreamWorkCore plugin;

    /** 버프가 활성화된 플레이어 */
    private static final Set<UUID> activeBuffs = ConcurrentHashMap.newKeySet();

    public SuperHeat(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        UUID uuid = player.getUniqueId();

        // 이미 활성화 중인 경우
        if (activeBuffs.contains(uuid)) {
            player.sendMessage("§c[스킬] 이미 슈퍼 히트가 활성화 중입니다!");
            return;
        }

        // 버프 활성화
        activeBuffs.add(uuid);

        // 이펙트
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);

        player.sendMessage("§6[스킬] §f슈퍼 히트 발동! 30초간 광물이 자동 제련됩니다.");

        // 30초 후 버프 해제
        new BukkitRunnable() {
            @Override
            public void run() {
                activeBuffs.remove(uuid);
                if (player.isOnline()) {
                    player.sendMessage("§7[스킬] 슈퍼 히트 효과가 종료되었습니다.");
                }
            }
        }.runTaskLater(plugin, 30 * 20L); // 30초
    }

    /**
     * 플레이어의 슈퍼 히트 버프 활성화 여부를 확인합니다.
     */
    public static boolean isActive(UUID uuid) {
        return activeBuffs.contains(uuid);
    }

    @Override
    public String getId() {
        return "superheat";
    }

    @Override
    public String getName() {
        return "슈퍼 히트";
    }

    @Override
    public String getDescription() {
        return "30초간 채광한 광물을 즉시 제련된 상태로 획득합니다.";
    }

    @Override
    public int getCooldown() {
        return 300; // 5분
    }

    @Override
    public int getManaCost() {
        return 30;
    }

    @Override
    public int getRequiredLevel() {
        return 30;
    }

    @Override
    public String getRequiredJob() {
        return "miner";
    }
}
