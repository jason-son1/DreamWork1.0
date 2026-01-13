package com.dreamwork.core.item.custom;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * 사냥용 덫 (Bear Trap) 아이템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 사냥꾼 Lv.50 해금
 * - 설치 시 밟은 몬스터를 3초간 속박
 * - 5 피해 입힘
 * </p>
 */
public class BearTrapItem implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey itemKey;

    /** 설치된 덫 위치 (위치 키 -> 만료 시간) */
    private final Map<String, Long> placedTraps = new HashMap<>();

    /** 덫 지속 시간 (밀리초) */
    private static final long TRAP_DURATION_MS = 60_000; // 1분간 유지

    /** 속박 시간 (틱) */
    private static final int SNARE_DURATION_TICKS = 60; // 3초

    public BearTrapItem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "bear_trap");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startTrapCheckTask();
    }

    /**
     * 사냥용 덫 아이템을 생성합니다.
     */
    public ItemStack createItem() {
        ItemStack item = ItemBuilder.of(Material.TRIPWIRE_HOOK)
                .name("§c§l사냥용 덫")
                .lore("")
                .lore("§7날카로운 이빨이 달린 덫입니다.")
                .lore("")
                .lore("§e[설치] §f바닥에 놓아 사용")
                .lore("")
                .lore("§8효과: §c3초 속박 + 5 피해")
                .lore("§8지속: §a1분간 유지")
                .lore("")
                .lore("§8[사냥꾼 Lv.50 필요]")
                .customModelData(10070)
                .build();

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 주기적으로 덫 발동을 체크하는 태스크
     */
    private void startTrapCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkTraps();
            }
        }.runTaskTimer(plugin, 5L, 5L); // 0.25초마다 체크
    }

    /**
     * 모든 설치된 덫을 체크합니다.
     */
    private void checkTraps() {
        long now = System.currentTimeMillis();

        // 만료된 덫 제거
        placedTraps.entrySet().removeIf(entry -> entry.getValue() < now);

        // 각 덫에 대해 몬스터 체크
        for (String trapKey : new HashSet<>(placedTraps.keySet())) {
            Location trapLoc = parseLocationKey(trapKey);
            if (trapLoc == null)
                continue;

            // 범위 내 엔티티 확인
            for (Entity entity : trapLoc.getWorld().getNearbyEntities(trapLoc, 0.5, 0.5, 0.5)) {
                if (!(entity instanceof LivingEntity living))
                    continue;
                if (entity instanceof Player)
                    continue; // 플레이어는 제외

                // 덫 발동!
                triggerTrap(trapLoc, living);
                placedTraps.remove(trapKey);
                break;
            }
        }
    }

    /**
     * 덫을 발동시킵니다.
     */
    private void triggerTrap(Location trapLoc, LivingEntity target) {
        // 피해 입힘
        target.damage(5.0);

        // 속박 (둔화 + 고정)
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, SNARE_DURATION_TICKS, 100, true, false, false));
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.JUMP_BOOST, SNARE_DURATION_TICKS, 200, true, false, false));

        // 효과
        trapLoc.getWorld().playSound(trapLoc, Sound.ENTITY_IRON_GOLEM_HURT, 1f, 1.5f);
        trapLoc.getWorld().spawnParticle(
                Particle.CRIT,
                trapLoc.clone().add(0.5, 0.5, 0.5),
                20, 0.3, 0.3, 0.3, 0.1);

        // 메시지
        for (Player nearby : trapLoc.getWorld().getPlayers()) {
            if (nearby.getLocation().distance(trapLoc) < 30) {
                nearby.sendMessage("§c[덫] §f" + target.getName() + "이(가) 덫에 걸렸습니다!");
            }
        }
    }

    /**
     * 덫 설치 (블록 배치 이벤트)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!isBearTrap(item))
            return;

        event.setCancelled(true);

        Player player = event.getPlayer();

        // 사냥꾼 레벨 확인
        int hunterLevel = getHunterLevel(player);
        if (hunterLevel < 50) {
            player.sendMessage("§c[사냥꾼] 이 아이템은 사냥꾼 레벨 50 이상만 사용할 수 있습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        Location placeLoc = event.getBlock().getLocation();
        String locKey = getLocationKey(placeLoc);

        // 덫 설치
        placedTraps.put(locKey, System.currentTimeMillis() + TRAP_DURATION_MS);

        // 아이템 소모
        item.setAmount(item.getAmount() - 1);

        // 효과
        player.playSound(placeLoc, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1f, 0.8f);
        placeLoc.getWorld().spawnParticle(
                Particle.SMOKE,
                placeLoc.clone().add(0.5, 0.1, 0.5),
                10, 0.2, 0.1, 0.2, 0);

        player.sendMessage("§c[사냥꾼] §f사냥용 덫을 설치했습니다. (1분간 유지)");
    }

    /**
     * 아이템이 사냥용 덫인지 확인합니다.
     */
    public boolean isBearTrap(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    /**
     * 위치 키를 생성합니다.
     */
    private String getLocationKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * 위치 키를 파싱합니다.
     */
    private Location parseLocationKey(String key) {
        try {
            String[] parts = key.split(":");
            org.bukkit.World world = plugin.getServer().getWorld(parts[0]);
            if (world == null)
                return null;
            return new Location(world,
                    Integer.parseInt(parts[1]) + 0.5,
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]) + 0.5);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 플레이어의 사냥꾼 레벨을 반환합니다.
     */
    private int getHunterLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"hunter".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }
}
