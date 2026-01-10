package com.dreamwork.core.job.engine;

import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.JobProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * 보상 처리 엔진 (고도화 버전)
 * 
 * <p>
 * 다양한 보상 타입을 지원합니다:
 * </p>
 * <ul>
 * <li>경험치 (Experience)</li>
 * <li>커맨드 (Commands)</li>
 * <li>아이템 (Items)</li>
 * <li>이펙트 (Sound, Particle 등)</li>
 * </ul>
 */
public class RewardProcessor {

    private final JobManager jobManager;

    public RewardProcessor(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    /**
     * 보상을 처리합니다. (경험치 지급 등)
     * 
     * @param provider 직업 제공자
     * @param player   플레이어
     * @param trigger  트리거 타입
     * @param exp      획득할 경험치
     */
    public void process(JobProvider provider, Player player, TriggerType trigger, double exp) {
        if (exp <= 0)
            return;

        // 1. 경험치 추가 (JobManager 위임 -> 레벨업 체크 자동 수행)
        jobManager.addExp(player, exp);
    }

    /**
     * 레벨업 보상을 처리합니다.
     * 
     * @param player  플레이어
     * @param rewards 보상 데이터 (YAML에서 파싱된 Map)
     */
    public void processLevelUpRewards(Player player, Map<String, Object> rewards) {
        if (rewards == null)
            return;

        // Commands
        if (rewards.containsKey("commands")) {
            processCommands(player, rewards.get("commands"));
        }

        // Items
        if (rewards.containsKey("items")) {
            processItems(player, rewards.get("items"));
        }

        // Effects
        if (rewards.containsKey("effects")) {
            processEffects(player, rewards.get("effects"));
        }

        // Message
        if (rewards.containsKey("message")) {
            String message = String.valueOf(rewards.get("message"));
            message = message.replace("%player%", player.getName());
            player.sendMessage(translateColorCodes(message));
        }
    }

    /**
     * 커맨드 보상 처리
     */
    @SuppressWarnings("unchecked")
    private void processCommands(Player player, Object commandsObj) {
        if (commandsObj instanceof List<?> commands) {
            for (Object cmd : commands) {
                String command = String.valueOf(cmd);
                command = command.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    /**
     * 아이템 보상 처리
     * 형식: "MATERIAL:AMOUNT" (예: "DIAMOND:3")
     */
    @SuppressWarnings("unchecked")
    private void processItems(Player player, Object itemsObj) {
        if (itemsObj instanceof List<?> items) {
            for (Object itemStr : items) {
                String[] parts = String.valueOf(itemStr).split(":");
                if (parts.length >= 1) {
                    Material material = Material.matchMaterial(parts[0]);
                    int amount = parts.length >= 2 ? parseInt(parts[1], 1) : 1;

                    if (material != null) {
                        ItemStack item = new ItemStack(material, amount);
                        player.getInventory().addItem(item);
                    }
                }
            }
        }
    }

    /**
     * 이펙트 보상 처리
     * 형식: "TYPE:VALUE" (예: "SOUND:ENTITY_PLAYER_LEVELUP")
     */
    @SuppressWarnings("unchecked")
    private void processEffects(Player player, Object effectsObj) {
        if (effectsObj instanceof List<?> effects) {
            for (Object effectStr : effects) {
                String[] parts = String.valueOf(effectStr).split(":");
                if (parts.length >= 2) {
                    String type = parts[0].toUpperCase();
                    String value = parts[1];

                    switch (type) {
                        case "SOUND" -> {
                            try {
                                Sound sound = Sound.valueOf(value);
                                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                        case "PARTICLE" -> {
                            // Particle 처리 (추가 구현 가능)
                        }
                    }
                }
            }
        }
    }

    private int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String translateColorCodes(String text) {
        return text.replace("&", "§");
    }
}
