package com.dreamwork.core.ui;

import com.dreamwork.core.DreamWorkCore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BossBar 매니저
 * 
 * <p>
 * 스킬 충전, 퀘스트 진행도 등을 BossBar로 표시합니다.
 * </p>
 */
public class BossBarManager {

    private final DreamWorkCore plugin;

    /** 플레이어별 BossBar (UUID -> (barId -> BossBar)) */
    private final Map<UUID, Map<String, BossBar>> playerBars = new ConcurrentHashMap<>();

    public BossBarManager(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    /**
     * BossBar를 생성하거나 가져옵니다.
     */
    public BossBar getOrCreateBar(Player player, String barId) {
        Map<String, BossBar> bars = playerBars.computeIfAbsent(
                player.getUniqueId(), k -> new ConcurrentHashMap<>());

        return bars.computeIfAbsent(barId, id -> {
            BossBar bar = BossBar.bossBar(
                    Component.text(""),
                    1.0f,
                    BossBar.Color.BLUE,
                    BossBar.Overlay.PROGRESS);
            player.showBossBar(bar);
            return bar;
        });
    }

    /**
     * 스킬 충전 상태를 표시합니다.
     */
    public void showSkillCharging(Player player, String skillName, float progress) {
        BossBar bar = getOrCreateBar(player, "skill_charge");
        bar.name(Component.text("§b" + skillName + " §f충전 중..."));
        bar.progress(progress);
        bar.color(BossBar.Color.BLUE);
    }

    /**
     * 퀘스트 진행도를 표시합니다.
     */
    public void showQuestProgress(Player player, String questName, int current, int required) {
        BossBar bar = getOrCreateBar(player, "quest");
        float progress = Math.min(1.0f, (float) current / required);
        bar.name(Component.text("§e" + questName + " §f" + current + "/" + required));
        bar.progress(progress);
        bar.color(progress >= 1.0f ? BossBar.Color.GREEN : BossBar.Color.YELLOW);
    }

    /**
     * 쿨타임을 표시합니다.
     */
    public void showCooldown(Player player, String skillName, float remaining, float total) {
        BossBar bar = getOrCreateBar(player, "cooldown");
        float progress = 1.0f - (remaining / total);
        bar.name(Component.text("§c[쿨다운] §f" + skillName + " §7(" + String.format("%.1f", remaining) + "초)"));
        bar.progress(progress);
        bar.color(BossBar.Color.RED);
    }

    /**
     * BossBar를 숨깁니다.
     */
    public void hideBar(Player player, String barId) {
        Map<String, BossBar> bars = playerBars.get(player.getUniqueId());
        if (bars != null) {
            BossBar bar = bars.remove(barId);
            if (bar != null) {
                player.hideBossBar(bar);
            }
        }
    }

    /**
     * 모든 BossBar를 숨깁니다.
     */
    public void hideAllBars(Player player) {
        Map<String, BossBar> bars = playerBars.remove(player.getUniqueId());
        if (bars != null) {
            for (BossBar bar : bars.values()) {
                player.hideBossBar(bar);
            }
        }
    }
}
