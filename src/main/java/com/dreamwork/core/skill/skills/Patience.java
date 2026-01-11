package com.dreamwork.core.skill.skills;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.skill.SkillEffect;
import org.bukkit.entity.Player;

/**
 * [어부] 인내심 (Passive)
 * - 낚시 입질 시간 감소 등
 */
public class Patience implements SkillEffect {

    private final DreamWorkCore plugin;

    public Patience(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        // 패시브
    }

    @Override
    public String getId() {
        return "patience";
    }

    @Override
    public String getName() {
        return "인내심";
    }

    @Override
    public String getDescription() {
        return "낚시 찌를 무는 시간이 10% 단축됩니다.";
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
        return 10;
    }

    @Override
    public String getRequiredJob() {
        return "fisher";
    }
}
