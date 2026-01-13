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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * 떡밥 (Chum) 아이템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 어부 Lv.50 해금
 * - 물에 던지면 5분간 해당 위치에서 희귀 물고기 확률 증가
 * </p>
 */
public class ChumItem implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey itemKey;

    /** 활성화된 떡밥 존 (위치 -> 만료 시간) */
    private final Map<String, Long> activeChumZones = new HashMap<>();

    /** 효과 지속 시간 (밀리초) */
    private static final long DURATION_MS = 300_000; // 5분

    /** 효과 반경 */
    private static final int RADIUS = 10;

    public ChumItem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "fishing_chum");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 떡밥 아이템을 생성합니다.
     */
    public ItemStack createItem() {
        ItemStack item = ItemBuilder.of(Material.FERMENTED_SPIDER_EYE)
                .name("§b§l떡밥")
                .lore("")
                .lore("§7잘 발효된 미끼입니다.")
                .lore("§7물에 던지면 물고기가 모여듭니다.")
                .lore("")
                .lore("§e[우클릭] §f물에 던지기")
                .lore("")
                .lore("§8지속 시간: §a5분")
                .lore("§8효과 범위: §a" + RADIUS + "블록")
                .lore("")
                .lore("§a희귀 물고기 확률 2배")
                .lore("§a쓰레기 확률 50% 감소")
                .lore("")
                .lore("§8[어부 Lv.50 필요]")
                .customModelData(10050)
                .build();

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 아이템 사용 이벤트 처리
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick())
            return;

        ItemStack item = event.getItem();
        if (!isChum(item))
            return;

        event.setCancelled(true);

        Player player = event.getPlayer();

        // 어부 레벨 확인
        int fisherLevel = getFisherLevel(player);
        if (fisherLevel < 50) {
            player.sendMessage("§c[어부] 이 아이템은 어부 레벨 50 이상만 사용할 수 있습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 물 근처인지 확인
        Location targetLoc = player.getTargetBlock(null, 50).getLocation();
        if (!isNearWater(targetLoc)) {
            player.sendMessage("§c[어부] 물 근처에서 사용해주세요.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 떡밥 사용
        String zoneKey = getZoneKey(targetLoc);
        activeChumZones.put(zoneKey, System.currentTimeMillis() + DURATION_MS);

        // 아이템 소모
        item.setAmount(item.getAmount() - 1);

        // 효과
        player.playSound(targetLoc, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1f, 0.8f);
        player.getWorld().spawnParticle(
                Particle.FISHING,
                targetLoc.clone().add(0.5, 1, 0.5),
                30, 3, 1, 3, 0);

        player.sendMessage("§b[어부] §f떡밥을 물에 뿌렸습니다! §7(5분간 효과 지속)");
    }

    /**
     * 낚시 이벤트 - 떡밥 존에서의 보너스 적용
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH)
            return;

        Location loc = event.getHook().getLocation();

        if (!isInChumZone(loc))
            return;

        // 떡밥 효과가 적용됨을 알림
        Player player = event.getPlayer();
        player.sendMessage("§b[떡밥] §f떡밥 효과로 더 좋은 물고기를 낚았습니다!");

        // 실제 드롭 조정은 FishMeasurementSystem 등에서 처리
    }

    /**
     * 해당 위치가 떡밥 존 내에 있는지 확인합니다.
     */
    public boolean isInChumZone(Location loc) {
        // 만료된 존 정리
        long now = System.currentTimeMillis();
        activeChumZones.entrySet().removeIf(entry -> entry.getValue() < now);

        // 범위 내 존 확인
        for (String zoneKey : activeChumZones.keySet()) {
            Location zoneLoc = parseZoneKey(zoneKey);
            if (zoneLoc != null && zoneLoc.getWorld().equals(loc.getWorld())) {
                if (zoneLoc.distance(loc) <= RADIUS) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 물 근처인지 확인합니다.
     */
    private boolean isNearWater(Location loc) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Material type = loc.clone().add(x, y, z).getBlock().getType();
                    if (type == Material.WATER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 아이템이 떡밥인지 확인합니다.
     */
    public boolean isChum(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    /**
     * 위치 키를 생성합니다.
     */
    private String getZoneKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * 위치 키를 파싱합니다.
     */
    private Location parseZoneKey(String key) {
        try {
            String[] parts = key.split(":");
            org.bukkit.World world = plugin.getServer().getWorld(parts[0]);
            if (world == null)
                return null;
            return new Location(world,
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 플레이어의 어부 레벨을 반환합니다.
     */
    private int getFisherLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"fisher".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }
}
