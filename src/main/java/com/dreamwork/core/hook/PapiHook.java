package com.dreamwork.core.hook;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.JobProvider;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.stat.StatManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI 등록 래퍼
 * 
 * <p>
 * DreamWork Core의 데이터를 PlaceholderAPI를 통해 다른 플러그인에서 사용할 수 있도록 합니다.
 * </p>
 * 
 * <h2>지원하는 Placeholder:</h2>
 * <ul>
 * <li>{@code %dreamwork_job%} - 현재 직업</li>
 * <li>{@code %dreamwork_level%} - 직업 레벨</li>
 * <li>{@code %dreamwork_exp%} - 현재 경험치</li>
 * <li>{@code %dreamwork_exp_percent%} - 경험치 퍼센트</li>
 * <li>{@code %dreamwork_str%} - 힘 스탯</li>
 * <li>{@code %dreamwork_dex%} - 민첩 스탯</li>
 * <li>{@code %dreamwork_con%} - 체력 스탯</li>
 * <li>{@code %dreamwork_int%} - 지능 스탯</li>
 * <li>{@code %dreamwork_luck%} - 행운 스탯</li>
 * </ul>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class PapiHook extends PlaceholderExpansion {

    private final DreamWorkCore plugin;

    /**
     * PapiHook 생성자
     * 
     * @param plugin 플러그인 인스턴스
     */
    public PapiHook(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dreamwork";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        // 플러그인 리로드 시에도 유지
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        // 오프라인 플레이어 처리
        if (offlinePlayer == null) {
            return "";
        }

        // 온라인 플레이어인 경우 더 많은 정보 제공
        if (offlinePlayer.isOnline()) {
            Player player = offlinePlayer.getPlayer();
            if (player != null) {
                return onPlaceholderRequest(player, params);
            }
        }

        // 오프라인 플레이어는 기본값 반환
        return handleOfflineRequest(offlinePlayer, params);
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // 소문자로 변환하여 대소문자 무시
        String param = params.toLowerCase();

        // TODO: 2단계에서 실제 데이터와 연동
        // 현재는 스켈레톤이므로 기본값 반환

        return switch (param) {
            // 직업 관련
            case "job" -> getPlayerJob(player);
            case "level" -> getPlayerLevel(player);
            case "exp" -> getPlayerExp(player);
            case "exp_percent" -> getPlayerExpPercent(player);

            // 스탯 관련
            case "str" -> getPlayerStat(player, "str");
            case "dex" -> getPlayerStat(player, "dex");
            case "con" -> getPlayerStat(player, "con");
            case "int" -> getPlayerStat(player, "int");
            case "luck" -> getPlayerStat(player, "luck");

            // 포인트
            case "points" -> getPlayerStatPoints(player);

            default -> null; // 알 수 없는 placeholder
        };
    }

    /**
     * 오프라인 플레이어 요청 처리
     * 
     * @param player 오프라인 플레이어
     * @param params placeholder 파라미터
     * @return placeholder 값
     */
    private String handleOfflineRequest(OfflinePlayer player, String params) {
        // 오프라인 플레이어는 기본값 반환
        return "0";
    }

    /**
     * 플레이어의 현재 직업을 반환합니다.
     * 
     * @param player 플레이어
     * @return 직업 이름
     */
    private String getPlayerJob(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return "없음";

        UserJobData data = jobManager.getUserJob(player.getUniqueId());
        if (!data.hasJob())
            return "없음";

        JobProvider job = jobManager.getJob(data.getJobId());
        return job != null ? job.getDisplayName() : data.getJobId();
    }

    /**
     * 플레이어의 직업 레벨을 반환합니다.
     * 
     * @param player 플레이어
     * @return 레벨
     */
    private String getPlayerLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return "0";

        UserJobData data = jobManager.getUserJob(player.getUniqueId());
        return String.valueOf(data.getLevel());
    }

    /**
     * 플레이어의 현재 경험치를 반환합니다.
     * 
     * @param player 플레이어
     * @return 경험치
     */
    private String getPlayerExp(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return "0";

        UserJobData data = jobManager.getUserJob(player.getUniqueId());
        return String.format("%.0f", data.getCurrentExp());
    }

    /**
     * 플레이어의 경험치 퍼센트를 반환합니다.
     * 
     * @param player 플레이어
     * @return 경험치 퍼센트 (0~100)
     */
    private String getPlayerExpPercent(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return "0.0";

        UserJobData data = jobManager.getUserJob(player.getUniqueId());
        if (!data.hasJob())
            return "0.0";

        JobProvider job = jobManager.getJob(data.getJobId());
        if (job == null)
            return "0.0";

        double required = job.getExpForLevel(data.getLevel() + 1);
        double percent = (data.getCurrentExp() / required) * 100;
        return String.format("%.1f", Math.min(percent, 100.0));
    }

    /**
     * 플레이어의 특정 스탯을 반환합니다.
     * 
     * @param player   플레이어
     * @param statName 스탯 이름
     * @return 스탯 값
     */
    private String getPlayerStat(Player player, String statName) {
        StatManager statManager = plugin.getStatManager();
        if (statManager == null)
            return "0";

        StatManager.PlayerStats stats = statManager.getStats(player);
        return switch (statName.toLowerCase()) {
            case "str" -> String.valueOf(stats.getStr());
            case "dex" -> String.valueOf(stats.getDex());
            case "con" -> String.valueOf(stats.getCon());
            case "int" -> String.valueOf(stats.getInt());
            case "luck" -> String.valueOf(stats.getLuck());
            default -> "0";
        };
    }

    /**
     * 플레이어의 남은 스탯 포인트를 반환합니다.
     * 
     * @param player 플레이어
     * @return 스탯 포인트
     */
    private String getPlayerStatPoints(Player player) {
        StatManager statManager = plugin.getStatManager();
        if (statManager == null)
            return "0";

        StatManager.PlayerStats stats = statManager.getStats(player);
        return String.valueOf(stats.getStatPoints());
    }
}
