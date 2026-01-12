package com.dreamwork.core.job.system;

import com.dreamwork.core.DreamWorkCore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 광부 채굴 콤보 시스템
 * <p>
 * 광물을 연속으로 캘수록 콤보가 쌓이며 보너스를 제공합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class MiningComboSystem implements Listener {

    private final DreamWorkCore plugin;

    /** 플레이어별 콤보 데이터 */
    private final Map<UUID, ComboData> comboMap = new HashMap<>();

    /** 콤보 초기화 시간 (틱 단위: 5초 = 100틱) */
    private static final long COMBO_TIMEOUT = 100L;

    /** 콤보 단계별 보상 */
    private static final int COMBO_TIER_1 = 30; // 이동 속도 증가
    private static final int COMBO_TIER_2 = 50; // 경험치 배수
    private static final int COMBO_TIER_3 = 100; // 드롭률 증가

    public MiningComboSystem(DreamWorkCore plugin) {
        this.plugin = plugin;

        // 1틱마다 콤보 타임아웃 체크
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkComboTimeouts, 1L, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isOre(event.getBlock().getType().name()))
            return;

        Player player = event.getPlayer();

        // 광부 직업인지 확인
        var userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null)
            return;

        UUID playerId = player.getUniqueId();
        ComboData data = comboMap.computeIfAbsent(playerId, k -> new ComboData());

        // 콤보 증가
        data.combo++;
        data.lastMineTime = System.currentTimeMillis();

        // ActionBar에 콤보 표시
        displayCombo(player, data.combo);

        // 콤보 보상 적용
        applyComboBonus(player, data.combo);
    }

    /**
     * 콤보 타임아웃 체크 (1틱마다)
     */
    private void checkComboTimeouts() {
        long currentTime = System.currentTimeMillis();

        comboMap.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            ComboData data = entry.getValue();

            // 5초(100틱) 이상 채굴하지 않으면 콤보 초기화
            if (currentTime - data.lastMineTime > (COMBO_TIMEOUT * 50)) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    // 콤보 중단 메시지
                    player.sendActionBar(Component.text("채굴 콤보 중단!", NamedTextColor.RED));
                }
                return true; // 맵에서 제거
            }
            return false;
        });
    }

    /**
     * ActionBar에 콤보 표시
     */
    private void displayCombo(Player player, int combo) {
        NamedTextColor color = NamedTextColor.YELLOW;

        if (combo >= COMBO_TIER_3) {
            color = NamedTextColor.GOLD;
        } else if (combo >= COMBO_TIER_2) {
            color = NamedTextColor.GREEN;
        } else if (combo >= COMBO_TIER_1) {
            color = NamedTextColor.AQUA;
        }

        Component message = Component.text("⛏ ", color, TextDecoration.BOLD)
                .append(Component.text("콤보: ", NamedTextColor.WHITE))
                .append(Component.text(combo, color, TextDecoration.BOLD))
                .append(Component.text(" ×", NamedTextColor.WHITE));

        player.sendActionBar(message);
    }

    /**
     * 콤보 단계별 보너스 적용
     */
    private void applyComboBonus(Player player, int combo) {
        // 30 콤보: 이동 속도 증가
        if (combo == COMBO_TIER_1) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
            player.sendMessage("§e[콤보] §a이동 속도가 증가했습니다! (30 Combo)");
        }

        // 50 콤보: 경험치 배수 (실제 경험치 배수는 JobManager에서 처리)
        if (combo == COMBO_TIER_2) {
            player.sendMessage("§e[콤보] §b경험치 획득량이 2배가 되었습니다! (50 Combo)");
        }

        // 100 콤보: 드롭률 증가 + 파티클
        if (combo == COMBO_TIER_3) {
            player.sendMessage("§6§l[콤보 마스터!] §e드롭률이 크게 증가했습니다! (100 Combo)");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        }
    }

    /**
     * 콤보 상태 확인 (다른 시스템에서 참조 가능)
     */
    public int getCombo(UUID playerId) {
        ComboData data = comboMap.get(playerId);
        return data != null ? data.combo : 0;
    }

    /**
     * 경험치 배수 계산 (JobManager에서 호출)
     */
    public double getExpMultiplier(UUID playerId) {
        int combo = getCombo(playerId);

        if (combo >= COMBO_TIER_3) {
            return 3.0; // 100 콤보: 3배
        } else if (combo >= COMBO_TIER_2) {
            return 2.0; // 50 콤보: 2배
        }
        return 1.0; // 기본
    }

    private boolean isOre(String materialName) {
        return materialName.contains("ORE") || materialName.equals("ANCIENT_DEBRIS");
    }

    /**
     * 콤보 데이터 저장 클래스
     */
    private static class ComboData {
        int combo = 0;
        long lastMineTime = System.currentTimeMillis();
    }
}
