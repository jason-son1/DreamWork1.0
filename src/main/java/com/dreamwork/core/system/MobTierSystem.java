package com.dreamwork.core.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 몬스터 티어 시스템
 * <p>
 * 몬스터에 등급(일반/고급/희귀/엘리트/보스)을 부여하고
 * 등급에 따라 강화된 스탯, 드롭, 경험치를 제공합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class MobTierSystem implements Listener {

    private final DreamWorkCore plugin;
    private static final String TIER_METADATA_KEY = "dreamwork_mob_tier";

    /** 티어별 출현 확률 */
    private static final double UNCOMMON_CHANCE = 0.15;
    private static final double RARE_CHANCE = 0.05;
    private static final double ELITE_CHANCE = 0.01;

    public MobTierSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster monster))
            return;

        // 자연 스폰만 대상
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL &&
                event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }

        // 티어 결정
        MobTier tier = determineTier();
        if (tier == MobTier.COMMON)
            return; // 일반은 변경 없음

        // 티어 적용
        applyTier(monster, tier);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster monster))
            return;

        if (!monster.hasMetadata(TIER_METADATA_KEY))
            return;

        MobTier tier = MobTier.valueOf(monster.getMetadata(TIER_METADATA_KEY).get(0).asString());
        Player killer = monster.getKiller();

        if (killer == null)
            return;

        // 드롭 증가
        double dropMultiplier = tier.dropMultiplier;
        event.getDrops().forEach(item -> {
            if (ThreadLocalRandom.current().nextDouble() < (dropMultiplier - 1)) {
                item.setAmount(item.getAmount() + 1);
            }
        });

        // 경험치 증가
        event.setDroppedExp((int) (event.getDroppedExp() * tier.expMultiplier));

        // 사냥꾼 경험치 보너스
        double bonusExp = tier.ordinal() * 10;
        plugin.getJobManager().addExp(killer, JobType.HUNTER, bonusExp);

        // 알림
        if (tier.ordinal() >= MobTier.RARE.ordinal()) {
            killer.sendMessage("§c[사냥꾼] §f" + tier.color + tier.displayName + " §f" +
                    getMonsterName(monster.getType()) + "§f을(를) 처치했습니다! §7(+" + (int) bonusExp + " XP)");
        }
    }

    /**
     * 티어를 결정합니다.
     */
    private MobTier determineTier() {
        double rand = ThreadLocalRandom.current().nextDouble();

        if (rand < ELITE_CHANCE)
            return MobTier.ELITE;
        if (rand < RARE_CHANCE)
            return MobTier.RARE;
        if (rand < UNCOMMON_CHANCE)
            return MobTier.UNCOMMON;
        return MobTier.COMMON;
    }

    /**
     * 몬스터에 티어를 적용합니다.
     */
    private void applyTier(Monster monster, MobTier tier) {
        // 메타데이터 설정
        monster.setMetadata(TIER_METADATA_KEY, new FixedMetadataValue(plugin, tier.name()));

        // 이름 설정
        String name = tier.color + "[" + tier.displayName + "] " + getMonsterName(monster.getType());
        monster.customName(net.kyori.adventure.text.Component.text(name));
        monster.setCustomNameVisible(true);

        // 체력 강화
        AttributeInstance maxHealth = monster.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            double newHealth = maxHealth.getValue() * tier.healthMultiplier;
            maxHealth.setBaseValue(newHealth);
            monster.setHealth(newHealth);
        }

        // 공격력 강화
        AttributeInstance attackDamage = monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(attackDamage.getValue() * tier.damageMultiplier);
        }

        // 이동속도 증가
        AttributeInstance movementSpeed = monster.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(movementSpeed.getValue() * tier.speedMultiplier);
        }

        // 파티클 효과 (엘리트 이상)
        if (tier.ordinal() >= MobTier.ELITE.ordinal()) {
            org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                if (monster.isDead() || !monster.isValid()) {
                    task.cancel();
                    return;
                }
                monster.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT,
                        monster.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.1);
            }, 0L, 10L);
        }
    }

    /**
     * 몬스터의 티어를 반환합니다.
     */
    public MobTier getTier(LivingEntity entity) {
        if (!entity.hasMetadata(TIER_METADATA_KEY))
            return MobTier.COMMON;
        return MobTier.valueOf(entity.getMetadata(TIER_METADATA_KEY).get(0).asString());
    }

    private String getMonsterName(EntityType type) {
        return switch (type) {
            case ZOMBIE -> "좀비";
            case SKELETON -> "스켈레톤";
            case CREEPER -> "크리퍼";
            case SPIDER -> "거미";
            case ENDERMAN -> "엔더맨";
            case WITCH -> "마녀";
            case PILLAGER -> "약탈자";
            case VINDICATOR -> "변명자";
            case EVOKER -> "소환사";
            case RAVAGER -> "파괴수";
            case WARDEN -> "워든";
            case WITHER_SKELETON -> "위더 스켈레톤";
            case BLAZE -> "블레이즈";
            case GHAST -> "가스트";
            default -> type.name();
        };
    }

    /**
     * 몬스터 티어 정의
     */
    public enum MobTier {
        COMMON("일반", "§f", 1.0, 1.0, 1.0, 1.0, 1.0),
        UNCOMMON("고급", "§a", 1.5, 1.2, 1.1, 1.5, 1.3),
        RARE("희귀", "§b", 2.5, 1.5, 1.2, 2.0, 2.0),
        ELITE("엘리트", "§6", 5.0, 2.0, 1.3, 3.0, 4.0),
        BOSS("보스", "§c§l", 10.0, 3.0, 1.0, 5.0, 10.0);

        public final String displayName;
        public final String color;
        public final double healthMultiplier;
        public final double damageMultiplier;
        public final double speedMultiplier;
        public final double dropMultiplier;
        public final double expMultiplier;

        MobTier(String displayName, String color, double healthMult, double damageMult,
                double speedMult, double dropMult, double expMult) {
            this.displayName = displayName;
            this.color = color;
            this.healthMultiplier = healthMult;
            this.damageMultiplier = damageMult;
            this.speedMultiplier = speedMult;
            this.dropMultiplier = dropMult;
            this.expMultiplier = expMult;
        }
    }
}
