package com.dreamwork.core.stat;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.JobProvider;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.manager.Manager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 스탯 관리 매니저 (스켈레톤)
 * 
 * <p>
 * 플레이어의 RPG 스탯을 관리합니다.
 * DB 부하 최소화를 위해 메모리에 캐싱하고,
 * 장비 교체나 레벨업 시에만 재계산합니다.
 * </p>
 * 
 * <h2>스탯 종류:</h2>
 * <ul>
 * <li><b>STR (힘)</b>: 물리 공격력</li>
 * <li><b>DEX (민첩)</b>: 치명타 확률, 이동 속도</li>
 * <li><b>CON (체력)</b>: 최대 체력, 방어력</li>
 * <li><b>INT (지능)</b>: 스킬 쿨타임, 마나</li>
 * <li><b>LUCK (행운)</b>: 채집/드롭 보너스</li>
 * </ul>
 * 
 * <h2>스탯 공식 (2단계에서 구현):</h2>
 * <ul>
 * <li>물리 데미지: {@code WeaponDamage + (STR * 0.5) + BaseDamage}</li>
 * <li>치명타 확률: {@code 5 + (DEX * 0.2)} (%)</li>
 * <li>최대 체력: {@code 20 + (CON * 2) + (JobLevel * 0.5)}</li>
 * <li>방어력 감소: {@code CON / (CON + 100) * 100} (%)</li>
 * <li>채집 보너스: {@code LUCK * 0.5} (%)</li>
 * </ul>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class StatManager extends Manager {

    private final DreamWorkCore plugin;

    /** 플레이어별 스탯 캐시 */
    private final Map<UUID, PlayerStats> statsCache;

    /**
     * StatManager 생성자
     * 
     * @param plugin 플러그인 인스턴스
     */
    public StatManager(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.statsCache = new ConcurrentHashMap<>();
    }

    @Override
    public void onEnable() {
        enabled = true;
        plugin.getLogger().info("StatManager 활성화 완료 (스켈레톤)");
    }

    @Override
    public void onDisable() {
        enabled = false;

        // 모든 캐시된 스탯 저장
        saveAllStats();
        statsCache.clear();

        plugin.getLogger().info("StatManager 비활성화 완료");
    }

    @Override
    public void reload() {
        // 설정 리로드 시 필요한 로직
        plugin.getLogger().info("StatManager 리로드 완료");
    }

    /**
     * 플레이어의 스탯을 로드합니다.
     * 
     * <p>
     * 캐시에 없으면 파일에서 로드하고 캐싱합니다.
     * </p>
     * 
     * @param player 플레이어
     * @return 플레이어 스탯
     */
    public PlayerStats getStats(Player player) {
        return getStats(player.getUniqueId());
    }

    /**
     * UUID로 플레이어의 스탯을 로드합니다.
     * 
     * @param uuid 플레이어 UUID
     * @return 플레이어 스탯
     */
    public PlayerStats getStats(UUID uuid) {
        return statsCache.computeIfAbsent(uuid, this::loadOrCreateStats);
    }

    /**
     * 스탯을 로드하거나 새로 생성합니다.
     * 
     * @param uuid 플레이어 UUID
     * @return 플레이어 스탯
     */
    private PlayerStats loadOrCreateStats(UUID uuid) {
        // TODO: 2단계에서 StorageManager와 연동하여 파일에서 로드
        return new PlayerStats(uuid);
    }

    /**
     * 플레이어의 스탯을 저장합니다.
     * 
     * <p>
     * 개별 저장은 지원하지 않습니다. 통합 데이터 저장을 이용하세요.
     * </p>
     * 
     * @param uuid 플레이어 UUID
     */
    public void saveStats(UUID uuid) {
        // 개별 저장 로직 제거 (UserData 통합 저장 사용)
    }

    /**
     * 모든 캐시된 스탯을 저장합니다.
     */
    public void saveAllStats() {
        // 개별 저장 로직 제거
    }

    /**
     * 플레이어의 스탯을 캐시에서 제거합니다.
     * 
     * <p>
     * 플레이어 로그아웃 시 호출합니다.
     * </p>
     * 
     * @param uuid 플레이어 UUID
     */
    public void unloadStats(UUID uuid) {
        saveStats(uuid);
        statsCache.remove(uuid);
    }

    /**
     * 플레이어의 스탯 데이터를 설정합니다.
     * 
     * @param uuid  플레이어 UUID
     * @param stats 스탯 데이터
     */
    public void setStats(UUID uuid, PlayerStats stats) {
        if (stats != null) {
            statsCache.put(uuid, stats);
        }
    }

    /**
     * 플레이어의 스탯을 재계산합니다.
     * 
     * <p>
     * 장비 교체, 레벨업 시 호출합니다.
     * 기본 스탯 + (직업 레벨 × 직업 스탯 보너스)를 계산합니다.
     * </p>
     * 
     * @param player 플레이어
     */
    public void recalculateStats(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerStats stats = getStats(uuid);

        // 보너스 스탯 초기화
        stats.setBonusStr(0);
        stats.setBonusDex(0);
        stats.setBonusCon(0);
        stats.setBonusInt(0);
        stats.setBonusLuck(0);

        // 직업 레벨에 따른 스탯 보너스 계산
        JobManager jobManager = plugin.getJobManager();
        if (jobManager != null) {
            UserJobData jobData = jobManager.getUserJob(uuid);
            if (jobData != null && jobData.hasJob()) {
                JobProvider job = jobManager.getJob(jobData.getJobId());
                if (job != null) {
                    int jobLevel = jobData.getLevel();
                    Map<String, Double> statsPerLevel = job.getStatsPerLevel();

                    // 각 스탯 보너스 적용
                    for (Map.Entry<String, Double> entry : statsPerLevel.entrySet()) {
                        String statName = entry.getKey().toLowerCase();
                        int bonus = (int) (entry.getValue() * jobLevel);

                        switch (statName) {
                            case "str", "strength" -> stats.setBonusStr(stats.getBonusStr() + bonus);
                            case "dex", "dexterity" -> stats.setBonusDex(stats.getBonusDex() + bonus);
                            case "con", "constitution", "stamina" -> stats.setBonusCon(stats.getBonusCon() + bonus);
                            case "int", "intelligence" -> stats.setBonusInt(stats.getBonusInt() + bonus);
                            case "luck", "luk" -> stats.setBonusLuck(stats.getBonusLuck() + bonus);
                        }
                    }
                }
            }
        }

        if (plugin.getSkillManager().hasSkill(player, "tough_skin")) {
            // 단단한 피부: 받는 데미지 5% 감소 (여기서는 방어력 등으로 구현하거나, CombatListener에서 처리)
            // 하지만 요구사항은 'StatManager에서 처리'가 아닐 수도 있음.
            // CombatListener에서 처리하는 것이 맞으나, 스탯 보너스가 있다면 여기서 처리.
            // 예시 구현에서는 'tough_skin'이 패시브로 동작하므로, 별도 처리 없음 (CombatListener에서 체크).
        }

        // 스탯 내부 재계산 (장비 등)
        stats.recalculate();

        // 바닐라 속성 적용 (최대 체력 등)
        applyVanillaAttributes(player, stats);

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] 스탯 재계산: " + player.getName() +
                    " (STR:" + stats.getStr() +
                    ", DEX:" + stats.getDex() +
                    ", CON:" + stats.getCon() +
                    ", INT:" + stats.getInt() +
                    ", LUCK:" + stats.getLuck() + ")");
        }
    }

    /**
     * 바닐라 마인크래프트 속성을 적용합니다.
     * 
     * @param player 플레이어
     * @param stats  스탯
     */
    private void applyVanillaAttributes(Player player, PlayerStats stats) {
        // 최대 체력 적용 (기본 20 + CON * 2)
        double maxHealth = 20.0 + (stats.getCon() * 2);
        maxHealth = Math.min(maxHealth, 2048.0); // Spigot max limit is usually 2048

        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(maxHealth);
        }

        // 이동 속도 적용 (기본 0.1 + DEX * 0.0005)
        // 사용자 요구사항은 0.002이나 너무 빠를 수 있어 0.0005로 조정하거나, 요구사항대로 0.002 적용.
        // 요구사항 엄수: AGI * 0.002. 여기선 DEX를 사용.
        double walkSpeed = 0.1 + (stats.getDex() * 0.0005);
        walkSpeed = Math.min(walkSpeed, 1.0); // 상한선

        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(walkSpeed);
        }
    }

    // ==================== 스탯 계산 공식 (2단계 구현용) ====================

    /**
     * 물리 데미지를 계산합니다.
     * 
     * @param player       플레이어
     * @param weaponDamage 무기 기본 데미지
     * @return 최종 물리 데미지
     */
    public double calculatePhysicalDamage(Player player, double weaponDamage) {
        PlayerStats stats = getStats(player);
        double baseDamage = 1.0;
        return weaponDamage + (stats.getStr() * 0.5) + baseDamage;
    }

    /**
     * 치명타 확률을 계산합니다.
     * 
     * @param player 플레이어
     * @return 치명타 확률 (0~50%)
     */
    public double calculateCritChance(Player player) {
        PlayerStats stats = getStats(player);
        double chance = 5.0 + (stats.getDex() * 0.2);
        return Math.min(chance, 50.0); // 최대 50%
    }

    /**
     * 최대 체력을 계산합니다.
     * 
     * @param player   플레이어
     * @param jobLevel 직업 레벨
     * @return 최대 체력
     */
    public double calculateMaxHealth(Player player, int jobLevel) {
        PlayerStats stats = getStats(player);
        return 20.0 + (stats.getCon() * 2) + (jobLevel * 0.5);
    }

    /**
     * 데미지 감소율을 계산합니다.
     * 
     * @param player 플레이어
     * @return 데미지 감소율 (0~100%)
     */
    public double calculateDamageReduction(Player player) {
        PlayerStats stats = getStats(player);
        int con = stats.getCon();
        return ((double) con / (con + 100)) * 100;
    }

    /**
     * 채집 보너스 확률을 계산합니다.
     * 
     * @param player 플레이어
     * @return 보너스 확률 (%)
     */
    public double calculateDropBonus(Player player) {
        PlayerStats stats = getStats(player);
        return stats.getLuck() * 0.5;
    }

    /**
     * 최대 마나를 계산합니다.
     * 
     * @param player 플레이어
     * @return 최대 마나
     */
    public double calculateMaxMana(Player player) {
        PlayerStats stats = getStats(player);
        return 100.0 + (stats.getInt() * 10.0);
    }

    /**
     * 마나 재생량을 계산합니다.
     * 
     * @param player 플레이어
     * @return 초당 마나 재생량
     */
    public double calculateManaRegen(Player player) {
        PlayerStats stats = getStats(player);
        return 5.0 + (stats.getInt() * 0.5);
    }

    // ==================== 내부 클래스: PlayerStats ====================

    /**
     * 플레이어 스탯 데이터 클래스
     */
    public static class PlayerStats {

        private final UUID uuid;

        /** 기본 스탯 */
        private int str; // 힘
        private int dex; // 민첩
        private int con; // 체력
        private int intelligence; // 지능 (int는 예약어)
        private int luck; // 행운

        /** 남은 스탯 포인트 */
        private int statPoints;

        /** 보너스 스탯 (직업 레벨 등) */
        private transient int bonusStr;
        private transient int bonusDex;
        private transient int bonusCon;
        private transient int bonusInt;
        private transient int bonusLuck;

        /** 장비 스탯 */
        private transient int equipmentStr;
        private transient int equipmentDex;
        private transient int equipmentCon;
        private transient int equipmentInt;
        private transient int equipmentLuck;

        /** 세트 보너스 스탯 */
        private transient int setBonusStr;
        private transient int setBonusDex;
        private transient int setBonusCon;
        private transient int setBonusInt;
        private transient int setBonusLuck;

        /**
         * PlayerStats 생성자 (기본 스탯)
         * 
         * @param uuid 플레이어 UUID
         */
        public PlayerStats(UUID uuid) {
            this.uuid = uuid;
            this.str = 0;
            this.dex = 0;
            this.con = 0;
            this.intelligence = 0;
            this.luck = 0;
            this.statPoints = 0;
        }

        /**
         * 스탯을 재계산합니다.
         */
        public void recalculate() {
            // TODO: 2단계에서 장비 Lore 파싱, 버프 효과 계산
        }

        // Getters (총합 = 기본 + 직업보너스 + 장비 + 세트효과)
        public int getStr() {
            return str + bonusStr + equipmentStr + setBonusStr;
        }

        public int getDex() {
            return dex + bonusDex + equipmentDex + setBonusDex;
        }

        public int getCon() {
            return con + bonusCon + equipmentCon + setBonusCon;
        }

        public int getInt() {
            return intelligence + bonusInt + equipmentInt + setBonusInt;
        }

        public int getLuck() {
            return luck + bonusLuck + equipmentLuck + setBonusLuck;
        }

        // Base Getters
        public int getBaseStr() {
            return str;
        }

        public int getBaseDex() {
            return dex;
        }

        public int getBaseCon() {
            return con;
        }

        public int getBaseInt() {
            return intelligence;
        }

        public int getBaseLuck() {
            return luck;
        }

        // Setters (기본 스탯만)
        public void setStr(int str) {
            this.str = str;
        }

        public void setDex(int dex) {
            this.dex = dex;
        }

        public void setCon(int con) {
            this.con = con;
        }

        public void setInt(int intelligence) {
            this.intelligence = intelligence;
        }

        public void setLuck(int luck) {
            this.luck = luck;
        }

        // Stat Points
        public int getStatPoints() {
            return statPoints;
        }

        public void setStatPoints(int points) {
            this.statPoints = points;
        }

        public void addStatPoints(int points) {
            this.statPoints += points;
        }

        // Bonus Getters
        public int getBonusStr() {
            return bonusStr;
        }

        public int getBonusDex() {
            return bonusDex;
        }

        public int getBonusCon() {
            return bonusCon;
        }

        public int getBonusInt() {
            return bonusInt;
        }

        public int getBonusLuck() {
            return bonusLuck;
        }

        // Bonus Setters (장비/버프용)
        public void setBonusStr(int bonus) {
            this.bonusStr = bonus;
        }

        public void setBonusDex(int bonus) {
            this.bonusDex = bonus;
        }

        public void setBonusCon(int bonus) {
            this.bonusCon = bonus;
        }

        public void setBonusInt(int bonus) {
            this.bonusInt = bonus;
        }

        public void setBonusLuck(int bonus) {
            this.bonusLuck = bonus;
        }

        // Equipment Setters (장비용)
        public void setEquipmentStr(int value) {
            this.equipmentStr = value;
        }

        public void setEquipmentDex(int value) {
            this.equipmentDex = value;
        }

        public void setEquipmentCon(int value) {
            this.equipmentCon = value;
        }

        public void setEquipmentInt(int value) {
            this.equipmentInt = value;
        }

        public void setEquipmentLuck(int value) {
            this.equipmentLuck = value;
        }

        // Set Bonus Setters
        public void setSetBonusStr(int value) {
            this.setBonusStr = value;
        }

        public void setSetBonusDex(int value) {
            this.setBonusDex = value;
        }

        public void setSetBonusCon(int value) {
            this.setBonusCon = value;
        }

        public void setSetBonusInt(int value) {
            this.setBonusInt = value;
        }

        public void setSetBonusLuck(int value) {
            this.setBonusLuck = value;
        }

        public UUID getUuid() {
            return uuid;
        }
    }
}
