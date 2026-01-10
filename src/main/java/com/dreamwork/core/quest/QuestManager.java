package com.dreamwork.core.quest;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.engine.RewardProcessor;
import com.dreamwork.core.job.engine.TriggerType;
import com.dreamwork.core.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 퀘스트 시스템 매니저
 * 
 * <p>
 * 일일/메인 퀘스트를 관리하고 JobManager의 Trigger 시스템을 재사용합니다.
 * </p>
 */
public class QuestManager extends Manager {

    private final DreamWorkCore plugin;

    /** 등록된 퀘스트 (ID -> Quest) */
    private final Map<String, Quest> quests;

    /** 플레이어별 퀘스트 진행 상황 (UUID -> (QuestID -> Progress)) */
    private final Map<UUID, Map<String, QuestProgress>> playerProgress;

    /** 일일 퀘스트 풀 (매일 재설정) */
    private final List<String> dailyQuestPool;

    /** 현재 활성화된 일일 퀘스트 ID 목록 */
    private List<String> todaysDailyQuests;

    /** 마지막 일일 퀘스트 재설정 날짜 */
    private LocalDate lastDailyReset;

    public QuestManager(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.quests = new ConcurrentHashMap<>();
        this.playerProgress = new ConcurrentHashMap<>();
        this.dailyQuestPool = new ArrayList<>();
        this.todaysDailyQuests = new ArrayList<>();
    }

    @Override
    public void onEnable() {
        // 퀘스트 설정 로드
        loadQuests();

        // 일일 퀘스트 설정
        checkDailyReset();

        // 자정 체크 태스크 (5분마다)
        new BukkitRunnable() {
            @Override
            public void run() {
                checkDailyReset();
            }
        }.runTaskTimer(plugin, 6000L, 6000L);

        enabled = true;
        plugin.getLogger().info("QuestManager 활성화 완료! 퀘스트: " + quests.size() + "개");
    }

    @Override
    public void onDisable() {
        enabled = false;
        quests.clear();
        playerProgress.clear();
        plugin.getLogger().info("QuestManager 비활성화 완료");
    }

    @Override
    public void reload() {
        quests.clear();
        loadQuests();
        checkDailyReset();
        plugin.getLogger().info("QuestManager 리로드 완료! 퀘스트: " + quests.size() + "개");
    }

    /**
     * 퀘스트 설정 파일들을 로드합니다.
     */
    private void loadQuests() {
        File questsFolder = new File(plugin.getDataFolder(), "quests");
        if (!questsFolder.exists()) {
            questsFolder.mkdirs();
            createDefaultQuestFile();
        }

        File[] files = questsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                for (String questId : config.getKeys(false)) {
                    ConfigurationSection section = config.getConfigurationSection(questId);
                    if (section != null) {
                        Map<String, Object> questConfig = section.getValues(true);
                        Quest quest = Quest.fromConfig(questId, questConfig);
                        quests.put(questId, quest);

                        // 일일 퀘스트 풀에 추가
                        if (quest.getType() == Quest.QuestType.DAILY) {
                            dailyQuestPool.add(questId);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "퀘스트 파일 로드 실패: " + file.getName(), e);
            }
        }
    }

    /**
     * 기본 퀘스트 파일을 생성합니다.
     */
    private void createDefaultQuestFile() {
        plugin.saveResource("quests/daily.yml", false);
    }

    /**
     * 일일 퀘스트 재설정 체크
     */
    private void checkDailyReset() {
        LocalDate today = LocalDate.now();
        if (lastDailyReset == null || !lastDailyReset.equals(today)) {
            resetDailyQuests();
            lastDailyReset = today;
        }
    }

