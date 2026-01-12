package com.dreamwork.core.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 광부 채굴 콤보 시스템
 * <p>
 * 연속 채굴 시 콤보가 쌓이며 경험치/드롭 보너스를 제공합니다.
 * ActionBar에 현재 콤보를 표시합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class MiningComboSystem implements Listener {

    private final DreamWorkCore plugin;

    /** 플레이어별 콤보 정보 */
    private final Map<UUID, ComboData> comboMap = new ConcurrentHashMap<>();

    /** 콤보 유지 시간 (밀리초) */
    private static final long COMBO_TIMEOUT_MS = 3000;

    /** 최대 콤보 */
    private static final int MAX_COMBO = 100;

    /** 광물 블록 */
    private static final Set<Material> ORES = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS);

    public MiningComboSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // 광물인지 확인
        if (!ORES.contains(blockType)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        ComboData data = comboMap.computeIfAbsent(uuid, k -> new ComboData());

        // 콤보 타임아웃 체크
        if (now - data.lastMineTime > COMBO_TIMEOUT_MS) {
            data.combo = 0;
        }

        // 콤보 증가
        data.combo = Math.min(data.combo + 1, MAX_COMBO);
        data.lastMineTime = now;

        // ActionBar 표시
        showComboActionBar(player, data.combo);

        // 콤보 보너스 경험치
        if (data.combo >= 10) {
            UserData userData = plugin.getStorageManager().getUserData(uuid);
            if (userData != null) {
                double bonusExp = getComboBonus(data.combo);
                plugin.getJobManager().addExp(player, JobType.MINER, bonusExp);
            }
        }
    }

    /**
     * ActionBar에 콤보를 표시합니다.
     */
    private void showComboActionBar(Player player, int combo) {
        String comboColor = getComboColor(combo);
        String bar = createComboBar(combo);

        Component message = Component.text(
                comboColor + "⚒ 채굴 콤보: " + combo + " " + bar +
                        " §7(+" + String.format("%.0f", getComboBonus(combo)) + "% XP)");

        player.sendActionBar(message);
    }

    /**
     * 콤보 진행 바를 생성합니다.
     */
    private String createComboBar(int combo) {
        int filled = combo / 10; // 10콤보당 1칸
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("§a|");
            } else {
                bar.append("§7|");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }

    /**
     * 콤보에 따른 색상을 반환합니다.
     */
    private String getComboColor(int combo) {
        if (combo >= 80)
            return "§c§l";
        if (combo >= 60)
            return "§6§l";
        if (combo >= 40)
            return "§e§l";
        if (combo >= 20)
            return "§a";
        return "§f";
    }

    /**
     * 콤보에 따른 보너스 경험치를 반환합니다.
     */
    private double getComboBonus(int combo) {
        return combo * 0.5; // 콤보당 0.5 경험치
    }

    /**
     * 현재 콤보를 반환합니다.
     */
    public int getCombo(Player player) {
        ComboData data = comboMap.get(player.getUniqueId());
        if (data == null)
            return 0;

        // 타임아웃 체크
        if (System.currentTimeMillis() - data.lastMineTime > COMBO_TIMEOUT_MS) {
            return 0;
        }
        return data.combo;
    }

    /**
     * 콤보 데이터 클래스
     */
    private static class ComboData {
        int combo = 0;
        long lastMineTime = 0;
    }
}
