package com.dreamwork.core.quest;

import com.dreamwork.core.job.engine.TriggerType;

import java.util.List;
import java.util.Map;

/**
 * 퀘스트 데이터 클래스
 */
public class Quest {

    private final String id;
    private final String name;
    private final String description;
    private final QuestType type;
    private final Requirement requirement;
    private final Reward reward;

    public Quest(String id, String name, String description, QuestType type,
            Requirement requirement, Reward reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.requirement = requirement;
        this.reward = reward;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public QuestType getType() {
        return type;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public Reward getReward() {
        return reward;
    }

    /**
     * 퀘스트 타입
     */
    public enum QuestType {
        DAILY, // 일일 퀘스트
        WEEKLY, // 주간 퀘스트
        MAIN, // 메인 퀘스트
        SIDE // 사이드 퀘스트
    }

    /**
     * 퀘스트 요구사항
     */
    public static class Requirement {
        private final TriggerType trigger;
        private final String target;
        private final int amount;

        public Requirement(TriggerType trigger, String target, int amount) {
            this.trigger = trigger;
            this.target = target;
            this.amount = amount;
        }

        public TriggerType getTrigger() {
            return trigger;
        }

        public String getTarget() {
            return target;
        }

        public int getAmount() {
            return amount;
        }

        /**
         * 행동이 요구사항과 일치하는지 확인
         */
        public boolean matches(TriggerType trigger, String target) {
            if (this.trigger != trigger)
                return false;
            if (this.target == null || this.target.isEmpty() || this.target.equals("*")) {
                return true; // 모든 타겟 허용
            }
            return this.target.equalsIgnoreCase(target);
        }
    }

    /**
     * 퀘스트 보상
     */
    public static class Reward {
        private final double exp;
        private final List<String> items;
        private final List<String> commands;

        public Reward(double exp, List<String> items, List<String> commands) {
            this.exp = exp;
            this.items = items != null ? items : List.of();
            this.commands = commands != null ? commands : List.of();
        }

        public double getExp() {
            return exp;
        }

        public List<String> getItems() {
            return items;
        }

        public List<String> getCommands() {
            return commands;
        }
    }

    /**
     * YAML 설정에서 Quest 객체를 생성합니다.
     */
    public static Quest fromConfig(String id, Map<String, Object> config) {
        String name = (String) config.getOrDefault("name", id);
        String description = (String) config.getOrDefault("description", "");
        QuestType type = QuestType.valueOf(
                ((String) config.getOrDefault("type", "DAILY")).toUpperCase());

        // Requirement 파싱
        @SuppressWarnings("unchecked")
        Map<String, Object> reqConfig = (Map<String, Object>) config.get("requirement");
        Requirement requirement = null;
        if (reqConfig != null) {
            TriggerType trigger = TriggerType.fromString((String) reqConfig.get("trigger"));
            String target = (String) reqConfig.getOrDefault("target", "*");
            int amount = ((Number) reqConfig.getOrDefault("amount", 1)).intValue();
            requirement = new Requirement(trigger, target, amount);
        }

        // Reward 파싱
        @SuppressWarnings("unchecked")
        Map<String, Object> rewardConfig = (Map<String, Object>) config.get("reward");
        Reward reward = null;
        if (rewardConfig != null) {
            double exp = ((Number) rewardConfig.getOrDefault("exp", 0)).doubleValue();
            @SuppressWarnings("unchecked")
            List<String> items = (List<String>) rewardConfig.get("items");
            @SuppressWarnings("unchecked")
            List<String> commands = (List<String>) rewardConfig.get("commands");
            reward = new Reward(exp, items, commands);
        }

        return new Quest(id, name, description, type, requirement, reward);
    }
}