    /**
     * 일일 퀘스트를 재설정합니다.
     */
    private void resetDailyQuests() {
        // 모든 플레이어의 일일 퀘스트 진행 상황 초기화
        for (Map<String, QuestProgress> progress : playerProgress.values()) {
            progress.entrySet().removeIf(entry -> {
                Quest quest = quests.get(entry.getKey());
                return quest != null && quest.getType() == Quest.QuestType.DAILY;
            });
        }

        // 오늘의 일일 퀘스트 선택 (최대 3개)
        todaysDailyQuests = new ArrayList<>();
        List<String> pool = new ArrayList<>(dailyQuestPool);
        Collections.shuffle(pool);
        for (int i = 0; i < Math.min(3, pool.size()); i++) {
            todaysDailyQuests.add(pool.get(i));
        }

        plugin.getLogger().info("일일 퀘스트 재설정: " + todaysDailyQuests);
    }

    /**
     * 플레이어에게 일일 퀘스트를 할당합니다.
     */
    public void assignDailyQuests(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, QuestProgress> progress = playerProgress.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        for (String questId : todaysDailyQuests) {
            if (!progress.containsKey(questId)) {
                progress.put(questId, new QuestProgress(questId));
            }
        }
    }

    /**
     * 퀘스트 진행도를 업데이트합니다.
     * 
     * @param player  플레이어
     * @param trigger 트리거 타입
     * @param target  대상 (블록 이름, 몹 이름 등)
     */
    public void updateProgress(Player player, TriggerType trigger, String target) {
        UUID uuid = player.getUniqueId();
        Map<String, QuestProgress> progress = playerProgress.get(uuid);
        if (progress == null)
            return;

        for (QuestProgress qp : progress.values()) {
            if (qp.getStatus() != QuestProgress.QuestStatus.IN_PROGRESS)
                continue;

            Quest quest = quests.get(qp.getQuestId());
            if (quest == null || quest.getRequirement() == null)
                continue;

            // 요구사항 일치 여부 확인
            if (quest.getRequirement().matches(trigger, target)) {
                qp.addProgress(1);

                // 완료 체크
                if (qp.isComplete(quest.getRequirement().getAmount())) {
                    qp.setStatus(QuestProgress.QuestStatus.COMPLETED);
                    player.sendMessage("§a[퀘스트] §f'" + quest.getName() + "' §a완료! 보상을 받으세요.");
                }

                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[Quest] " + player.getName() + " - " + quest.getName() +
                            " 진행: " + qp.getCurrentProgress() + "/" + quest.getRequirement().getAmount());
                }
            }
        }
    }

    /**
     * 퀘스트 보상을 지급합니다.
     */
    public boolean giveReward(Player player, String questId) {
        UUID uuid = player.getUniqueId();
        Map<String, QuestProgress> progress = playerProgress.get(uuid);
        if (progress == null)
            return false;

        QuestProgress qp = progress.get(questId);
        if (qp == null || qp.getStatus() != QuestProgress.QuestStatus.COMPLETED) {
            return false;
        }

        Quest quest = quests.get(questId);
        if (quest == null || quest.getReward() == null)
            return false;

        Quest.Reward reward = quest.getReward();

        // 경험치 지급
        if (reward.getExp() > 0) {
            plugin.getJobManager().addExp(player, reward.getExp());
        }

        // 아이템 지급
        for (String itemStr : reward.getItems()) {
            String[] parts = itemStr.split(":");
            Material material = Material.matchMaterial(parts[0]);
            int amount = parts.length >= 2 ? parseInt(parts[1], 1) : 1;
            if (material != null) {
                player.getInventory().addItem(new ItemStack(material, amount));
            }
        }

        // 커맨드 실행
        for (String cmd : reward.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    cmd.replace("%player%", player.getName()));
        }

        qp.setStatus(QuestProgress.QuestStatus.REWARDED);
        player.sendMessage("§a[퀘스트] '" + quest.getName() + "' 보상을 받았습니다!");

        return true;
    }

    // ==================== Getters ====================

    public Quest getQuest(String id) {
        return quests.get(id);
    }

    public Map<String, Quest> getQuests() {
        return Collections.unmodifiableMap(quests);
    }

    public List<String> getTodaysDailyQuests() {
        return Collections.unmodifiableList(todaysDailyQuests);
    }

    public Map<String, QuestProgress> getPlayerProgress(UUID uuid) {
        return playerProgress.getOrDefault(uuid, Collections.emptyMap());
    }

    private int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
