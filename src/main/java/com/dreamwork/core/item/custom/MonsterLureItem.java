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
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * 몬스터 미끼 (Monster Lure) 아이템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 사냥꾼 Lv.50 해금
 * - 사용 시 주변 몬스터들을 10초간 유인
 * </p>
 */
public class MonsterLureItem implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey itemKey;

    /** 유인 중인 위치 (위치 키 -> 종료 시간) */
    private final Map<String, Long> activeLures = new HashMap<>();

    /** 유인 반경 */
    private static final int LURE_RADIUS = 30;

    /** 유인 지속 시간 (밀리초) */
    private static final long LURE_DURATION_MS = 10_000; // 10초

    public MonsterLureItem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "monster_lure");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startLureTask();
    }

    /**
     * 몬스터 미끼 아이템을 생성합니다.
     */
    public ItemStack createItem() {
        ItemStack item = ItemBuilder.of(Material.ROTTEN_FLESH)
                .name("§e§l몬스터 미끼")
                .lore("")
                .lore("§7강렬한 향기가 나는 미끼입니다.")
                .lore("§7몬스터들이 냄새에 이끌려옵니다.")
                .lore("")
                .lore("§e[우클릭] §f미끼 사용")
                .lore("")
                .lore("§8반경: §a" + LURE_RADIUS + "블록")
                .lore("§8지속: §a10초")
                .lore("")
                .lore("§8[사냥꾼 Lv.50 필요]")
                .customModelData(10071)
                .build();

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 유인 효과를 지속적으로 적용하는 태스크
     */
    private void startLureTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                processLures();
            }
        }.runTaskTimer(plugin, 10L, 10L); // 0.5초마다
    }

    /**
     * 모든 활성화된 미끼를 처리합니다.
     */
    private void processLures() {
        long now = System.currentTimeMillis();

        // 만료된 미끼 제거
        activeLures.entrySet().removeIf(entry -> entry.getValue() < now);

        // 각 미끼에 대해 몬스터 유인
        for (String lureKey : activeLures.keySet()) {
            Location lureLoc = parseLocationKey(lureKey);
            if (lureLoc == null)
                continue;

            // 범위 내 몬스터 유인
            for (Entity entity : lureLoc.getWorld().getNearbyEntities(lureLoc, LURE_RADIUS, LURE_RADIUS, LURE_RADIUS)) {
                if (!(entity instanceof Mob mob))
                    continue;

                // 플레이어가 아닌 몬스터만 유인
                mob.getPathfinder().moveTo(lureLoc);
            }

            // 파티클 효과
            lureLoc.getWorld().spawnParticle(
                    Particle.WITCH,
                    lureLoc.clone().add(0, 1, 0),
                    5, 0.5, 0.5, 0.5, 0);
        }
    }

    /**
     * 아이템 사용 이벤트 처리
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick())
            return;

        ItemStack item = event.getItem();
        if (!isMonsterLure(item))
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

        Location useLoc = player.getLocation();
        String locKey = getLocationKey(useLoc);

        // 미끼 활성화
        activeLures.put(locKey, System.currentTimeMillis() + LURE_DURATION_MS);

        // 아이템 소모
        item.setAmount(item.getAmount() - 1);

        // 효과
        player.playSound(useLoc, Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 0.5f);
        useLoc.getWorld().spawnParticle(
                Particle.WITCH,
                useLoc.clone().add(0, 1, 0),
                30, 2, 1, 2, 0.1);

        player.sendMessage("§e[사냥꾼] §f몬스터 미끼를 사용했습니다! (10초간 유인)");
    }

    /**
     * 아이템이 몬스터 미끼인지 확인합니다.
     */
    public boolean isMonsterLure(ItemStack item) {
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
