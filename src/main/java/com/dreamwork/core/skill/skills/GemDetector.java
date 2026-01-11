package com.dreamwork.core.skill.skills;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.skill.SkillEffect;
import org.bukkit.entity.Player;

/**
 * [광부] 보석 탐지 (Passive)
 * - 돌 채광 시 희귀 보석 드롭 확률 증가
 */
public class GemDetector implements SkillEffect {

    private final DreamWorkCore plugin;

    public GemDetector(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        // 패시브: JobActivityListener나 Trigger 로직에서 처리
    }

    @Override
    public String getId() {
        return "gem_detector";
    }

    @Override
    public String getName() {
        return "보석 탐지";
    }

    @Override
    public String getDescription() {
        return "돌 채광 시 0.1% 확률로 랜덤 보석을 발견합니다.";
    }

    @Override
    public int getCooldown() {
        return 0;
    }

    @Override
    public int getManaCost() {
        return 0;
    }

    @Override
    public int getRequiredLevel() {
        return 80;
    }

    @Override
    public String getRequiredJob() {
        return "miner";
    }
}
