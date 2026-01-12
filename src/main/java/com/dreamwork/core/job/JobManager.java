package com.dreamwork.core.job;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.manager.Manager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.dreamwork.core.job.engine.*;
import com.dreamwork.core.job.event.JobLevelUpEvent;

/**
 * 직업 엔진 컨트롤러
 * 
 * <p>
 * 모든 직업을 관리하고, 플레이어의 직업 데이터를 처리합니다.
 * YAML 파일에서 직업 설정을 로드하고 경험치/레벨업을 관리합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class JobManager extends Manager {

    private final DreamWorkCore plugin;

    /** 등록된 직업 목록 (ID -> JobProvider) */
    private final Map<String, JobProvider> jobs;

    /** 플레이어별 직업 데이터 (UUID -> UserJobData) */
    private final Map<UUID, UserJobData> userJobs;

    /** 직업 설정 파일 폴더 */
    private File jobsFolder;

    /** 경험치 배율 */
    private double expMultiplier = 1.0;

    /** 경험치 계산기 */
    private ExpCalculator expCalculator;

    // 직업 엔진 컴포넌트
    private TriggerManager triggerManager;
    private JobValidator jobValidator;
    private RewardProcessor rewardProcessor;

    /**
     * JobManager 생성자
     * 
     * @param plugin 플러그인 인스턴스
     */
    public JobManager(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.jobs = new ConcurrentHashMap<>();
        this.userJobs = new ConcurrentHashMap<>();

        // 엔진 컴포넌트 초기화
        this.triggerManager = new TriggerManager();
        this.jobValidator = new JobValidator();
        this.rewardProcessor = new RewardProcessor(this);
    }

    @Override
    public void onEnable() {
        // 설정 로드
        loadConfig();

        // jobs 폴더 생성 및 기본 파일 복사
        setupJobsFolder();

        // YAML 파일에서 직업 로드
        loadJobsFromYaml();

        enabled = true;
        plugin.getLogger().info("JobManager 활성화 완료! 등록된 직업: " + jobs.size() + "개");
    }

    @Override
    public void onDisable() {
        enabled = false;

        // 모든 유저 직업 데이터 저장
        saveAllUserJobs();

        jobs.clear();
        userJobs.clear();

        plugin.getLogger().info("JobManager 비활성화 완료");
    }

    @Override
    public void reload() {
        loadConfig();

        // 직업 다시 로드
        jobs.clear();
        loadJobsFromYaml();

        plugin.getLogger().info("JobManager 리로드 완료! 직업: " + jobs.size() + "개");
    }

    /**
     * 설정을 로드합니다.
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        expMultiplier = config.getDouble("jobs.exp-multiplier", 1.0);

        String formula = config.getString("exp-formula", "100 + (level * 25) + (level^2 * 5)");
        this.expCalculator = new ExpCalculator(formula);
    }

    /**
     * jobs 폴더를 설정합니다.
     */
    private void setupJobsFolder() {
        jobsFolder = new File(plugin.getDataFolder(), "jobs");
        if (!jobsFolder.exists()) {
            jobsFolder.mkdirs();

            // 기본 직업 파일 생성 (miner.yml)
            createDefaultJobFile();
        }
    }

    /**
     * 기본 직업 파일을 생성합니다.
     */
    private void createDefaultJobFile() {
        // 플러그인 리소스에서 복사 시도
        if (plugin.getResource("jobs/miner.yml") != null) {
            plugin.saveResource("jobs/miner.yml", false);
        }
    }

    /**
     * YAML 파일에서 모든 직업을 로드합니다.
     */
    private void loadJobsFromYaml() {
        if (!jobsFolder.exists() || !jobsFolder.isDirectory()) {
            return;
        }

        File[] files = jobsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = config.getString("id");

                if (id == null || id.isEmpty()) {
                    plugin.getLogger().warning("직업 파일에 ID가 없습니다: " + file.getName());
                    continue;
                }

                ConfiguredJobProvider job = new ConfiguredJobProvider(id, config);
                registerJob(id, job);

                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Debug] 직업 로드: " + id + " (" + file.getName() + ")");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "직업 파일 로드 실패: " + file.getName(), e);
            }
        }
    }

    // ==================== 직업 등록/조회 ====================

    /**
     * 직업을 등록합니다.
     * 
     * @param id       직업 ID
     * @param provider 직업 제공자
     */
    public void registerJob(String id, JobProvider provider) {
        jobs.put(id.toLowerCase(), provider);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] 직업 등록됨: " + id);
        }
    }

    /**
     * 직업을 조회합니다.
     * 
     * @param id 직업 ID
     * @return JobProvider (없으면 null)
     */
    public JobProvider getJob(String id) {
        return jobs.get(id.toLowerCase());
    }

    /**
     * 모든 등록된 직업을 반환합니다.
     * 
     * @return 직업 맵 (불변)
     */
    public Map<String, JobProvider> getJobs() {
        return Collections.unmodifiableMap(jobs);
    }

    /**
     * 직업이 등록되어 있는지 확인합니다.
     * 
     * @param id 직업 ID
     * @return 등록 여부
     */
    public boolean hasJob(String id) {
        return jobs.containsKey(id.toLowerCase());
    }

    // ==================== 유저 직업 데이터 관리 ====================

    /**
     * 유저의 직업 데이터를 조회합니다.
     * 
     * @param uuid 플레이어 UUID
     * @return UserJobData (없으면 생성)
     */
    public UserJobData getUserJob(UUID uuid) {
        return userJobs.computeIfAbsent(uuid, UserJobData::new);
    }

    /**
     * 유저의 직업 데이터를 설정합니다.
     * 
     * @param uuid 플레이어 UUID
     * @param data 직업 데이터
     */
    public void setUserJobData(UUID uuid, UserJobData data) {
        userJobs.put(uuid, data);
    }

    /**
     * 유저의 직업을 변경합니다.
     * 
     * @param uuid  플레이어 UUID
     * @param jobId 새 직업 ID
     * @return 변경 성공 여부
     */
    public boolean setUserJob(UUID uuid, String jobId) {
        JobProvider job = getJob(jobId);
        if (job == null) {
            return false;
        }

        UserJobData data = getUserJob(uuid);
        String oldJobId = data.getJobId();

        // 이전 직업 해제 콜백
        if (oldJobId != null) {
            JobProvider oldJob = getJob(oldJobId);
            if (oldJob != null) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    oldJob.onJobLeave(player);
                }
            }
        }

        // 새 직업 설정
        data.setJobId(jobId.toLowerCase());
        data.setLevel(1);
        data.setCurrentExp(0);

        // 새 직업 선택 콜백
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            job.onJobSelect(player);
            plugin.getStatManager().recalculateStats(player);
        }

        return true;
    }

    /**
     * 유저의 직업 데이터를 캐시에서 제거합니다.
     * 
     * @param uuid 플레이어 UUID
     */
    public void unloadUserJob(UUID uuid) {
        userJobs.remove(uuid);
    }

    /**
     * 모든 유저의 직업 데이터를 저장합니다.
     */
    private void saveAllUserJobs() {
        // StorageManager를 통해 저장 (UserData 통합)
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] 모든 직업 데이터 저장: " + userJobs.size() + "명");
        }
    }

    // ==================== 경험치 시스템 ====================

    /**
     * 플레이어에게 특정 직업의 경험치를 추가합니다. (다중 직업 시스템)
     * 
     * @param player  플레이어
     * @param jobType 직업 타입
     * @param amount  경험치 양
     */
    public void addExp(Player player, JobType jobType, double amount) {
        if (amount <= 0 || jobType == null)
            return;

        UUID uuid = player.getUniqueId();

        // UserData에서 직업 정보 가져오기
        var userData = plugin.getStorageManager().getUserData(uuid);
        if (userData == null)
            return;

        JobInfo jobInfo = userData.getJobInfo(jobType);
        JobProvider job = getJob(jobType.getConfigKey());

        if (job == null) {
            plugin.getLogger().warning("[JobManager] 직업 설정을 찾을 수 없음: " + jobType.getConfigKey());
            return;
        }

        // 경험치 배율 적용
        double finalExp = amount * expMultiplier;

        // 광부 콤보 시스템 배율 적용
        if (jobType == JobType.MINER && plugin.getMiningComboSystem() != null) {
            finalExp *= plugin.getMiningComboSystem().getExpMultiplier(uuid);
        }

        jobInfo.addExp(finalExp);
        userData.markDirty();

        // 레벨업 체크
        checkLevelUp(player, jobInfo, job, jobType);

        // 디버그 메시지
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] " + player.getName() + " " + jobType.getDisplayName() +
                    " 경험치 획득: " + String.format("%.1f", finalExp) +
                    " (Lv." + jobInfo.getLevel() + " | " +
                    String.format("%.0f", jobInfo.getCurrentExp()) + "/" +
                    String.format("%.0f", calculateRequiredExp(jobInfo.getLevel() + 1)) + ")");
        }
    }

    /**
     * 플레이어에게 경험치를 추가합니다. (하위 호환용)
     * 
     * @param player 플레이어
     * @param amount 경험치 양
     * @deprecated addExp(Player, JobType, double) 사용 권장
     */
    @Deprecated
    public void addExp(Player player, double amount) {
        if (amount <= 0)
            return;

        UUID uuid = player.getUniqueId();
        UserJobData data = getUserJob(uuid);

        if (!data.hasJob()) {
            return; // 직업이 없으면 무시
        }

        JobProvider job = getJob(data.getJobId());
        if (job == null) {
            return;
        }

        // 경험치 배율 적용
        double finalExp = amount * expMultiplier;
        data.addExp(finalExp);

        // 레벨업 체크
        checkLevelUp(player, data, job);

        // 디버그 메시지
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] " + player.getName() + " 경험치 획득: " + finalExp +
                    " (총: " + data.getCurrentExp() + "/" + job.getExpForLevel(data.getLevel() + 1) + ")");
        }
    }

    /**
     * 레벨업을 체크하고 처리합니다. (다중 직업 시스템)
     * 
     * @param player  플레이어
     * @param jobInfo 직업 정보
     * @param job     직업 제공자
     * @param jobType 직업 타입
     */
    private void checkLevelUp(Player player, JobInfo jobInfo, JobProvider job, JobType jobType) {
        int maxLevel = job.getMaxLevel();

        while (jobInfo.getLevel() < maxLevel) {
            double requiredExp = calculateRequiredExp(jobInfo.getLevel() + 1);

            if (jobInfo.getCurrentExp() < requiredExp) {
                break; // 경험치 부족
            }

            // 레벨업!
            int oldLevel = jobInfo.getLevel();
            jobInfo.levelUp(requiredExp);
            int newLevel = jobInfo.getLevel();

            // 커스텀 이벤트 호출
            JobLevelUpEvent event = new JobLevelUpEvent(player, job.getId(), oldLevel, newLevel);
            Bukkit.getPluginManager().callEvent(event);

            // 레벨업 콜백 (JobProvider)
            job.onLevelUp(player, oldLevel, newLevel);

            // 스탯 재계산
            plugin.getStatManager().recalculateStats(player);

            // 레벨업 이펙트
            showLevelUpEffect(player, job, newLevel);

            plugin.getLogger().info(player.getName() + "님이 " + jobType.getDisplayName() +
                    " 레벨 " + newLevel + "을(를) 달성했습니다!");
        }
    }

    /**
     * 레벨업을 체크하고 처리합니다. (하위 호환용)
     * 
     * @param player 플레이어
     * @param data   직업 데이터
     * @param job    직업 제공자
     * @deprecated checkLevelUp(Player, JobInfo, JobProvider, JobType) 사용 권장
     */
    @Deprecated
    private void checkLevelUp(Player player, UserJobData data, JobProvider job) {
        int maxLevel = job.getMaxLevel();

        while (data.getLevel() < maxLevel) {
            double requiredExp = calculateRequiredExp(data.getLevel() + 1);

            if (data.getCurrentExp() < requiredExp) {
                break; // 경험치 부족
            }

            // 레벨업!
            int oldLevel = data.getLevel();
            data.levelUp(requiredExp);
            int newLevel = data.getLevel();

            // 커스텀 이벤트 호출
            JobLevelUpEvent event = new JobLevelUpEvent(player, job.getId(), oldLevel, newLevel);
            Bukkit.getPluginManager().callEvent(event);

            // 레벨업 콜백 (JobProvider)
            job.onLevelUp(player, oldLevel, newLevel);

            // 스탯 재계산
            plugin.getStatManager().recalculateStats(player);

            // 레벨업 이펙트
            showLevelUpEffect(player, job, newLevel);

            plugin.getLogger().info(player.getName() + "님이 " + job.getDisplayName() +
                    " 레벨 " + newLevel + "을(를) 달성했습니다!");
        }
    }

    /**
     * 레벨업 이펙트를 표시합니다.
     * 
     * @param player   플레이어
     * @param job      직업
     * @param newLevel 새 레벨
     */
    private void showLevelUpEffect(Player player, JobProvider job, int newLevel) {
        // 타이틀 메시지
        Title title = Title.title(
                Component.text("LEVEL UP!", NamedTextColor.GOLD),
                Component.text(job.getDisplayName() + " Lv." + newLevel, NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500)));
        player.showTitle(title);

        // 사운드
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.2f);
    }

    public double calculateRequiredExp(int level) {
        return expCalculator.getRequiredExp(level);
    }

    /**
     * 필요 경험치를 반환합니다. (PlaceholderAPI용 래퍼)
     */
    public double getRequiredExp(int level) {
        return calculateRequiredExp(level);
    }

    // ==================== 엔진 컴포넌트 Getters ====================

    public TriggerManager getTriggerManager() {
        return triggerManager;
    }

    public JobValidator getJobValidator() {
        return jobValidator;
    }

    public RewardProcessor getRewardProcessor() {
        return rewardProcessor;
    }

    // ==================== Getters ====================

    /**
     * 직업 폴더를 반환합니다.
     * 
     * @return jobs 폴더
     */
    public File getJobsFolder() {
        return jobsFolder;
    }

    /**
     * 현재 경험치 배율을 반환합니다.
     * 
     * @return 경험치 배율
     */
    public double getExpMultiplier() {
        return expMultiplier;
    }

    /**
     * 경험치 배율을 설정합니다.
     * 
     * @param multiplier 새 배율
     */
    public void setExpMultiplier(double multiplier) {
        this.expMultiplier = multiplier;
    }
}
