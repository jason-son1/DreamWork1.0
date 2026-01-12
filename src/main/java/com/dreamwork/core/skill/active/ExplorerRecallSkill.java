package com.dreamwork.core.skill.active;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 탐험가 액티브 스킬: 귀환 (Recall)
 * <p>
 * 지정된 위치(홈/타운)로 순간이동합니다.
 * 시전 시간이 있으며, 피격 시 취소됩니다.
 * </p>
 * <p>
 * 레벨별 효과:
 * - Lv.25+: 5초 시전, 타운 귀환
 * - Lv.50+: 4초 시전, 타운/홈 선택
 * - Lv.75+: 3초 시전
 * - Lv.100: 2초 시전, 쿨다운 50% 감소
 * </p>
 * <p>
 * 쿨다운: 600초 (10분)
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class ExplorerRecallSkill {

    private final DreamWorkCore plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> casting = new HashSet<>();

    private static final long BASE_COOLDOWN_MS = 600_000; // 10분

    public ExplorerRecallSkill(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 귀환 스킬을 시작합니다.
     */
    public boolean activate(Player player, String destination) {
        UUID uuid = player.getUniqueId();

        // 이미 시전 중인지 확인
        if (casting.contains(uuid)) {
            player.sendMessage("§c[탐험가] 이미 귀환을 시전 중입니다.");
            return false;
        }

        // 쿨다운 확인
        UserData userData = plugin.getStorageManager().getUserData(uuid);
        if (userData == null)
            return false;

        int explorerLevel = userData.getJobLevel(JobType.EXPLORER);
        long cooldown = explorerLevel >= 100 ? BASE_COOLDOWN_MS / 2 : BASE_COOLDOWN_MS;

        if (cooldowns.containsKey(uuid)) {
            long remaining = (cooldowns.get(uuid) + cooldown - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                player.sendMessage("§c[탐험가] 귀환 쿨다운: §e" + remaining + "초");
                return false;
            }
        }

        // 레벨 확인
        if (explorerLevel < 25) {
            player.sendMessage("§c[탐험가] 귀환은 레벨 25 이상에서 사용 가능합니다.");
            return false;
        }

        // 목적지 결정
        Location target = getDestination(player, destination, explorerLevel);
        if (target == null) {
            player.sendMessage("§c[탐험가] 귀환 목적지를 찾을 수 없습니다.");
            return false;
        }

        // 시전 시작
        casting.add(uuid);
        int castTime = getCastTime(explorerLevel);

        player.sendMessage("§a[탐험가] §e" + castTime + "초 §f후 귀환합니다. 움직이면 취소됩니다.");
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.5f);

        Location startLoc = player.getLocation().clone();

        // 시전 카운트다운
        final int[] countdown = { castTime };
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!casting.contains(uuid))
                return;

            // 이동 감지
            if (player.getLocation().distanceSquared(startLoc) > 1.0) {
                casting.remove(uuid);
                player.sendMessage("§c[탐험가] 귀환이 취소되었습니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            countdown[0]--;

            // 파티클 효과
            player.getWorld().spawnParticle(Particle.PORTAL,
                    player.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.1);

            if (countdown[0] <= 0) {
                // 텔레포트
                casting.remove(uuid);
                cooldowns.put(uuid, System.currentTimeMillis());

                player.teleport(target);
                player.sendMessage("§a[탐험가] 귀환 완료!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
            }
        }, 20L, 20L).getTaskId();

        // 시전 시간 초과 시 태스크 취소
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getScheduler().cancelTask(taskId);
            casting.remove(uuid);
        }, (castTime + 2) * 20L);

        return true;
    }

    /**
     * 귀환 목적지를 결정합니다.
     */
    private Location getDestination(Player player, String destination, int level) {
        // 홈 귀환 (Lv.50+)
        if (destination.equalsIgnoreCase("home") && level >= 50) {
            return player.getBedSpawnLocation();
        }

        // 타운 귀환 (Towny 연동)
        if (destination.equalsIgnoreCase("town") || destination.isEmpty()) {
            // Towny 스폰 시도
            if (plugin.getHookManager().isTownyEnabled()) {
                String townName = plugin.getHookManager().getTownyHook().getPlayerTown(player);
                if (townName != null) {
                    // Towny 타운 스폰 위치 (리플렉션으로 가져올 수 있음)
                    // 현재는 월드 스폰으로 대체
                    return player.getWorld().getSpawnLocation();
                }
            }

            // 월드 스폰
            return player.getWorld().getSpawnLocation();
        }

        return null;
    }

    private int getCastTime(int level) {
        if (level >= 100)
            return 2;
        if (level >= 75)
            return 3;
        if (level >= 50)
            return 4;
        return 5;
    }

    /**
     * 피격 시 귀환 취소
     */
    public void onPlayerDamage(Player player) {
        if (casting.remove(player.getUniqueId())) {
            player.sendMessage("§c[탐험가] 피격으로 귀환이 취소되었습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}
