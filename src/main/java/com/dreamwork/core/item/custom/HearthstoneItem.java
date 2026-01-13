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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * 귀환석 (Hearthstone) 아이템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 탐험가 Lv.50 해금
 * - 사용 시 지정된 위치로 텔레포트
 * - 5초 캐스팅 (이동 시 취소)
 * - 30분 재사용 대기
 * </p>
 */
public class HearthstoneItem implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey itemKey;
    private final NamespacedKey homeWorldKey;
    private final NamespacedKey homeXKey;
    private final NamespacedKey homeYKey;
    private final NamespacedKey homeZKey;

    /** 캐스팅 중인 플레이어 (UUID -> 종료 시간, 목표 위치) */
    private final Map<UUID, CastingData> castingPlayers = new HashMap<>();

    /** 쿨다운 중인 플레이어 */
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /** 캐스팅 시간 (밀리초) */
    private static final long CAST_TIME_MS = 5000; // 5초

    /** 쿨다운 (밀리초) */
    private static final long COOLDOWN_MS = 1800_000; // 30분

    public HearthstoneItem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "hearthstone");
        this.homeWorldKey = new NamespacedKey(plugin, "hearthstone_world");
        this.homeXKey = new NamespacedKey(plugin, "hearthstone_x");
        this.homeYKey = new NamespacedKey(plugin, "hearthstone_y");
        this.homeZKey = new NamespacedKey(plugin, "hearthstone_z");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 귀환석 아이템을 생성합니다.
     */
    public ItemStack createItem() {
        ItemStack item = ItemBuilder.of(Material.ECHO_SHARD)
                .name("§b§l귀환석")
                .lore("")
                .lore("§7고향의 기억이 담긴 신비로운 돌입니다.")
                .lore("")
                .lore("§e[우클릭] §f저장된 위치로 귀환")
                .lore("§e[Shift+우클릭] §f현재 위치 저장")
                .lore("")
                .lore("§8캐스팅 시간: §a5초")
                .lore("§8재사용 대기: §a30분")
                .lore("")
                .lore("§c이동 시 캐스팅 취소")
                .lore("")
                .lore("§7§o저장된 위치: 없음")
                .lore("")
                .lore("§8[탐험가 Lv.50 필요]")
                .customModelData(10060)
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
        if (!isHearthstone(item))
            return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 탐험가 레벨 확인
        int explorerLevel = getExplorerLevel(player);
        if (explorerLevel < 50) {
            player.sendMessage("§c[탐험가] 이 아이템은 탐험가 레벨 50 이상만 사용할 수 있습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Shift + 우클릭: 위치 저장
        if (player.isSneaking()) {
            saveHomeLocation(player, item);
            return;
        }

        // 이미 캐스팅 중인지 확인
        if (castingPlayers.containsKey(uuid)) {
            player.sendMessage("§c[탐험가] 이미 귀환 중입니다.");
            return;
        }

        // 쿨다운 확인
        if (isOnCooldown(uuid)) {
            long remaining = getRemainingCooldown(uuid);
            player.sendMessage(
                    "§c[탐험가] 재사용 대기 중입니다. (" + (remaining / 60000) + "분 " + ((remaining / 1000) % 60) + "초)");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 저장된 위치 확인
        Location homeLocation = getHomeLocation(item);
        if (homeLocation == null) {
            player.sendMessage("§c[탐험가] 저장된 위치가 없습니다. Shift+우클릭으로 현재 위치를 저장하세요.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 캐스팅 시작
        startCasting(player, homeLocation);
    }

    /**
     * 이동 시 캐스팅 취소
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!castingPlayers.containsKey(uuid))
            return;

        // 실제 이동인지 확인 (블록 좌표 변경)
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            cancelCasting(player);
            player.sendMessage("§c[탐험가] 이동으로 인해 귀환이 취소되었습니다.");
        }
    }

    /**
     * 캐스팅을 시작합니다.
     */
    private void startCasting(Player player, Location destination) {
        UUID uuid = player.getUniqueId();
        long endTime = System.currentTimeMillis() + CAST_TIME_MS;

        castingPlayers.put(uuid, new CastingData(endTime, destination));

        player.sendMessage("§b[탐험가] §f귀환 시작... (5초)");
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.5f);

        // 캐스팅 태스크
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!castingPlayers.containsKey(uuid)) {
                    cancel();
                    return;
                }

                ticks++;

                // 파티클 효과
                player.getWorld().spawnParticle(
                        Particle.PORTAL,
                        player.getLocation().add(0, 1, 0),
                        20, 0.5, 0.5, 0.5, 0);

                // 진행 상황 표시
                int remaining = 5 - (ticks / 20);
                if (ticks % 20 == 0 && remaining > 0) {
                    player.sendActionBar(
                            net.kyori.adventure.text.Component.text("§b귀환 중... §f" + remaining + "초"));
                }

                // 캐스팅 완료
                if (ticks >= 100) { // 5초 = 100틱
                    completeTeleport(player, destination);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 텔레포트를 완료합니다.
     */
    private void completeTeleport(Player player, Location destination) {
        UUID uuid = player.getUniqueId();

        castingPlayers.remove(uuid);
        cooldowns.put(uuid, System.currentTimeMillis() + COOLDOWN_MS);

        // 텔레포트
        player.teleport(destination);

        // 효과
        player.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        player.getWorld().spawnParticle(
                Particle.REVERSE_PORTAL,
                destination.clone().add(0, 1, 0),
                50, 0.5, 1, 0.5, 0);

        player.sendMessage("§b[탐험가] §f귀환 완료!");
    }

    /**
     * 캐스팅을 취소합니다.
     */
    private void cancelCasting(Player player) {
        castingPlayers.remove(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
    }

    /**
     * 현재 위치를 저장합니다.
     */
    private void saveHomeLocation(Player player, ItemStack item) {
        Location loc = player.getLocation();

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(homeWorldKey, PersistentDataType.STRING, loc.getWorld().getName());
        pdc.set(homeXKey, PersistentDataType.DOUBLE, loc.getX());
        pdc.set(homeYKey, PersistentDataType.DOUBLE, loc.getY());
        pdc.set(homeZKey, PersistentDataType.DOUBLE, loc.getZ());

        // 로어 업데이트
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7고향의 기억이 담긴 신비로운 돌입니다.");
        lore.add("");
        lore.add("§e[우클릭] §f저장된 위치로 귀환");
        lore.add("§e[Shift+우클릭] §f현재 위치 저장");
        lore.add("");
        lore.add("§8캐스팅 시간: §a5초");
        lore.add("§8재사용 대기: §a30분");
        lore.add("");
        lore.add("§c이동 시 캐스팅 취소");
        lore.add("");
        lore.add("§7§o저장된 위치: §f" + loc.getWorld().getName() + " (" +
                (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ() + ")");
        lore.add("");
        lore.add("§8[탐험가 Lv.50 필요]");
        meta.setLore(lore);

        item.setItemMeta(meta);

        player.sendMessage("§b[탐험가] §f현재 위치를 귀환석에 저장했습니다.");
        player.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.5f);
    }

    /**
     * 저장된 위치를 가져옵니다.
     */
    private Location getHomeLocation(ItemStack item) {
        if (!item.hasItemMeta())
            return null;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        String worldName = pdc.get(homeWorldKey, PersistentDataType.STRING);
        if (worldName == null)
            return null;

        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null)
            return null;

        Double x = pdc.get(homeXKey, PersistentDataType.DOUBLE);
        Double y = pdc.get(homeYKey, PersistentDataType.DOUBLE);
        Double z = pdc.get(homeZKey, PersistentDataType.DOUBLE);

        if (x == null || y == null || z == null)
            return null;

        return new Location(world, x, y, z);
    }

    /**
     * 아이템이 귀환석인지 확인합니다.
     */
    public boolean isHearthstone(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    /**
     * 쿨다운 관련 메서드들
     */
    private boolean isOnCooldown(UUID uuid) {
        Long endTime = cooldowns.get(uuid);
        if (endTime == null)
            return false;
        return System.currentTimeMillis() < endTime;
    }

    private long getRemainingCooldown(UUID uuid) {
        Long endTime = cooldowns.get(uuid);
        if (endTime == null)
            return 0;
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    /**
     * 플레이어의 탐험가 레벨을 반환합니다.
     */
    private int getExplorerLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"explorer".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }

    /**
     * 캐스팅 데이터 클래스
     */
    private record CastingData(long endTime, Location destination) {
    }
}
