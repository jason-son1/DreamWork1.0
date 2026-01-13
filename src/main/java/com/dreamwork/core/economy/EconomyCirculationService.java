package com.dreamwork.core.economy;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 경제 순환 서비스
 * 
 * <p>
 * Plan 2.0 기준:
 * - 경제 지표 모니터링 (인플레이션 감지)
 * - 세금 및 수수료 시스템
 * - 화폐 공급량 조절
 * - 보상 배율 자동 조정
 * </p>
 */
public class EconomyCirculationService extends Manager {

    private final DreamWorkCore plugin;

    /** 총 화폐 공급량 */
    private double totalMoneySupply = 0;

    /** 기준 1인당 화폐량 */
    private static final double BASELINE_PER_PLAYER = 10000.0;

    /** 현재 인플레이션 계수 (1.0 = 정상) */
    private double inflationMultiplier = 1.0;

    /** 세금률 (거래 수수료) */
    private double taxRate = 0.05; // 5%

    /** 플레이어별 일일 수입 추적 */
    private final Map<UUID, Double> dailyIncome = new ConcurrentHashMap<>();

    /** 플레이어별 일일 지출 추적 */
    private final Map<UUID, Double> dailyExpense = new ConcurrentHashMap<>();

    /** 서버 금고 (세금 누적) */
    private double serverTreasury = 0;

    public EconomyCirculationService(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        // 경제 지표 모니터링 (30분마다)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::updateEconomyMetrics, 20 * 60 * 30, 20 * 60 * 30);

        // 일일 리셋 (24시간마다)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::dailyReset, 20 * 60 * 60 * 24, 20 * 60 * 60 * 24);

        enabled = true;
        plugin.getLogger().info("EconomyCirculationService 활성화 완료");
    }

    @Override
    public void onDisable() {
        dailyIncome.clear();
        dailyExpense.clear();
        enabled = false;
    }

    @Override
    public void reload() {
        updateEconomyMetrics();
    }

    /**
     * 경제 지표 업데이트
     */
    private void updateEconomyMetrics() {
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        if (onlinePlayers == 0)
            return;

        // 1인당 평균 화폐량 계산
        double averageBalance = totalMoneySupply / Math.max(onlinePlayers, 1);

        // 인플레이션 계수 조정
        if (averageBalance > BASELINE_PER_PLAYER * 2) {
            inflationMultiplier = 0.7; // 보상 30% 감소
            taxRate = 0.10; // 세금 10%
        } else if (averageBalance > BASELINE_PER_PLAYER * 1.5) {
            inflationMultiplier = 0.85;
            taxRate = 0.07;
        } else if (averageBalance < BASELINE_PER_PLAYER * 0.5) {
            inflationMultiplier = 1.3; // 보상 30% 증가
            taxRate = 0.03;
        } else {
            inflationMultiplier = 1.0;
            taxRate = 0.05;
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Economy] 인플레이션 계수: " + inflationMultiplier +
                    ", 세금률: " + (taxRate * 100) + "%");
        }
    }

    /**
     * 일일 리셋
     */
    private void dailyReset() {
        dailyIncome.clear();
        dailyExpense.clear();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Economy] 일일 경제 지표 리셋 완료");
        }
    }

    /**
     * 보상에 인플레이션 계수 적용
     */
    public double applyInflationModifier(double baseReward) {
        return baseReward * inflationMultiplier;
    }

    /**
     * 거래에 세금 적용
     */
    public double applyTax(double amount) {
        double tax = amount * taxRate;
        serverTreasury += tax;
        return amount - tax;
    }

    /**
     * 수입 추적
     */
    public void trackIncome(UUID uuid, double amount) {
        dailyIncome.merge(uuid, amount, Double::sum);
        totalMoneySupply += amount;
    }

    /**
     * 지출 추적
     */
    public void trackExpense(UUID uuid, double amount) {
        dailyExpense.merge(uuid, amount, Double::sum);
        totalMoneySupply -= amount;
    }

    /**
     * 직업별 수입 보너스 계산
     */
    public double getJobIncomeBonus(String jobId, int level) {
        // 레벨당 1% 보너스
        double bonus = 1.0 + (level * 0.01);

        // 직업별 추가 보너스 (탐험가는 거래 보너스)
        if ("explorer".equals(jobId)) {
            bonus += 0.1; // 탐험가 10% 추가
        }

        return bonus * inflationMultiplier;
    }

    /**
     * 플레이어 일일 수입 조회
     */
    public double getDailyIncome(UUID uuid) {
        return dailyIncome.getOrDefault(uuid, 0.0);
    }

    /**
     * 플레이어 일일 지출 조회
     */
    public double getDailyExpense(UUID uuid) {
        return dailyExpense.getOrDefault(uuid, 0.0);
    }

    /**
     * 서버 금고 잔액
     */
    public double getServerTreasury() {
        return serverTreasury;
    }

    /**
     * 현재 인플레이션 계수
     */
    public double getInflationMultiplier() {
        return inflationMultiplier;
    }

    /**
     * 현재 세금률
     */
    public double getTaxRate() {
        return taxRate;
    }

    /**
     * 총 화폐 공급량 설정
     */
    public void setTotalMoneySupply(double amount) {
        this.totalMoneySupply = amount;
    }
}
