package com.dreamwork.core.quest;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.engine.TriggerType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * 직업별 퀘스트 풀 관리
 * 
 * <p>
 * 직업 ID별로 사용 가능한 퀘스트를 관리합니다.
 * </p>
 */
public class QuestPool {

    private final DreamWorkCore plugin;
    private final Random random = new Random();

    /** 직업별 퀘스트 풀 (jobId -> List<Quest>) */
    private final Map<String, List<Quest>> jobQuestPools = new HashMap<>();

    /** 공용 퀘스트 풀 */
    private final List<Quest> commonQuests = new ArrayList<>();

    public QuestPool(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 퀘스트 풀을 로드합니다.
     */
    public void loadPools() {
        jobQuestPools.clear();
        commonQuests.clear();

        File questsFolder = new File(plugin.getDataFolder(), "quests");
        if (!questsFolder.exists()) {
            questsFolder.mkdirs();
        }

        File[] files = questsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            loadQuestFile(file);
        }

        plugin.getLogger().info("QuestPool: " + getTotalQuestCount() + "개 퀘스트 로드 완료");
    }

    private void loadQuestFile(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            for (String questId : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(questId);
                if (section == null)
                    continue;

                Quest quest = parseQuest(questId, section);
                if (quest == null)
                    continue;

                // 직업별 분류
                String jobRequirement = section.getString("job", "common");

                if (jobRequirement.equals("common") || jobRequirement.isEmpty()) {
                    commonQuests.add(quest);
                } else {
                    jobQuestPools.computeIfAbsent(jobRequirement, k -> new ArrayList<>()).add(quest);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "퀘스트 파일 로드 실패: " + file.getName(), e);
        }
    }

    private Quest parseQuest(String id, ConfigurationSection section) {
        String name = section.getString("name", id);
        String description = section.getString("description", "");
        Quest.QuestType type = Quest.QuestType.valueOf(
                section.getString("type", "DAILY").toUpperCase());

        // Requirement
        ConfigurationSection reqSection = section.getConfigurationSection("requirement");
        Quest.Requirement requirement = null;
        if (reqSection != null) {
            TriggerType trigger = TriggerType.fromString(reqSection.getString("trigger", "BLOCK_BREAK"));
            String target = reqSection.getString("target", "*");
            int amount = reqSection.getInt("amount", 1);
            requirement = new Quest.Requirement(trigger, target, amount);
        }

        // Reward
        ConfigurationSection rewardSection = section.getConfigurationSection("reward");
        Quest.Reward reward = null;
        if (rewardSection != null) {
            double exp = rewardSection.getDouble("exp", 0);
            List<String> items = rewardSection.getStringList("items");
            List<String> commands = rewardSection.getStringList("commands");
            reward = new Quest.Reward(exp, items, commands);
        }

        return new Quest(id, name, description, type, requirement, reward);
    }

    /**
     * 직업에 맞는 퀘스트 풀을 반환합니다.
     */
    public List<Quest> loadPool(String jobId) {
        List<Quest> pool = new ArrayList<>(commonQuests);

        if (jobId != null && jobQuestPools.containsKey(jobId)) {
            pool.addAll(jobQuestPools.get(jobId));
        }

        return pool;
    }

    /**
     * 직업에 맞는 랜덤 퀘스트를 반환합니다.
     */
    public Quest getRandomQuest(String jobId) {
        List<Quest> pool = loadPool(jobId);
        if (pool.isEmpty())
            return null;

        return pool.get(random.nextInt(pool.size()));
    }

    /**
     * 일일 퀘스트만 필터링하여 랜덤 반환
     */
    public Quest getRandomDailyQuest(String jobId) {
        List<Quest> pool = loadPool(jobId);
        List<Quest> dailyQuests = pool.stream()
                .filter(q -> q.getType() == Quest.QuestType.DAILY)
                .toList();

        if (dailyQuests.isEmpty())
            return null;
        return dailyQuests.get(random.nextInt(dailyQuests.size()));
    }

    /**
     * 여러 개의 랜덤 일일 퀘스트를 반환합니다.
     */
    public List<Quest> getRandomDailyQuests(String jobId, int count) {
        List<Quest> pool = loadPool(jobId);
        List<Quest> dailyQuests = new ArrayList<>(pool.stream()
                .filter(q -> q.getType() == Quest.QuestType.DAILY)
                .toList());

        Collections.shuffle(dailyQuests);
        return dailyQuests.subList(0, Math.min(count, dailyQuests.size()));
    }

    public int getTotalQuestCount() {
        int count = commonQuests.size();
        for (List<Quest> pool : jobQuestPools.values()) {
            count += pool.size();
        }
        return count;
    }
}
