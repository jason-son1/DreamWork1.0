package com.dreamwork.core.skill.skills;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.skill.SkillEffect;
import org.bukkit.entity.Player;

/**
 * [사냥꾼] 헤드헌터 (Passive)
 * - 몬스터 처치 시 머리 드롭
 */
public class HeadHunter implements SkillEffect {

    private final DreamWorkCore plugin;

    public HeadHunter(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        // 패시브
    }

    @Override
    public String getId() {
        return "head_hunter";
    }

    @Override
    public String getName() {
        return "헤드헌터";
    }

    @Override
    public String getDescription() {
        return "몬스터 처치 시 5% 확률로 머리를 획득합니다.";
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
        return 50;
    }

    @Override
    public String getRequiredJob() {
        return "hunter";
    }
}
