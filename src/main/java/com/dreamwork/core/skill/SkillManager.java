package com.dreamwork.core.skill;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.manager.Manager;
import com.dreamwork.core.skill.skills.Dash;
import com.dreamwork.core.skill.skills.Adrenaline;
import com.dreamwork.core.skill.skills.GoldenHook;
import com.dreamwork.core.skill.skills.MinerBlast;
import com.dreamwork.core.skill.skills.SuperHeat;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 스킬 시스템 매니저
 * 
 * <p>
 * 스킬 등록, 쿨타임 관리, 발동을 담당합니다.
 * </p>
 */
public class SkillManager extends Manager {

    private final DreamWorkCore plugin;

    /** 등록된 스킬 (skillId -> SkillEffect) */
    private final Map<String, SkillEffect> skills = new HashMap<>();

    /** 플레이어 쿨타임 (UUID -> (skillId -> endTime)) */
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    /** 플레이어 마나 (UUID -> mana) */
    private final Map<UUID, Integer> playerMana = new ConcurrentHashMap<>();

    /** 최대 마나 */
    private int maxMana = 100;

    public SkillManager(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        loadConfig();
        registerDefaultSkills();

        // 마나 재생 태스크
        startManaRegenTask();

        enabled = true;
        plugin.getLogger().info("SkillManager 활성화 완료! 스킬: " + skills.size() + "개");
    }

    @Override
    public void onDisable() {
        enabled = false;
        skills.clear();
        cooldowns.clear();
        playerMana.clear();
    }

    @Override
    public void reload() {
        loadConfig();
        plugin.getLogger().info("SkillManager 리로드 완료!");
    }

    private void loadConfig() {
        this.maxMana = plugin.getConfig().getInt("skill.max-mana", 100);
    }

    /**
     * 기본 스킬을 등록합니다.
     */
    private void registerDefaultSkills() {
        // 광부 스킬
        registerSkill(new MinerBlast(plugin));
        registerSkill(new SuperHeat(plugin));
        registerSkill(new com.dreamwork.core.skill.skills.ToughSkin(plugin));
        registerSkill(new com.dreamwork.core.skill.skills.GemDetector(plugin));

        // 낚시꾼 스킬
        registerSkill(new GoldenHook(plugin));
        registerSkill(new com.dreamwork.core.skill.skills.Patience(plugin));

        // 사냥꾼 스킬
        registerSkill(new Adrenaline(plugin));
        registerSkill(new com.dreamwork.core.skill.skills.HeadHunter(plugin));

        // 공용 스킬
        registerSkill(new Dash(plugin));
    }

    /**
     * 스킬을 등록합니다.
     */
    public void registerSkill(SkillEffect skill) {
        skills.put(skill.getId(), skill);
    }

    /**
     * 스킬 사용 가능 여부를 확인합니다.
     * 
     * @param player  플레이어
     * @param skillId 스킬 ID
     * @return 사용 가능 여부
     */
    public boolean canUseSkill(Player player, String skillId) {
        SkillEffect skill = skills.get(skillId);
        if (skill == null)
            return false;

        // 쿨타임 체크
        if (isOnCooldown(player, skillId)) {
            long remaining = getRemainingCooldown(player, skillId);
            player.sendMessage("§c[스킬] 쿨타임 중입니다. (" + remaining + "초)");
            return false;
        }

        // 마나 체크
        int currentMana = getMana(player);
        if (currentMana < skill.getManaCost()) {
            player.sendMessage("§c[스킬] 기력이 부족합니다. (필요: " + skill.getManaCost() + ")");
            return false;
        }

        return hasSkill(player, skillId);
    }

    /**
     * 플레이어가 해당 스킬을 보유하고 있는지 확인합니다.
     * (직업 및 레벨 조건 충족 여부 확인)
     */
    public boolean hasSkill(Player player, String skillId) {
        SkillEffect skill = skills.get(skillId);
        if (skill == null)
            return false;

        // 직업 체크
        if (skill.getRequiredJob() != null) {
            JobManager jobManager = plugin.getJobManager();
            UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
            if (!jobData.hasJob() || !jobData.getJobId().equals(skill.getRequiredJob())) {
                return false;
            }

            // 레벨 체크
            if (jobData.getLevel() < skill.getRequiredLevel()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 스킬을 발동합니다.
     * 
     * @param player  플레이어
     * @param skillId 스킬 ID
     * @return 발동 성공 여부
     */
    public boolean triggerSkill(Player player, String skillId) {
        if (!canUseSkill(player, skillId)) {
            return false;
        }

        SkillEffect skill = skills.get(skillId);

        // 마나 소모
        consumeMana(player, skill.getManaCost());

        // 쿨타임 시작
        startCooldown(player, skillId, skill.getCooldown());

        // 스킬 실행
        skill.execute(player);

        player.sendMessage("§a[스킬] §f" + skill.getName() + " §a발동!");

        return true;
    }

    /**
     * 쿨타임을 시작합니다.
     */
    public void startCooldown(Player player, String skillId, int seconds) {
        long endTime = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(skillId, endTime);
    }

    /**
     * 쿨타임 중인지 확인합니다.
     */
    public boolean isOnCooldown(Player player, String skillId) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null)
            return false;

        Long endTime = playerCooldowns.get(skillId);
        if (endTime == null)
            return false;

        return System.currentTimeMillis() < endTime;
    }

    /**
     * 남은 쿨타임(초)을 반환합니다.
     */
    public long getRemainingCooldown(Player player, String skillId) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null)
            return 0;

        Long endTime = playerCooldowns.get(skillId);
        if (endTime == null)
            return 0;

        long remaining = (endTime - System.currentTimeMillis()) / 1000L;
        return Math.max(0, remaining);
    }

    // ==================== 마나 관리 ====================

    public int getMana(Player player) {
        return playerMana.getOrDefault(player.getUniqueId(), maxMana);
    }

    public void setMana(Player player, int mana) {
        playerMana.put(player.getUniqueId(), Math.min(mana, maxMana));
    }

    public void consumeMana(Player player, int amount) {
        int current = getMana(player);
        setMana(player, Math.max(0, current - amount));
    }

    public void restoreMana(Player player, int amount) {
        int current = getMana(player);
        setMana(player, Math.min(maxMana, current + amount));
    }

    public int getMaxMana() {
        return maxMana;
    }

    private void startManaRegenTask() {
        int regenAmount = plugin.getConfig().getInt("skill.mana-regen-amount", 5);
        int regenInterval = plugin.getConfig().getInt("skill.mana-regen-interval", 60); // 틱

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    restoreMana(player, regenAmount);
                }
            }
        }.runTaskTimer(plugin, regenInterval, regenInterval);
    }

    // ==================== Getters ====================

    public SkillEffect getSkill(String skillId) {
        return skills.get(skillId);
    }

    public Collection<SkillEffect> getAllSkills() {
        return Collections.unmodifiableCollection(skills.values());
    }
}
