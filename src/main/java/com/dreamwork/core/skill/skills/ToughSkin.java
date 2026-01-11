package com.dreamwork.core.skill.skills;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.skill.SkillEffect;
import org.bukkit.entity.Player;

/**
 * [광부] 단단한 피부 (Passive)
 * - 받는 물리 데미지 감소
 */
public class ToughSkin implements SkillEffect {

    private final DreamWorkCore plugin;

    public ToughSkin(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        // 패시브 스킬이므로 발동 시 아무런 행동도 하지 않음
        // 효과는 CombatListener/CombatMechanic에서 적용
    }

    @Override
    public String getId() {
        return "tough_skin";
    }

    @Override
    public String getName() {
        return "단단한 피부";
    }

    @Override
    public String getDescription() {
        return "받는 물리 데미지가 5% 감소합니다.";
    }

    @Override
    public int getCooldown() {
        return 0; // 패시브
    }

    @Override
    public int getManaCost() {
        return 0; // 패시브
    }

    @Override
    public int getRequiredLevel() {
        return 10;
    }

    @Override
    public String getRequiredJob() {
        return "miner";
    }
}
