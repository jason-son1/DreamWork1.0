package com.dreamwork.core.job;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * YAML 설정 기반 직업 구현체
 * 
 * <p>
 * YAML 파일에서 직업 설정을 읽어와 JobProvider를 구현합니다.
 * 하드코딩 없이 설정 파일만으로 새로운 직업을 추가할 수 있습니다.
 * </p>
 * 
 * <h2>YAML 구조 예시:</h2>
 * 
 * <pre>{@code
 * id: miner
 * display: "광부"
 * description: "광물을 채굴하여 경험치를 얻는 직업"
 * icon: IRON_PICKAXE
 * max_level: 100
 * exp_formula:
 *   base: 100
 *   exponent: 1.5
 * exp_sources:
 *   block_break:
 *     STONE: 1
 *     COAL_ORE: 5
 *     IRON_ORE: 10
 * stats_per_level:
 *   str: 1
 *   con: 0.5
 * rewards:
 *   10:
 *     commands:
 *       - "give %player% diamond 1"
 *     message: "&a10레벨 달성 축하!"
 * }</pre>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class ConfiguredJobProvider implements JobProvider {

    private final String id;
    private final String displayName;
    private final String description;
    private final String icon;
    private final int maxLevel;

    /** 경험치 공식: base * level^exponent */
    private final double expBase;
    private final double expExponent;

    /** 경험치 소스 (action -> target -> exp) */
    private final Map<String, Map<String, Double>> expSources;

    /** 레벨당 스탯 증가량 */
    private final Map<String, Double> statsPerLevel;

    /** 레벨별 보상 (level -> rewards) */
    private final Map<Integer, LevelReward> rewards;

    /**
     * YAML ConfigurationSection에서 직업을 파싱합니다.
     * 
     * @param id     직업 ID
     * @param config YAML 설정
     */
    public ConfiguredJobProvider(String id, ConfigurationSection config) {
        this.id = id.toLowerCase();
        this.displayName = config.getString("display", id);
        this.description = config.getString("description", "");
        this.icon = config.getString("icon", "PAPER");
        this.maxLevel = config.getInt("max_level", 100);

        // 경험치 공식
        ConfigurationSection expFormula = config.getConfigurationSection("exp_formula");
        if (expFormula != null) {
            this.expBase = expFormula.getDouble("base", 100);
            this.expExponent = expFormula.getDouble("exponent", 1.5);
        } else {
            this.expBase = 100;
            this.expExponent = 1.5;
        }

        // 경험치 소스 파싱
        this.expSources = parseExpSources(config.getConfigurationSection("exp_sources"));

        // 레벨당 스탯 파싱
        this.statsPerLevel = parseStatsPerLevel(config.getConfigurationSection("stats_per_level"));

        // 보상 파싱
        this.rewards = parseRewards(config.getConfigurationSection("rewards"));
    }

    /**
     * 경험치 소스를 파싱합니다.
     */
    private Map<String, Map<String, Double>> parseExpSources(ConfigurationSection section) {
        Map<String, Map<String, Double>> sources = new HashMap<>();

        if (section == null) {
            return sources;
        }

        for (String action : section.getKeys(false)) {
            ConfigurationSection actionSection = section.getConfigurationSection(action);
            if (actionSection != null) {
                Map<String, Double> targets = new HashMap<>();
                for (String target : actionSection.getKeys(false)) {
                    targets.put(target.toUpperCase(), actionSection.getDouble(target));
                }
                sources.put(action.toUpperCase(), targets);
            }
        }

        return sources;
    }

    /**
     * 레벨당 스탯을 파싱합니다.
     */
    private Map<String, Double> parseStatsPerLevel(ConfigurationSection section) {
        Map<String, Double> stats = new HashMap<>();

        if (section == null) {
            return stats;
        }

        for (String stat : section.getKeys(false)) {
            stats.put(stat.toLowerCase(), section.getDouble(stat));
        }

        return stats;
    }

    /**
     * 보상을 파싱합니다.
     */
    private Map<Integer, LevelReward> parseRewards(ConfigurationSection section) {
        Map<Integer, LevelReward> rewardMap = new HashMap<>();

        if (section == null) {
            return rewardMap;
        }

        for (String levelStr : section.getKeys(false)) {
            try {
                int level = Integer.parseInt(levelStr);
                ConfigurationSection rewardSection = section.getConfigurationSection(levelStr);
                if (rewardSection != null) {
                    List<String> commands = rewardSection.getStringList("commands");
                    String message = rewardSection.getString("message", "");
                    rewardMap.put(level, new LevelReward(commands, message));
                }
            } catch (NumberFormatException e) {
                // 숫자가 아닌 키는 무시
            }
        }

        return rewardMap;
    }

    // ==================== JobProvider 구현 ====================

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public double getExpForLevel(int level) {
        return expBase * Math.pow(level, expExponent);
    }

    @Override
    public double calculateExp(String action, String target) {
        String actionKey = action.toUpperCase();
        String targetKey = target.toUpperCase();

        Map<String, Double> targets = expSources.get(actionKey);
        if (targets == null) {
            return 0;
        }

        return targets.getOrDefault(targetKey, 0.0);
    }

    @Override
    public void onLevelUp(Player player, int oldLevel, int newLevel) {
        // 레벨 보상 체크
        LevelReward reward = rewards.get(newLevel);
        if (reward != null) {
            reward.execute(player);
        }
    }

    @Override
    public void onJobSelect(Player player) {
        // 직업 선택 시 메시지
        player.sendMessage("§a" + displayName + " 직업을 선택했습니다!");
    }

    @Override
    public void onJobLeave(Player player) {
        // 직업 해제 시 메시지
        player.sendMessage("§c" + displayName + " 직업을 해제했습니다.");
    }

    @Override
    public List<Map<String, Object>> getExpSources() {
        List<Map<String, Object>> sources = new ArrayList<>();

        for (Map.Entry<String, Map<String, Double>> entry : expSources.entrySet()) {
            for (Map.Entry<String, Double> targetEntry : entry.getValue().entrySet()) {
                Map<String, Object> source = new HashMap<>();
                source.put("type", entry.getKey());
                source.put("target", targetEntry.getKey());
                source.put("amount", targetEntry.getValue());
                sources.add(source);
            }
        }

        return sources;
    }

    @Override
    public List<String> getRewards(int level) {
        LevelReward reward = rewards.get(level);
        if (reward != null) {
            return reward.getCommands();
        }
        return Collections.emptyList();
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public Map<String, Double> getStatsPerLevel() {
        return Collections.unmodifiableMap(statsPerLevel);
    }

    // ==================== 내부 클래스 ====================

    /**
     * 레벨 보상 데이터
     */
    public static class LevelReward {
        private final List<String> commands;
        private final String message;

        public LevelReward(List<String> commands, String message) {
            this.commands = commands != null ? commands : new ArrayList<>();
            this.message = message;
        }

        /**
         * 보상을 실행합니다.
         * 
         * @param player 대상 플레이어
         */
        public void execute(Player player) {
            // 메시지 전송
            if (message != null && !message.isEmpty()) {
                player.sendMessage(message.replace("&", "§"));
            }

            // 명령어 실행
            for (String cmd : commands) {
                String parsed = cmd.replace("%player%", player.getName())
                        .replace("%uuid%", player.getUniqueId().toString());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }

        public List<String> getCommands() {
            return commands;
        }

        public String getMessage() {
            return message;
        }
    }
}
