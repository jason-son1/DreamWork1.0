package com.dreamwork.core.job.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.job.JobType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 몬스터 등급 및 도감 시스템 (Mob Tier & Slayer Knowledge)
 * 
 * <p>
 * 1. 몬스터 스폰 시 일정 확률로 '엘리트(Alpha)' 등급을 부여하여 강화합니다.
 * 2. 몬스터 처치 시 사냥꾼의 '도감(Kill Count)'을 업데이트하고 보너스를 지급합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class MobTierSystem implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey TIER_KEY;
    private final NamespacedKey KILL_COUNT_KEY; // UserData에 저장하겠지만, 여기선 로직용

    public MobTierSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.TIER_KEY = new NamespacedKey(plugin, "mob_tier");
        this.KILL_COUNT_KEY = new NamespacedKey(plugin, "slayer_kills");
    }

    // 1. 몬스터 스폰 시 등급 부여
    @EventHandler(priority = EventPriority.HIGH)
    public void onMobSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Monster monster))
            return;

        // 이미 등급이 부여된 경우 패스 (청크 언로드 후 로드 등)
        PersistentDataContainer pdc = monster.getPersistentDataContainer();
        if (pdc.has(TIER_KEY, PersistentDataType.STRING))
            return;

        // 5% 확률로 엘리트(Alpha) 몬스터 생성
        if (ThreadLocalRandom.current().nextDouble() < 0.05) {
            makeElite(monster);
        } else {
            // 일반(Common) 태그
            pdc.set(TIER_KEY, PersistentDataType.STRING, "COMMON");
        }
    }

    private void makeElite(Monster monster) {
        PersistentDataContainer pdc = monster.getPersistentDataContainer();
        pdc.set(TIER_KEY, PersistentDataType.STRING, "ELITE");

        // 이름 변경
        monster.customName(Component.text("§c[Alpha] §f" + monster.getName()));
        monster.setCustomNameVisible(true);

        // 스탯 강화 (체력 2배, 공격력 1.5배)
        var maxHealth = monster.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() * 2.0);
            monster.setHealth(maxHealth.getValue());
        }

        var attackDamage = monster.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(attackDamage.getBaseValue() * 1.5);
        }

        // 시각 효과 (파티클)
        // 지속적인 파티클은 Runnable Task가 필요하므로 여기선 생략하거나 간단히 스폰 시 한 번
        monster.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, monster.getLocation().add(0, 1, 0), 10);
    }

    // 2. 데미지 처리 (슬레이어 지식 효과: 주는 피해 증가, 받는 피해 감소)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof Monster monster) {
            // 플레이어가 몬스터 가격 시: 도감 레벨에 따른 데미지 증가
            int knowledgeLevel = getSlayerLevel(player, monster.getType().name());
            if (knowledgeLevel >= 1) {
                // Lv.1: 피해량 +5%
                double bonus = event.getDamage() * 0.05;
                event.setDamage(event.getDamage() + bonus);
            }
        } else if (event.getEntity() instanceof Player player && event.getDamager() instanceof Monster monster) {
            // 몬스터가 플레이어 가격 시: 도감 레벨에 따른 피해 감소
            int knowledgeLevel = getSlayerLevel(player, monster.getType().name());
            if (knowledgeLevel >= 2) {
                // Lv.2: 받는 피해 -10%
                double reduction = event.getDamage() * 0.10;
                event.setDamage(Math.max(0, event.getDamage() - reduction));
            }
        }
    }

    // 3. 처치 시 보상 및 카운트 증가
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null)
            return;
        if (!(entity instanceof Monster))
            return;

        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        String tier = pdc.get(TIER_KEY, PersistentDataType.STRING);
        boolean isElite = "ELITE".equals(tier);

        // 엘리트 몹 보상
        if (isElite) {
            // 경험치 대량
            event.setDroppedExp(event.getDroppedExp() * 5);

            // 강화 가죽 드롭 (확률)
            if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                ItemStack leather = ItemBuilder.of(Material.LEATHER)
                        .name("§c강화 가죽")
                        .lore("§7엘리트 몬스터의 가죽입니다.")
                        .lore("§7질겨서 갑옷 안감으로 쓰입니다.")
                        .customModelData(40001)
                        .build();
                entity.getWorld().dropItemNaturally(entity.getLocation(), leather);
            }

            killer.sendMessage("§c§l[Alpha 처치] §e강력한 몬스터를 제압했습니다!");
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 0.5f);
        }

        // 슬레이어 지식(Kill Count) 업데이트
        updateSlayerCount(killer, entity.getType().name());

        // 지식 Lv.3 효과: 경험치/돈 1.5배 (JobManager 연동 필요, 여기선 드롭 경험치만 증가시킴)
        int knowledgeLevel = getSlayerLevel(killer, entity.getType().name());
        if (knowledgeLevel >= 3) {
            event.setDroppedExp((int) (event.getDroppedExp() * 1.5));
        }
    }

    // --- Helper Methods ---
    // 실제로는 UserData(DB)에 저장해야 하지만, 여기서는 간단히 메모리나 PDC 임시 저장 로직으로 개념만 구현
    // UserData에 Map<String, Integer> mobKillCounts 추가 권장.

    private void updateSlayerCount(Player player, String mobType) {
        var userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null)
            return;

        userData.addMobKillCount(mobType, 1);

        // 디버그: 카운트 알림
        // player.sendActionBar(Component.text("§7[도감] " + mobType + ": " +
        // userData.getMobKillCount(mobType)));
    }

    private int getSlayerLevel(Player player, String mobType) {
        var userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null)
            return 0;

        int kills = userData.getMobKillCount(mobType);

        if (kills >= 1000)
            return 3;
        if (kills >= 500)
            return 2;
        if (kills >= 100)
            return 1;

        return 0;
    }
}
