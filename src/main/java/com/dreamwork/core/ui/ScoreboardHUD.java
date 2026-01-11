package com.dreamwork.core.ui;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.database.StorageManager;
import com.dreamwork.core.model.UserData;
import com.dreamwork.core.stat.StatManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 스코어보드 HUD
 * 
 * <p>
 * 플레이어의 직업, 레벨, 경험치 등을 스코어보드로 표시합니다.
 * </p>
 */
public class ScoreboardHUD {

    private final DreamWorkCore plugin;
    private final JobManager jobManager;
    private final StorageManager storageManager;
    private final StatManager statManager;

    /** 플레이어별 스코어보드 */
    private final Map<UUID, Scoreboard> playerScoreboards = new ConcurrentHashMap<>();

    public ScoreboardHUD(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.storageManager = plugin.getStorageManager();
        this.statManager = plugin.getStatManager();
    }

    /**
     * HUD 업데이트 태스크를 시작합니다.
     */
    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1초마다 갱신

        plugin.getLogger().info("ScoreboardHUD 시작됨");
    }

    /**
     * 플레이어의 스코어보드를 업데이트합니다.
     */
    public void updateScoreboard(Player player) {
        Scoreboard scoreboard = getOrCreateScoreboard(player);
        Objective objective = scoreboard.getObjective("dreamwork");

        if (objective == null) {
            objective = scoreboard.registerNewObjective("dreamwork", Criteria.DUMMY, Component.text("§6§lDreamWork"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // 기존 점수 초기화 (Anti-flicker)
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // 데이터 수집
        UUID uuid = player.getUniqueId();
        UserJobData jobData = jobManager.getUserJob(uuid);
        UserData userData = storageManager.getUserData(uuid);

        if (userData == null)
            return; // 데이터 로드 전이면 패스

        int line = 10;

        // 공백
        setScore(objective, "§7§m          ", line--);

        // 직업 정보
        String jobName = "없음";
        int level = 0;
        double exp = 0;
        double maxExp = 0;

        if (jobData != null && jobData.hasJob()) {
            var job = jobManager.getJob(jobData.getJobId());
            if (job != null) {
                jobName = job.getDisplayName();
                level = jobData.getLevel();
                exp = jobData.getCurrentExp();
                maxExp = jobManager.getRequiredExp(level);
            }
        }

        setScore(objective, "§e직업: §f" + jobName, line--);
        setScore(objective, "§e레벨: §f" + level, line--);
        if (maxExp > 0) {
            setScore(objective, "§e경험치: §f" + String.format("%.0f", exp) + "§7/§f" + String.format("%.0f", maxExp),
                    line--);
        } else {
            setScore(objective, "§e경험치: §f-", line--);
        }

        // 공백
        setScore(objective, "§8§m          ", line--);

        // 자원 정보
        double mana = userData.getCurrentMana();

        // Max Mana Calculation: 100 + (INT * 2)
        int totalInt = 0;
        StatManager.PlayerStats stats = statManager.getStats(uuid);
        if (stats != null) {
            totalInt = stats.getInt();
        }

        double maxMana = 100 + (totalInt * 2);
        setScore(objective, "§b기력: §f" + (int) mana + "§7/§f" + (int) maxMana, line--);

        // 경제 정보 (Vault 연동 시)
        // TODO: Vault 연동

        // 공백
        setScore(objective, "§f§m          ", line--);

        // 서버 정보
        setScore(objective, "§7play.dreamwork.mc", line--);
    }

    private void setScore(Objective objective, String text, int score) {
        if (text.length() > 40)
            text = text.substring(0, 40); // Length safe
        objective.getScore(text).setScore(score);
    }

    private Scoreboard getOrCreateScoreboard(Player player) {
        return playerScoreboards.computeIfAbsent(player.getUniqueId(), uuid -> {
            Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(sb);
            return sb;
        });
    }

    /**
     * 플레이어의 스코어보드를 제거합니다.
     */
    public void removeScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
