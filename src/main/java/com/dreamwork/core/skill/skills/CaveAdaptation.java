package com.dreamwork.core.skill.skills;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.skill.SkillEffect;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 광부 30레벨 스킬: 지하 적응 (Cave Adaptation)
 * 
 * <p>
 * Plan 2.0 기준:
 * - Y좌표 0 이하(Deepslate 층) 또는 네더 월드에서 자동 발동하는 패시브 스킬
 * - 레벨에 따른 효과:
 * - Lv.10+: 낙하 피해 10% 감소
 * - Lv.30+: 낙하 피해 20% 감소 + 성급함 I + 용암 피해 10% 감소
 * - Lv.50+: 낙하 피해 30% 감소 + 성급함 II + 용암에 빠질 시 화염 저항 10초 (쿨타임 5분)
 * </p>
 */
public class CaveAdaptation implements SkillEffect {

    private final DreamWorkCore plugin;

    /** 용암 면역 쿨타임 추적 (UUID -> 만료 시간) */
    private static final Map<UUID, Long> lavaImmunityCooldowns = new ConcurrentHashMap<>();

    /** 지하 적응이 활성화된 플레이어 */
    private static final Map<UUID, Integer> activePlayers = new ConcurrentHashMap<>();

    /** 용암 면역 쿨타임 (밀리초) */
    private static final long LAVA_IMMUNITY_COOLDOWN_MS = 300_000; // 5분

    public CaveAdaptation(DreamWorkCore plugin) {
        this.plugin = plugin;
        startPassiveChecker();
    }

    /**
     * 패시브 효과 체커를 시작합니다.
     * 매 초마다 온라인 플레이어의 위치를 확인하여 효과를 적용합니다.
     */
    private void startPassiveChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    checkAndApplyEffect(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // 매 초마다 체크
    }

    /**
     * 플레이어의 위치를 확인하고 조건에 맞으면 효과를 적용합니다.
     */
    public void checkAndApplyEffect(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();

        // 광부 레벨 확인
        int minerLevel = getMinerLevel(player);
        if (minerLevel < 10) {
            deactivate(player);
            return;
        }

        // 조건 확인: Y좌표 0 이하 또는 네더 월드
        boolean isDeepUnderground = loc.getY() <= 0;
        boolean isNether = loc.getWorld().getEnvironment() == World.Environment.NETHER;

        if (isDeepUnderground || isNether) {
            activate(player, minerLevel);
        } else {
            deactivate(player);
        }
    }

    /**
     * 지하 적응 효과를 활성화합니다.
     */
    private void activate(Player player, int level) {
        UUID uuid = player.getUniqueId();

        // 이미 활성화 중이면 레벨만 업데이트
        Integer currentLevel = activePlayers.get(uuid);
        if (currentLevel != null && currentLevel == level) {
            // 포션 효과 갱신만
            applyPotionEffects(player, level);
            return;
        }

        activePlayers.put(uuid, level);
        applyPotionEffects(player, level);

        // 처음 활성화될 때만 메시지 출력
        if (currentLevel == null) {
            player.sendMessage(ChatColor.GREEN + "[지하 적응] " + ChatColor.WHITE +
                    "지하 환경에 적응하여 생존력이 향상됩니다.");
        }
    }

    /**
     * 지하 적응 효과를 해제합니다.
     */
    private void deactivate(Player player) {
        UUID uuid = player.getUniqueId();

        if (activePlayers.remove(uuid) != null) {
            // 성급함 효과 제거 (다른 소스에서 준 효과는 유지됨)
            player.removePotionEffect(PotionEffectType.HASTE);

            player.sendMessage(ChatColor.GRAY + "[지하 적응] 지하 환경을 벗어났습니다.");
        }
    }

    /**
     * 레벨에 따른 포션 효과를 적용합니다.
     */
    private void applyPotionEffects(Player player, int level) {
        // 성급함 효과 (Lv.30+: I, Lv.50+: II)
        if (level >= 50) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE, 40, 1, false, false, true)); // 2초 지속, 성급함 II
        } else if (level >= 30) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE, 40, 0, false, false, true)); // 2초 지속, 성급함 I
        }
    }

    /**
     * 플레이어가 낙하 피해를 받을 때 호출됩니다.
     * 감소율을 반환합니다.
     * 
     * @param player 플레이어
     * @return 피해 감소율 (0.0 ~ 1.0)
     */
    public static double getFallDamageReduction(Player player) {
        Integer level = activePlayers.get(player.getUniqueId());
        if (level == null)
            return 0.0;

        if (level >= 50)
            return 0.30;
        if (level >= 30)
            return 0.20;
        if (level >= 10)
            return 0.10;
        return 0.0;
    }

    /**
     * 플레이어가 용암 피해를 받을 때 호출됩니다.
     * 감소율을 반환합니다.
     * 
     * @param player 플레이어
     * @return 피해 감소율 (0.0 ~ 1.0)
     */
    public static double getLavaDamageReduction(Player player) {
        Integer level = activePlayers.get(player.getUniqueId());
        if (level == null || level < 30)
            return 0.0;

        return 0.10; // 10% 감소
    }

    /**
     * 플레이어가 용암에 빠졌을 때 화염 저항을 발동합니다 (Lv.50+).
     * 쿨타임이 있으면 발동하지 않습니다.
     * 
     * @param player 플레이어
     * @return 화염 저항이 발동되었으면 true
     */
    public static boolean triggerLavaImmunity(Player player) {
        UUID uuid = player.getUniqueId();
        Integer level = activePlayers.get(uuid);

        if (level == null || level < 50)
            return false;

        // 쿨타임 확인
        Long cooldownEnd = lavaImmunityCooldowns.get(uuid);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            return false;
        }

        // 화염 저항 10초 부여
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE, 200, 0, false, true, true)); // 10초

        // 쿨타임 시작
        lavaImmunityCooldowns.put(uuid, System.currentTimeMillis() + LAVA_IMMUNITY_COOLDOWN_MS);

        // 파티클 및 사운드
        player.getWorld().spawnParticle(
                Particle.FLAME,
                player.getLocation().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.05);

        player.sendMessage(ChatColor.GOLD + "[지하 적응] " + ChatColor.WHITE +
                "위급 상황! 화염 저항이 발동되었습니다. (쿨타임 5분)");

        return true;
    }

    /**
     * 플레이어의 지하 적응 활성화 여부를 확인합니다.
     */
    public static boolean isActive(UUID uuid) {
        return activePlayers.containsKey(uuid);
    }

    /**
     * 플레이어의 광부 레벨을 반환합니다.
     */
    private int getMinerLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"miner".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }

    @Override
    public void execute(Player player) {
        // 패시브 스킬이므로 직접 실행하지 않음
        player.sendMessage(ChatColor.YELLOW + "[지하 적응] 이 스킬은 지하에서 자동으로 발동됩니다.");
    }

    @Override
    public String getId() {
        return "cave_adaptation";
    }

    @Override
    public String getName() {
        return "지하 적응";
    }

    @Override
    public String getDescription() {
        return "깊은 지하(Y≤0)나 네더에서 생존력이 향상됩니다.";
    }

    @Override
    public int getCooldown() {
        return 0; // 패시브
    }

    @Override
    public int getManaCost() {
        return 0; // 패시브
    }

    @Override
    public int getRequiredLevel() {
        return 10; // 10레벨부터 낙하 피해 감소
    }

    @Override
    public String getRequiredJob() {
        return "miner";
    }
}
