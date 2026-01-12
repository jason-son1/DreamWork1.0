package com.dreamwork.core.hook;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI 확장 등록
 * <p>
 * DreamWork 플러그인의 플레이스홀더를 제공합니다.
 * </p>
 * 
 * <h2>사용 가능한 플레이스홀더:</h2>
 * <ul>
 * <li>%dreamwork_job_miner_level% - 광부 레벨</li>
 * <li>%dreamwork_job_farmer_level% - 농부 레벨</li>
 * <li>%dreamwork_job_fisher_level% - 어부 레벨</li>
 * <li>%dreamwork_job_hunter_level% - 사냥꾼 레벨</li>
 * <li>%dreamwork_job_explorer_level% - 탐험가 레벨</li>
 * <li>%dreamwork_total_level% - 총 직업 레벨</li>
 * <li>%dreamwork_rank% - 플레이어 등급</li>
 * </ul>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class DreamWorkPlaceholders extends PlaceholderExpansion {

    private final DreamWorkCore plugin;

    public DreamWorkPlaceholders(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dreamwork";
    }

    @Override
    public @NotNull String getAuthor() {
        return "DreamWork Team";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) {
            return "";
        }

        UserData userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null) {
            return "";
        }

        // 직업 레벨: job_<jobtype>_level
        if (params.startsWith("job_") && params.endsWith("_level")) {
            String jobKey = params.substring(4, params.length() - 6); // "job_" ~ "_level"
            JobType jobType = JobType.fromConfigKey(jobKey);
            if (jobType != null) {
                return String.valueOf(userData.getJobLevel(jobType));
            }
        }

        // 직업 경험치: job_<jobtype>_exp
        if (params.startsWith("job_") && params.endsWith("_exp")) {
            String jobKey = params.substring(4, params.length() - 4);
            JobType jobType = JobType.fromConfigKey(jobKey);
            if (jobType != null) {
                return String.format("%.0f", userData.getJobExp(jobType));
            }
        }

        // 총 레벨
        if (params.equals("total_level")) {
            return String.valueOf(userData.getTotalJobLevel());
        }

        // 등급
        if (params.equals("rank")) {
            return getRank(userData.getTotalJobLevel());
        }

        // 가장 높은 레벨 직업
        if (params.equals("highest_job")) {
            return userData.getHighestLevelJob().getDisplayName();
        }

        // 스탯
        if (params.equals("str"))
            return String.valueOf(userData.getStr());
        if (params.equals("dex"))
            return String.valueOf(userData.getDex());
        if (params.equals("con"))
            return String.valueOf(userData.getCon());
        if (params.equals("int"))
            return String.valueOf(userData.getIntel());
        if (params.equals("luk"))
            return String.valueOf(userData.getLuk());

        return null;
    }

    private String getRank(int totalLevel) {
        if (totalLevel >= 500)
            return "전설";
        if (totalLevel >= 400)
            return "영웅";
        if (totalLevel >= 300)
            return "숙련자";
        if (totalLevel >= 200)
            return "숙련공";
        if (totalLevel >= 100)
            return "견습생";
        return "초보자";
    }
}
