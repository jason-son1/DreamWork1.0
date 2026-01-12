package com.dreamwork.core.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 탐험가 아틀라스 시스템 (The Atlas)
 * <p>
 * 새로운 청크, 바이옴, 구조물을 발견하면 보상을 획득합니다.
 * 최초 발견 시 추가 보너스를 제공합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class AtlasDiscoverySystem implements Listener {

    private final DreamWorkCore plugin;

    /** 플레이어별 발견한 청크 (UUID -> Set<"world:x:z">) */
    private final Map<UUID, Set<String>> discoveredChunks = new ConcurrentHashMap<>();

    /** 플레이어별 발견한 바이옴 (UUID -> Set<Biome>) */
    private final Map<UUID, Set<Biome>> discoveredBiomes = new ConcurrentHashMap<>();

    /** 플레이어별 총 발견 청크 수 */
    private final Map<UUID, Integer> totalChunksDiscovered = new ConcurrentHashMap<>();

    /** 마지막 체크 시간 */
    private final Map<UUID, Long> lastCheckTime = new ConcurrentHashMap<>();

    private static final long CHECK_INTERVAL_MS = 1000; // 1초

    /** 청크당 기본 경험치 */
    private static final double CHUNK_EXP = 5.0;

    /** 바이옴 최초 발견 경험치 */
    private static final double BIOME_DISCOVERY_EXP = 100.0;

    public AtlasDiscoverySystem(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 청크 변경 확인
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null)
            return;
        if (from.getChunk().equals(to.getChunk()))
            return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 체크 간격 제한
        long now = System.currentTimeMillis();
        Long lastCheck = lastCheckTime.get(uuid);
        if (lastCheck != null && now - lastCheck < CHECK_INTERVAL_MS) {
            return;
        }
        lastCheckTime.put(uuid, now);

        // 탐험가 레벨 확인
        UserData userData = plugin.getStorageManager().getUserData(uuid);
        if (userData == null)
            return;

        int explorerLevel = userData.getJobLevel(JobType.EXPLORER);

        Chunk chunk = to.getChunk();
        String chunkKey = chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();

        // 청크 발견 체크
        Set<String> playerChunks = discoveredChunks.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());

        if (playerChunks.add(chunkKey)) {
            // 새로운 청크 발견!
            int totalDiscovered = totalChunksDiscovered.merge(uuid, 1, Integer::sum);

            // 경험치 부여
            double exp = CHUNK_EXP * (1 + explorerLevel * 0.01);
            plugin.getJobManager().addExp(player, JobType.EXPLORER, exp);

            // 마일스톤 체크
            checkMilestone(player, totalDiscovered);

            // 바이옴 발견 체크
            checkBiomeDiscovery(player, to.getBlock().getBiome(), explorerLevel);
        }
    }

    /**
     * 바이옴 발견을 체크합니다.
     */
    private void checkBiomeDiscovery(Player player, Biome biome, int explorerLevel) {
        UUID uuid = player.getUniqueId();
        Set<Biome> playerBiomes = discoveredBiomes.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());

        if (playerBiomes.add(biome)) {
            // 새로운 바이옴 발견!
            double exp = BIOME_DISCOVERY_EXP * (1 + explorerLevel * 0.02);
            plugin.getJobManager().addExp(player, JobType.EXPLORER, exp);

            String biomeName = getBiomeName(biome);
            player.sendMessage("§a[탐험가] §e새로운 바이옴 발견: §f" + biomeName);
            player.sendMessage("§7  → 경험치 +" + String.format("%.0f", exp) + ", 총 발견: " + playerBiomes.size() + "개");

            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        }
    }

    /**
     * 마일스톤을 체크합니다.
     */
    private void checkMilestone(Player player, int totalChunks) {
        int[] milestones = { 100, 500, 1000, 5000, 10000 };
        double[] bonusExp = { 500, 2000, 5000, 15000, 50000 };

        for (int i = 0; i < milestones.length; i++) {
            if (totalChunks == milestones[i]) {
                plugin.getJobManager().addExp(player, JobType.EXPLORER, bonusExp[i]);
                player.sendMessage("§6§l[아틀라스] §e" + milestones[i] + " 청크 발견 마일스톤 달성!");
                player.sendMessage("§7  → 보너스 경험치 +" + (int) bonusExp[i]);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                break;
            }
        }
    }

    /**
     * 바이옴 이름을 반환합니다.
     */
    private String getBiomeName(Biome biome) {
        return switch (biome) {
            case PLAINS -> "평원";
            case FOREST -> "숲";
            case DESERT -> "사막";
            case TAIGA -> "타이가";
            case SWAMP -> "늪지대";
            case JUNGLE -> "정글";
            case SNOWY_PLAINS -> "눈 덮인 평원";
            case OCEAN -> "바다";
            case DEEP_OCEAN -> "깊은 바다";
            case RIVER -> "강";
            case BEACH -> "해변";
            case BADLANDS -> "악지";
            case MUSHROOM_FIELDS -> "버섯 들판";
            case NETHER_WASTES -> "네더 황무지";
            case THE_END -> "엔드";
            case CHERRY_GROVE -> "벚꽃 숲";
            case DEEP_DARK -> "깊은 어둠";
            case LUSH_CAVES -> "무성한 동굴";
            case DRIPSTONE_CAVES -> "종유석 동굴";
            default -> biome.name().replace("_", " ");
        };
    }

    /**
     * 플레이어의 총 발견 청크 수를 반환합니다.
     */
    public int getTotalDiscoveredChunks(Player player) {
        return totalChunksDiscovered.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * 플레이어의 발견한 바이옴 수를 반환합니다.
     */
    public int getDiscoveredBiomeCount(Player player) {
        Set<Biome> biomes = discoveredBiomes.get(player.getUniqueId());
        return biomes != null ? biomes.size() : 0;
    }
}
