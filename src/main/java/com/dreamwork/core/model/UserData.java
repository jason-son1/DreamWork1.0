package com.dreamwork.core.model;

import com.dreamwork.core.quest.QuestProgress;
import lombok.Data;
import java.util.*;
import java.time.LocalDate;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.stat.StatManager;

/**
 * 플레이어 데이터 모델 (DTO)
 * <p>
 * 직업, 스탯, 퀘스트 등 모든 영구 데이터를 포함합니다.
 * StorageManager를 통해 DB와 동기화됩니다.
 * </p>
 */
@Data
public class UserData {
    private final UUID uuid;
    private String name;

    // Job Data
    private String jobId;
    private int jobLevel = 1;
    private double jobExp = 0;

    // Stat Data
    private int str;
    private int dex;
    private int con;
    private int intel; // 'int' is a reserved keyword
    private int luk;
    private int statPoints;

    // Transient / Runtime Data
    private double currentMana = 100.0;

    // Quest Data
    // Key: Quest ID
    private Map<String, QuestProgress> questProgresses = new HashMap<>();

    // Daily Reset Tracking
    private LocalDate lastDailyReset;

    /**
     * Dirty Flag - 데이터 변경 여부 확인용
     * (DB 저장 최적화를 위함)
     */
    private boolean dirty = false;

    public UserData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.lastDailyReset = LocalDate.now();
    }

    /**
     * 직업 데이터를 설정합니다.
     * 
     * @param jobData 직업 데이터
     */
    public void setJobData(UserJobData jobData) {
        if (jobData != null) {
            this.jobId = jobData.getJobId();
            this.jobLevel = jobData.getLevel();
            this.jobExp = jobData.getCurrentExp(); // DB stores current level exp
            // Note: totalExp is usually derived or stored separately if needed,
            // but the flat UserData structure only has jobExp.
            // Assuming jobExp maps to currentExp based on variable naming pattern.
        }
    }

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
        }
    }

    public void markDirty() {
        this.dirty = true;
    }
}
