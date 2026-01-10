package com.dreamwork.core.skill;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.storage.StorageManager;
import com.dreamwork.core.storage.UserData;
import org.bukkit.entity.Player;

/**
 * 스킬 발동 조건 체커
 * 
 * <p>
 * 쿨타임, 마나 등 스킬 발동 조건을 체크합니다.
 * </p>
 */
public class SkillConditionChecker {

    private final DreamWorkCore plugin;
    private final SkillManager skillManager;
    private final StorageManager storageManager;

    public SkillConditionChecker(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.skillManager = plugin.getSkillManager();
        this.storageManager = plugin.getStorageManager();
    }

    /**
     * 마나가 충분한지 확인합니다.
     */
    public boolean hasEnoughMana(Player player, SkillEffect skill) {
        UserData data = storageManager.getUserData(player.getUniqueId());
        return data.getCurrentMana() >= skill.getManaCost();
    }

    /**
     * 쿨타임이 끝났는지 확인합니다.
     */
    public boolean isCooldownReady(Player player, SkillEffect skill) {
        return !skillManager.isOnCooldown(player, skill.getId());
    }

    /**
     * 모든 조건을 종합 체크합니다.
     */
    public boolean canUseSkill(Player player, SkillEffect skill) {
        if (!hasEnoughMana(player, skill)) {
            player.sendMessage("§c[스킬] 기력이 부족합니다. (필요: " + skill.getManaCost() + ")");
            return false;
        }

        if (!isCooldownReady(player, skill)) {
            long remaining = skillManager.getRemainingCooldown(player, skill.getId());
            player.sendMessage("§c[스킬] 쿨타임 중입니다. (" + remaining + "초)");
            return false;
        }

        return true;
    }

    /**
     * 마나를 소모합니다.
     */
    public void consumeMana(Player player, SkillEffect skill) {
        UserData data = storageManager.getUserData(player.getUniqueId());
        double current = data.getCurrentMana();
        data.setCurrentMana(current - skill.getManaCost());
    }
}
