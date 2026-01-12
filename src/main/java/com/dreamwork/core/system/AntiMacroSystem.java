package com.dreamwork.core.system;

import com.dreamwork.core.DreamWorkCore;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 매크로 방지 시스템 (Anti-Macro)
 * <p>
 * 같은 위치에서 반복적인 행동을 감지하여
 * 매크로 사용을 방지합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class AntiMacroSystem implements Listener {

    private final DreamWorkCore plugin;

    /** 플레이어별 최근 위치 기록 */
    private final Map<UUID, LinkedList<Location>> recentLocations = new ConcurrentHashMap<>();

    /** 플레이어별 경고 횟수 */
    private final Map<UUID, Integer> warningCount = new ConcurrentHashMap<>();

    /** 위치 기록 최대 개수 */
    private static final int MAX_LOCATION_HISTORY = 20;

    /** 같은 위치 판정 거리 (블록) */
    private static final double SAME_LOCATION_THRESHOLD = 2.0;

    /** 매크로 판정 비율 (50% 이상 같은 위치면 의심) */
    private static final double MACRO_THRESHOLD = 0.5;

    /** 경고 최대 횟수 */
    private static final int MAX_WARNINGS = 5;

    public AntiMacroSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        checkAction(event.getPlayer(), event.getBlock().getLocation(), "채굴");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            checkAction(event.getPlayer(), event.getPlayer().getLocation(), "낚시");
        }
    }

    /**
     * 행동을 체크하고 매크로 여부를 판정합니다.
     */
    private void checkAction(Player player, Location location, String actionType) {
        UUID uuid = player.getUniqueId();

        LinkedList<Location> locations = recentLocations.computeIfAbsent(uuid, k -> new LinkedList<>());

        // 위치 기록
        locations.addLast(location.clone());
        while (locations.size() > MAX_LOCATION_HISTORY) {
            locations.removeFirst();
        }

        // 충분한 기록이 있을 때만 체크
        if (locations.size() < MAX_LOCATION_HISTORY)
            return;

        // 같은 위치 비율 계산
        Location latest = locations.getLast();
        int sameLocationCount = 0;

        for (Location loc : locations) {
            if (isSameLocation(loc, latest)) {
                sameLocationCount++;
            }
        }

        double ratio = (double) sameLocationCount / locations.size();

        if (ratio >= MACRO_THRESHOLD) {
            // 매크로 의심
            int warnings = warningCount.merge(uuid, 1, Integer::sum);

            if (warnings <= MAX_WARNINGS) {
                player.sendMessage(
                        "§c§l[경고] §f같은 위치에서 반복적인 " + actionType + "이 감지되었습니다. (" + warnings + "/" + MAX_WARNINGS + ")");
                player.sendMessage("§7이동하면서 활동해 주세요. 매크로 사용은 금지됩니다.");

                // 디버그 로그
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning(
                            "[AntiMacro] " + player.getName() + " 매크로 의심 (" + actionType + ") - 경고 " + warnings);
                }
            }

            if (warnings >= MAX_WARNINGS) {
                // 경험치 획득 차단 (실제로는 별도 플래그로 관리)
                player.sendMessage("§c§l[경고] §f매크로 의심으로 인해 일시적으로 경험치 획득이 제한됩니다.");
                player.sendMessage("§7정상적인 플레이를 위해 위치를 이동해 주세요.");

                // 운영자 알림
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("dreamwork.admin"))
                        .forEach(p -> p
                                .sendMessage("§c[AntiMacro] §f" + player.getName() + " 매크로 의심 (" + actionType + ")"));
            }
        } else {
            // 정상 활동 - 경고 감소
            warningCount.compute(uuid, (k, v) -> v == null || v <= 1 ? null : v - 1);
        }
    }

    private boolean isSameLocation(Location a, Location b) {
        if (!Objects.equals(a.getWorld(), b.getWorld()))
            return false;
        return a.distanceSquared(b) <= SAME_LOCATION_THRESHOLD * SAME_LOCATION_THRESHOLD;
    }

    /**
     * 플레이어가 매크로 제한 상태인지 확인합니다.
     */
    public boolean isRestricted(Player player) {
        Integer warnings = warningCount.get(player.getUniqueId());
        return warnings != null && warnings >= MAX_WARNINGS;
    }

    /**
     * 플레이어의 경고를 초기화합니다.
     */
    public void resetWarnings(Player player) {
        warningCount.remove(player.getUniqueId());
        recentLocations.remove(player.getUniqueId());
    }
}
