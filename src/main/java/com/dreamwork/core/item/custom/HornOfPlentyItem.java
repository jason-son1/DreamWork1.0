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
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 풍요의 뿔피리 (Horn of Plenty) 아이템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 농부 Lv.50 해금 아이템
 * - 사용 시 주변 7블록 반경의 작물을 즉시 완전 성장
 * - 3분 재사용 대기
 * </p>
 */
public class HornOfPlentyItem implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey itemKey;

    /** 플레이어별 쿨다운 (UUID -> 종료 시간) */
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /** 쿨다운 시간 (밀리초) */
    private static final long COOLDOWN_MS = 180_000; // 3분

    /** 효과 반경 */
    private static final int RADIUS = 7;

    public HornOfPlentyItem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "horn_of_plenty");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 풍요의 뿔피리 아이템을 생성합니다.
     */
    public ItemStack createItem() {
        ItemStack item = ItemBuilder.of(Material.GOAT_HORN)
                .name("§6§l풍요의 뿔피리")
                .lore("")
                .lore("§7고대 농부의 축복이 깃든 뿔피리입니다.")
                .lore("")
                .lore("§e[우클릭] §f주변 작물 즉시 성장")
                .lore("")
                .lore("§8반경: §a" + RADIUS + "블록")
                .lore("§8재사용 대기: §a3분")
                .lore("")
                .lore("§7§o\"대지가 당신의 부름에 응답합니다.\"")
                .lore("")
                .lore("§8[농부 Lv.50 필요]")
                .customModelData(10040)
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
        if (!isHornOfPlenty(item))
            return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 농부 레벨 확인
        int farmerLevel = getFarmerLevel(player);
        if (farmerLevel < 50) {
            player.sendMessage("§c[농부] 이 아이템은 농부 레벨 50 이상만 사용할 수 있습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 쿨다운 확인
        if (isOnCooldown(uuid)) {
            long remaining = getRemainingCooldown(uuid);
            player.sendMessage("§c[농부] 재사용 대기 중입니다. (" + (remaining / 1000) + "초)");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 효과 실행
        int grownCrops = growCropsInRadius(player.getLocation());

        if (grownCrops == 0) {
            player.sendMessage("§e[농부] 주변에 성장시킬 작물이 없습니다.");
            return;
        }

        // 쿨다운 시작
        cooldowns.put(uuid, System.currentTimeMillis() + COOLDOWN_MS);

        // 효과 및 메시지
        player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1f, 1f);
        player.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                player.getLocation().add(0, 1, 0),
                50, RADIUS, 1, RADIUS, 0);

        player.sendMessage("§a[농부] §f풍요의 뿔피리가 " + grownCrops + "개의 작물을 성장시켰습니다!");
    }

    /**
     * 반경 내 작물을 즉시 성장시킵니다.
     */
    private int growCropsInRadius(Location center) {
        int count = 0;

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();

                    if (!isCrop(block.getType()))
                        continue;
                    if (!(block.getBlockData() instanceof Ageable ageable))
                        continue;

                    // 이미 다 자란 경우 스킵
                    if (ageable.getAge() >= ageable.getMaximumAge())
                        continue;

                    // 완전 성장
                    ageable.setAge(ageable.getMaximumAge());
                    block.setBlockData(ageable);
                    count++;

                    // 파티클 효과
                    block.getWorld().spawnParticle(
                            Particle.HAPPY_VILLAGER,
                            block.getLocation().add(0.5, 0.5, 0.5),
                            5, 0.2, 0.2, 0.2, 0);
                }
            }
        }

        return count;
    }

    /**
     * 블록이 작물인지 확인합니다.
     */
    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS,
                    NETHER_WART, SWEET_BERRY_BUSH, COCOA,
                    TORCHFLOWER_CROP, PITCHER_CROP ->
                true;
            default -> false;
        };
    }

    /**
     * 아이템이 풍요의 뿔피리인지 확인합니다.
     */
    public boolean isHornOfPlenty(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    /**
     * 쿨다운 중인지 확인합니다.
     */
    private boolean isOnCooldown(UUID uuid) {
        Long endTime = cooldowns.get(uuid);
        if (endTime == null)
            return false;
        return System.currentTimeMillis() < endTime;
    }

    /**
     * 남은 쿨다운 시간을 반환합니다.
     */
    private long getRemainingCooldown(UUID uuid) {
        Long endTime = cooldowns.get(uuid);
        if (endTime == null)
            return 0;
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    /**
     * 플레이어의 농부 레벨을 반환합니다.
     */
    private int getFarmerLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"farmer".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }
}
