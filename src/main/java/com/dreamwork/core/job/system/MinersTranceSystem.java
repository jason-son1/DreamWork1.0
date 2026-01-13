package com.dreamwork.core.job.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 광부의 몰입 시스템 (Miner's Trance)
 * 
 * <p>
 * Plan 2.0 기준:
 * - 한 청크에서 지속적으로 채굴할 때 단계별 버프 적용
 * - 단계별 효과:
 * - Stage 1 (10분): 경험치 +10%
 * - Stage 2 (20분): 경험치 +15%, Fortune +0.5 효과
 * - Stage 3 (30분+): 경험치 +25%, Fortune +1.0, 희귀 드롭 2배
 * - 청크 이탈 또는 5분 비활동 시 초기화
 * </p>
 */
public class MinersTranceSystem implements Listener {

    private final DreamWorkCore plugin;

    /** 플레이어별 몰입 데이터 */
    private final Map<UUID, TranceData> tranceDataMap = new ConcurrentHashMap<>();

    /** 비활동 초기화 시간 (틱) */
    private static final long IDLE_RESET_TICKS = 20 * 60 * 5; // 5분

    /** 분당 최소 채굴 횟수 */
    private static final int MIN_BREAKS_PER_MINUTE = 5;

    public MinersTranceSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startTickTask();
    }

    /**
     * 매 초마다 몰입 상태를 업데이트하는 태스크를 시작합니다.
     */
    private void startTickTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                for (Map.Entry<UUID, TranceData> entry : tranceDataMap.entrySet()) {
                    UUID uuid = entry.getKey();
                    TranceData data = entry.getValue();
                    Player player = plugin.getServer().getPlayer(uuid);

                    if (player == null || !player.isOnline()) {
                        continue;
                    }

                    // 비활동 체크 (마지막 채굴로부터 5분 경과)
                    if (currentTime - data.lastBreakTime > IDLE_RESET_TICKS * 50) {
                        resetTrance(uuid, "비활동");
                        continue;
                    }

                    // 분당 채굴 횟수 체크
                    long oneMinuteAgo = currentTime - 60_000;
                    if (data.sessionStartTime < oneMinuteAgo) {
                        int minutesPassed = (int) ((currentTime - data.sessionStartTime) / 60_000);
                        int expectedBreaks = minutesPassed * MIN_BREAKS_PER_MINUTE;

                        if (data.totalBreaks < expectedBreaks) {
                            // 채굴 활동 부족
                            data.tranceMinutes = 0;
                            continue;
                        }
                    }

                    // 몰입 시간 증가 (초당 1/60분)
                    data.secondsInTrance++;
                    if (data.secondsInTrance >= 60) {
                        data.secondsInTrance = 0;
                        data.tranceMinutes++;
                        onTranceMinuteUp(player, data.tranceMinutes);
                    }

                    // 액션바 업데이트
                    updateActionBar(player, data);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * 몰입 분이 증가했을 때 호출됩니다.
     */
    private void onTranceMinuteUp(Player player, int minutes) {
        int stage = getStage(minutes);
        int prevStage = getStage(minutes - 1);

        if (stage > prevStage) {
            // 새로운 단계 진입
            String stageName = getStageMessage(stage);
            player.sendMessage(ChatColor.GOLD + "[광부의 몰입] " + ChatColor.WHITE + stageName);

            if (stage == 3) {
                // Stage 3 파티클 효과
                player.getWorld().spawnParticle(
                        Particle.TOTEM_OF_UNDYING,
                        player.getLocation().add(0, 1, 0),
                        20, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    /**
     * 액션바에 몰입 상태를 표시합니다.
     */
    private void updateActionBar(Player player, TranceData data) {
        int stage = getStage(data.tranceMinutes);
        if (stage == 0)
            return;

        String message;
        switch (stage) {
            case 1:
                message = ChatColor.GRAY + "⛏ 몰입 중... (경험치 +10%)";
                break;
            case 2:
                message = ChatColor.YELLOW + "⛏ 깊은 몰입 중... (경험치 +15%)";
                break;
            case 3:
                message = ChatColor.GOLD + "✨ 완벽한 몰입! (경험치 +25%, 희귀 드롭 2배)";
                break;
            default:
                message = "";
        }

        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(message));
    }

    /**
     * 블록 파괴 이벤트를 처리합니다.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 광부인지 확인
        if (!isMiner(player))
            return;

        Chunk chunk = event.getBlock().getChunk();
        long currentTime = System.currentTimeMillis();

        TranceData data = tranceDataMap.get(uuid);

        if (data == null) {
            // 새 세션 시작
            data = new TranceData();
            data.currentChunk = chunk;
            data.sessionStartTime = currentTime;
            data.lastBreakTime = currentTime;
            data.totalBreaks = 1;
            tranceDataMap.put(uuid, data);
            return;
        }

        // 청크 변경 확인
        if (!isSameChunk(data.currentChunk, chunk)) {
            resetTrance(uuid, "청크 이동");

            // 새 청크에서 다시 시작
            data = new TranceData();
            data.currentChunk = chunk;
            data.sessionStartTime = currentTime;
            data.lastBreakTime = currentTime;
            data.totalBreaks = 1;
            tranceDataMap.put(uuid, data);
            return;
        }

        // 채굴 기록
        data.lastBreakTime = currentTime;
        data.totalBreaks++;
    }

    /**
     * 청크 이동을 감지합니다.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // 청크 변경만 체크
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }

        // 청크 변경은 BlockBreakEvent에서 처리
    }

    /**
     * 플레이어 퇴장 시 데이터 정리
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        tranceDataMap.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 몰입 상태를 초기화합니다.
     */
    private void resetTrance(UUID uuid, String reason) {
        TranceData data = tranceDataMap.remove(uuid);
        if (data != null && data.tranceMinutes >= 10) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                player.sendMessage(ChatColor.GRAY + "[광부의 몰입] " + reason + "으로 인해 몰입이 해제되었습니다.");
            }
        }
    }

    /**
     * 플레이어의 현재 몰입 단계를 반환합니다.
     */
    public int getPlayerStage(UUID uuid) {
        TranceData data = tranceDataMap.get(uuid);
        if (data == null)
            return 0;
        return getStage(data.tranceMinutes);
    }

    /**
     * 분 단위 몰입 시간에 따른 단계를 반환합니다.
     */
    private int getStage(int minutes) {
        if (minutes >= 30)
            return 3;
        if (minutes >= 20)
            return 2;
        if (minutes >= 10)
            return 1;
        return 0;
    }

    /**
     * 단계별 메시지를 반환합니다.
     */
    private String getStageMessage(int stage) {
        switch (stage) {
            case 1:
                return "몰입 1단계 진입! 경험치 획득량 +10%";
            case 2:
                return "몰입 2단계 진입! 경험치 +15%, 드롭률 소폭 상승";
            case 3:
                return "완벽한 몰입! 경험치 +25%, 희귀 드롭 2배!";
            default:
                return "";
        }
    }

    /**
     * 현재 경험치 배율을 반환합니다.
     */
    public double getExpMultiplier(UUID uuid) {
        int stage = getPlayerStage(uuid);
        switch (stage) {
            case 1:
                return 1.10;
            case 2:
                return 1.15;
            case 3:
                return 1.25;
            default:
                return 1.0;
        }
    }

    /**
     * 현재 희귀 드롭 배율을 반환합니다.
     */
    public double getRareDropMultiplier(UUID uuid) {
        int stage = getPlayerStage(uuid);
        return stage == 3 ? 2.0 : 1.0;
    }

    /**
     * Fortune 보너스를 반환합니다.
     */
    public double getFortuneBonus(UUID uuid) {
        int stage = getPlayerStage(uuid);
        switch (stage) {
            case 2:
                return 0.5;
            case 3:
                return 1.0;
            default:
                return 0.0;
        }
    }

    /**
     * 두 청크가 같은지 확인합니다.
     */
    private boolean isSameChunk(Chunk a, Chunk b) {
        return a.getX() == b.getX() &&
                a.getZ() == b.getZ() &&
                a.getWorld().equals(b.getWorld());
    }

    /**
     * 플레이어가 광부인지 확인합니다.
     */
    private boolean isMiner(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return false;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"miner".equals(jobData.getJobId()))
            return false;
        return jobData.getLevel() >= 1;
    }

    /**
     * 몰입 데이터 클래스
     */
    private static class TranceData {
        /** 현재 위치한 청크 */
        Chunk currentChunk;

        /** 세션 시작 시간 */
        long sessionStartTime;

        /** 마지막 채굴 시간 */
        long lastBreakTime;

        /** 총 채굴 횟수 */
        int totalBreaks;

        /** 몰입 시간 (분) */
        int tranceMinutes;

        /** 현재 분 내 초 */
        int secondsInTrance;
    }
}
