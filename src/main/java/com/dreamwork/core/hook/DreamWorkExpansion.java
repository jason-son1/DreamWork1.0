package com.dreamwork.core.hook;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.stat.StatManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI 확장
 * 
 * <h2>지원 플레이스홀더:</h2>
 * <ul>
 * <li>%dw_stat_str% - 힘 스탯</li>
 * <li>%dw_stat_dex% - 민첩 스탯</li>
 * <li>%dw_stat_con% - 체력 스탯</li>
 * <li>%dw_stat_int% - 지능 스탯</li>
 * <li>%dw_stat_luck% - 행운 스탯</li>
 * <li>%dw_stat_points% - 남은 스탯 포인트</li>
 * <li>%dw_job_name% - 직업 이름</li>
 * <li>%dw_job_level% - 직업 레벨</li>
 * <li>%dw_job_exp% - 현재 경험치</li>
 * <li>%dw_job_exp_req% - 필요 경험치</li>
 * <li>%dw_job_exp_percent% - 경험치 퍼센트</li>
 * <li>%dw_mana% - 현재 마나</li>
 * <li>%dw_mana_max% - 최대 마나</li>
 * </ul>
 */
public class DreamWorkExpansion extends PlaceholderExpansion {

    private final DreamWorkCore plugin;

    public DreamWorkExpansion(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dw";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getPluginMeta().getAuthors().toString();
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
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null)
            return "";

        StatManager statManager = plugin.getStatManager();
        JobManager jobManager = plugin.getJobManager();

        // 스탯 관련
        if (params.startsWith("stat_")) {
            StatManager.PlayerStats stats = statManager.getStats(player);

            return switch (params) {
                case "stat_str" -> String.valueOf(stats.getStr());
                case "stat_dex" -> String.valueOf(stats.getDex());
                case "stat_con" -> String.valueOf(stats.getCon());
                case "stat_int" -> String.valueOf(stats.getInt());
                case "stat_luck" -> String.valueOf(stats.getLuck());
                case "stat_points" -> String.valueOf(stats.getStatPoints());
                default -> null;
            };
        }

        // 직업 관련
        if (params.startsWith("job_")) {
            UserJobData jobData = jobManager.getUserJob(player.getUniqueId());

            return switch (params) {
                case "job_name" -> {
                    if (!jobData.hasJob())
                        yield "없음";
                    var job = jobManager.getJob(jobData.getJobId());
                    yield job != null ? job.getDisplayName() : "알 수 없음";
                }
                case "job_id" -> jobData.hasJob() ? jobData.getJobId() : "none";
                case "job_level" -> String.valueOf(jobData.getLevel());
                case "job_exp" -> String.format("%.0f", jobData.getCurrentExp());
                case "job_exp_req" -> {
                    if (!jobData.hasJob())
                        yield "0";
                    yield String.valueOf(jobManager.getRequiredExp(jobData.getLevel()));
                }
                case "job_exp_percent" -> {
                    if (!jobData.hasJob())
                        yield "0";
                    double required = jobManager.getRequiredExp(jobData.getLevel());
                    double percent = (jobData.getCurrentExp() / required) * 100;
                    yield String.format("%.1f", percent);
                }
                default -> null;
            };
        }

        // 마나 관련
        if (params.startsWith("mana")) {
            var skillManager = plugin.getSkillManager();
            if (skillManager == null)
                return "0";

            return switch (params) {
                case "mana" -> String.valueOf(skillManager.getMana(player));
                case "mana_max" -> String.valueOf(skillManager.getMaxMana());
                default -> null;
            };
        }

        return null;
    }
}
