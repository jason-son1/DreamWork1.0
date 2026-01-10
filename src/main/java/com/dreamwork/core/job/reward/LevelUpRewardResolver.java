package com.dreamwork.core.job.reward;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.stat.StatManager;
import org.bukkit.entity.Player;

/**
 * 레벨업 보상 처리기
 * 
 * <p>
 * 레벨업 시 스탯 포인트, 스킬 해금, 칭호 지급을 처리합니다.
 * </p>
 */
public class LevelUpRewardResolver {

    private final DreamWorkCore plugin;
    private final StatManager statManager;

    public LevelUpRewardResolver(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.statManager = plugin.getStatManager();
    }

    /**
     * 스탯 포인트를 지급합니다.
     */
    public void giveStatPoints(Player player, int amount) {
        StatManager.PlayerStats stats = statManager.getStats(player);
        stats.addStatPoints(amount);

        player.sendMessage("§a[레벨업 보상] §f스탯 포인트 §e+" + amount + " §f획득!");

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Reward] " + player.getName() + " 스탯 포인트 +" + amount);
        }
    }

    /**
     * 스킬을 해금합니다.
     */
    public void unlockSkill(Player player, String skillId) {
        // TODO: SkillManager와 연동하여 스킬 해금
        // 현재는 메시지만 표시
        player.sendMessage("§a[레벨업 보상] §f스킬 §b" + skillId + " §f해금!");

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Reward] " + player.getName() + " 스킬 해금: " + skillId);
        }
    }

    /**
     * 칭호를 지급합니다.
     */
    public void giveTitle(Player player, String titleId) {
        // TODO: 칭호 시스템과 연동
        // 현재는 메시지만 표시
        player.sendMessage("§a[레벨업 보상] §f칭호 §d" + titleId + " §f획득!");

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Reward] " + player.getName() + " 칭호 획득: " + titleId);
        }
    }

    /**
     * 레벨업 시 모든 보상을 처리합니다.
     */
    public void processLevelUp(Player player, int newLevel) {
        // 레벨당 스탯 포인트
        int statPointsPerLevel = plugin.getConfig().getInt("stats.points-per-level", 3);
        giveStatPoints(player, statPointsPerLevel);

        // 특정 레벨 스킬 해금 (예시)
        if (newLevel == 5) {
            unlockSkill(player, "miner_blast");
        }
        if (newLevel == 10) {
            unlockSkill(player, "double_harvest");
        }

        // 특정 레벨 칭호 (예시)
        if (newLevel == 10) {
            giveTitle(player, "견습 광부");
        }
        if (newLevel == 25) {
            giveTitle(player, "숙련 광부");
        }
        if (newLevel == 50) {
            giveTitle(player, "마스터 광부");
        }
    }
}
