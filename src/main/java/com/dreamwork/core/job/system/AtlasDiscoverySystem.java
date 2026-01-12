package com.dreamwork.core.job.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 아틀라스 발견 시스템 (Atlas Discovery System)
 * 
 * <p>
 * 플레이어가 새로운 청크/지역을 탐험할 때 최초 발견 보상을 지급합니다.
 * Chunk의 PDC(PersistentDataContainer)를 활용하여 해당 청크의 발견 여부를 영구 저장합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class AtlasDiscoverySystem implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey DISCOVERED_BY_KEY;
    private final NamespacedKey DISCOVERED_TIME_KEY;

    // 플레이어 이동 이벤트 과부하 방지를 위한 캐시 (최근 방문 청크)
    private final Map<UUID, Long> chunkKeyCache = new HashMap<>();

    public AtlasDiscoverySystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.DISCOVERED_BY_KEY = new NamespacedKey(plugin, "atlas_discovered_by");
        this.DISCOVERED_TIME_KEY = new NamespacedKey(plugin, "atlas_discovered_time");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // 블록 단위 이동이 아니면 무시 (성능 최적화)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Chunk chunk = event.getTo().getChunk();

        long chunkKey = (long) chunk.getX() & 0xffffffffL | ((long) chunk.getZ() & 0xffffffffL) << 32;

        // 같은 청크 내 이동이면 무시 (캐시 활용)
        if (chunkKeyCache.getOrDefault(player.getUniqueId(), -1L) == chunkKey) {
            return;
        }
        chunkKeyCache.put(player.getUniqueId(), chunkKey);

        processDiscovery(player, chunk);
    }

    private void processDiscovery(Player player, Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        // 이미 발견된 청크인지 확인
        if (pdc.has(DISCOVERED_BY_KEY, PersistentDataType.STRING)) {
            // 이미 누군가 발견함. (추가 로직: '내가' 처음 방문했는지 체크하려면 UserData에 방문 기록 필요)
            // 설계안상 "서버 내 최초 진입"이 핵심이므로, 청크 데이터만 보면 됨.
            return;
        }

        // 최초 발견 처리
        // 1. 청크에 발견자 정보 저장
        pdc.set(DISCOVERED_BY_KEY, PersistentDataType.STRING, player.getName());
        pdc.set(DISCOVERED_TIME_KEY, PersistentDataType.LONG, System.currentTimeMillis());

        // 2. 보상 지급 (모험가 직업 레벨에 비례?)
        // 직업 상관없이 주거나, 모험가 레벨이 높으면 더 주거나.
        // 여기서는 기본 보상 + 모험가 보너스

        var userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null)
            return;

        int explorerLevel = userData.getJobLevel(JobType.EXPLORER);

        // 기본 경험치 5.0 + 레벨 비례
        double expReward = 5.0 + (explorerLevel * 0.1);

        plugin.getJobManager().addExp(player, JobType.EXPLORER, expReward);

        // 3. UserData에 방문 기록 저장 (개인 업적용)
        long chunkKey = (long) chunk.getX() & 0xffffffffL | ((long) chunk.getZ() & 0xffffffffL) << 32;
        userData.addExploredChunk(chunkKey);

        // 4. 메시지 알림 (너무 자주 뜨면 귀찮으므로 Actionbar로 처리)
        // 단, 주요 구조물 발견 시에는 타이틀/전체메시지인데, 일반 청크는 개인 알림만.
        player.sendActionBar(net.kyori.adventure.text.Component
                .text("§b[신대륙 발견] §f새로운 지역을 개척했습니다! §7(+" + String.format("%.1f", expReward) + " EXP)"));
        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.5f, 1.2f);
    }
}
