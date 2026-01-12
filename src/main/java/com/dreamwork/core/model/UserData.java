package com.dreamwork.core.model;

import com.dreamwork.core.job.JobInfo;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.quest.QuestProgress;
import com.dreamwork.core.stat.StatManager;
import lombok.Data;

import java.time.LocalDate;
import java.util.*;

/**
 * 플레이어 데이터 모델 (DTO)
 * <p>
 * 직업, 스탯, 퀘스트 등 모든 영구 데이터를 포함합니다.
 * StorageManager를 통해 DB와 동기화됩니다.
 * </p>
 * <p>
 * DreamWork 1.0에서는 5개 직업(광부, 농부, 어부, 사냥꾼, 탐험가)을
 * 동시에 성장시키는 구조를 사용합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
@Data
public class UserData {

    private final UUID uuid;
    private String name;

    // ==================== 다중 직업 데이터 ====================
    /**
     * 5개 직업의 레벨/경험치 데이터
     * Key: JobType, Value: JobInfo (레벨, 현재 경험치, 총 경험치)
     */
    private Map<JobType, JobInfo> jobs = new EnumMap<>(JobType.class);

    // ==================== 스탯 데이터 ====================
    private int str; // 힘 (근접 공격력)
    private int dex; // 민첩 (이동 속도, 도구 속도)
    private int con; // 체력 (최대 HP)
    private int intel; // 지능 (마나, 스킬 효과)
    private int luk; // 행운 (드롭 확률, 크리티컬)
    private int statPoints;

    // ==================== 경제 데이터 ====================
    private double balance = 0.0; // 소지금 (Money)

    // ==================== 런타임 데이터 ====================
    private double currentMana = 100.0;
    private double maxMana = 100.0;

    // ==================== 퀘스트 데이터 ====================
    /**
     * 퀘스트 진행 상황
     * Key: 퀘스트 ID
     */
    private Map<String, QuestProgress> questProgresses = new HashMap<>();

    // ==================== 일일 리셋 추적 ====================
    private LocalDate lastDailyReset;

    // ==================== 신규 시스템 데이터 (Phase 1) ====================
    /**
     * 몬스터 처치 수 (슬레이어 도감)
     * Key: Mob Type Name (e.g., "ZOMBIE"), Value: 처치 수
     */
    private Map<String, Integer> mobKillCounts = new HashMap<>();

    /**
     * 탐험한 청크 키 목록 (아틀라스 시스템 - 유저별 방문 기록용)
     * *참고: AtlasDiscoverySystem은 주로 청크 PDC를 쓰지만, 유저별 업적 관리를 위해 필요할 수 있음.
     * 현재 설계상으로는 청크 PDC가 메인이지만, 백업용으로 둡니다.*
     */
    private Set<Long> exploredChunks = new HashSet<>();

    // ==================== 더티 플래그 ====================
    /**
     * 데이터 변경 여부 (DB 저장 최적화용)
     */
    private boolean dirty = false;

    /**
     * 새 플레이어 데이터를 생성합니다.
     * 모든 직업이 레벨 1로 초기화됩니다.
     * 
     * @param uuid 플레이어 UUID
     * @param name 플레이어 이름
     */
    public UserData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.lastDailyReset = LocalDate.now();

        // 모든 직업을 레벨 1로 초기화
        for (JobType jobType : JobType.values()) {
            jobs.put(jobType, new JobInfo());
        }
    }

    // ==================== 직업 관련 메서드 ====================

    /**
     * 특정 직업의 정보를 가져옵니다.
     * 
     * @param jobType 직업 타입
     * @return 직업 정보 (없으면 새로 생성)
     */
    public JobInfo getJobInfo(JobType jobType) {
        return jobs.computeIfAbsent(jobType, k -> new JobInfo());
    }

    /**
     * 특정 직업의 레벨을 가져옵니다.
     * 
     * @param jobType 직업 타입
     * @return 레벨
     */
    public int getJobLevel(JobType jobType) {
        return getJobInfo(jobType).getLevel();
    }

    /**
     * 특정 직업의 현재 경험치를 가져옵니다.
     * 
     * @param jobType 직업 타입
     * @return 현재 경험치
     */
    public double getJobExp(JobType jobType) {
        return getJobInfo(jobType).getCurrentExp();
    }

    /**
     * 모든 직업의 레벨 합계를 반환합니다.
     * 플레이어 랭크 계산에 사용됩니다.
     * 
     * @return 총 레벨 합계
     */
    public int getTotalJobLevel() {
        int total = 0;
        for (JobInfo info : jobs.values()) {
            total += info.getLevel();
        }
        return total;
    }

    /**
     * 가장 높은 레벨의 직업을 반환합니다.
     * 
     * @return 최고 레벨 직업 타입
     */
    public JobType getHighestLevelJob() {
        JobType highest = JobType.MINER;
        int highestLevel = 0;

        for (Map.Entry<JobType, JobInfo> entry : jobs.entrySet()) {
            if (entry.getValue().getLevel() > highestLevel) {
                highestLevel = entry.getValue().getLevel();
                highest = entry.getKey();
            }
        }
        return highest;
    }

    /**
     * 직업에 경험치를 추가합니다.
     * 
     * @param jobType 직업 타입
     * @param amount  추가할 경험치량
     */
    public void addJobExp(JobType jobType, double amount) {
        if (amount > 0) {
            getJobInfo(jobType).addExp(amount);
            markDirty();
        }
    }

    // ==================== 스탯 관련 메서드 ====================

    /**
     * 스탯 데이터를 설정합니다.
     * 
     * @param stats 플레이어 스탯
     */
    public void setStats(StatManager.PlayerStats stats) {
        if (stats != null) {
            this.str = stats.getBaseStr();
            this.dex = stats.getBaseDex();
            this.con = stats.getBaseCon();
            this.intel = stats.getBaseInt();
            this.luk = stats.getBaseLuck();
            this.statPoints = stats.getStatPoints();
            markDirty();
        }
    }

    /**
     * 최대 마나를 반환합니다.
     * 
     * @return 최대 마나
     */
    public double getMaxMana() {
        // 기본 100 + 지능 보너스
        return 100.0 + (intel * 2.0);
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 데이터가 변경되었음을 표시합니다.
     */
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * 더티 플래그를 초기화합니다. (저장 완료 후 호출)
     */
    public void clearDirty() {
        this.dirty = false;
    }

    // ==================== 슬레이어 시스템 메서드 ====================

    public void addMobKillCount(String mobType, int amount) {
        mobKillCounts.merge(mobType, amount, Integer::sum);
        markDirty();
    }

    public int getMobKillCount(String mobType) {
        return mobKillCounts.getOrDefault(mobType, 0);
    }

    public Map<String, Integer> getMobKillCounts() {
        return Collections.unmodifiableMap(mobKillCounts);
    }

    // ==================== 아틀라스 시스템 메서드 ====================

    public void addExploredChunk(long chunkKey) {
        if (exploredChunks.add(chunkKey)) {
            markDirty();
        }
    }

    public boolean hasExploredChunk(long chunkKey) {
        return exploredChunks.contains(chunkKey);
    }

    // ==================== 경제 시스템 메서드 ====================

    public void addMoney(double amount) {
        if (amount <= 0)
            return;
        this.balance += amount;
        markDirty();
    }

    public boolean hasMoney(double amount) {
        return this.balance >= amount;
    }

    public void removeMoney(double amount) {
        if (amount <= 0)
            return;
        this.balance = Math.max(0, this.balance - amount);
        markDirty();
    }

    public void setMoney(double amount) {
        this.balance = Math.max(0, amount);
        markDirty();
    }

    // ==================== 하위 호환성 메서드 (Deprecated) ====================

    /**
     * @deprecated 다중 직업 시스템에서는 getJobInfo(JobType)을 사용하세요.
     */
    @Deprecated
    public String getJobId() {
        return getHighestLevelJob().getConfigKey();
    }

    /**
     * @deprecated 다중 직업 시스템에서는 getJobLevel(JobType)을 사용하세요.
     */
    @Deprecated
    public int getJobLevel() {
        return getJobLevel(getHighestLevelJob());
    }

    /**
     * @deprecated 다중 직업 시스템에서는 getJobExp(JobType)을 사용하세요.
     */
    @Deprecated
    public double getJobExp() {
        return getJobExp(getHighestLevelJob());
    }
}
