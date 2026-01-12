package com.dreamwork.core.hook;

import com.dreamwork.core.DreamWorkCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Towny 플러그인 연동 헬퍼 클래스
 * <p>
 * Towny Advanced 플러그인이 있을 경우 타운 관련 기능을 제공합니다.
 * 리플렉션을 사용하여 Towny 의존성 없이 동작합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class TownyHook {

    private final DreamWorkCore plugin;
    private boolean townyEnabled = false;
    private Object townyAPI = null;

    public TownyHook(DreamWorkCore plugin) {
        this.plugin = plugin;
        checkTowny();
    }

    /**
     * Towny 플러그인 존재 여부를 확인합니다.
     */
    private void checkTowny() {
        if (Bukkit.getPluginManager().getPlugin("Towny") != null) {
            try {
                Class<?> townyAPIClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
                Method getInstance = townyAPIClass.getMethod("getInstance");
                townyAPI = getInstance.invoke(null);
                townyEnabled = true;
                plugin.getLogger().info("[TownyHook] Towny 연동 활성화!");
            } catch (Exception e) {
                plugin.getLogger().warning("[TownyHook] Towny 연동 실패: " + e.getMessage());
                townyEnabled = false;
            }
        }
    }

    /**
     * Towny 연동 여부를 반환합니다.
     */
    public boolean isEnabled() {
        return townyEnabled;
    }

    /**
     * 플레이어가 속한 타운 이름을 반환합니다.
     * 
     * @param player 플레이어
     * @return 타운 이름 또는 null
     */
    public String getPlayerTown(Player player) {
        if (!townyEnabled || townyAPI == null)
            return null;

        try {
            // TownyAPI.getInstance().getResident(player).getTownOrNull()
            Method getResident = townyAPI.getClass().getMethod("getResident", Player.class);
            Object resident = getResident.invoke(townyAPI, player);

            if (resident == null)
                return null;

            Method getTownOrNull = resident.getClass().getMethod("getTownOrNull");
            Object town = getTownOrNull.invoke(resident);

            if (town == null)
                return null;

            Method getName = town.getClass().getMethod("getName");
            return (String) getName.invoke(town);

        } catch (Exception e) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[TownyHook] 타운 조회 실패: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * 플레이어가 타운에 소속되어 있는지 확인합니다.
     */
    public boolean hasTown(Player player) {
        return getPlayerTown(player) != null;
    }

    /**
     * 타운 뱅크에 돈을 입금합니다.
     * 
     * @param player 플레이어 (타운 소속)
     * @param amount 금액
     * @return 성공 여부
     */
    public boolean depositToTownBank(Player player, double amount) {
        if (!townyEnabled || townyAPI == null)
            return false;

        try {
            // TownyAPI.getInstance().getResident(player).getTownOrNull().getAccount().deposit(amount,
            // "DreamWork Tax")
            Method getResident = townyAPI.getClass().getMethod("getResident", Player.class);
            Object resident = getResident.invoke(townyAPI, player);

            if (resident == null)
                return false;

            Method getTownOrNull = resident.getClass().getMethod("getTownOrNull");
            Object town = getTownOrNull.invoke(resident);

            if (town == null)
                return false;

            Method getAccount = town.getClass().getMethod("getAccount");
            Object account = getAccount.invoke(town);

            if (account == null)
                return false;

            Method deposit = account.getClass().getMethod("deposit", double.class, String.class);
            deposit.invoke(account, amount, "DreamWork 세금");

            return true;

        } catch (Exception e) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[TownyHook] 타운 뱅크 입금 실패: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * 타운 세금을 자동으로 차감합니다.
     * 
     * @param player  플레이어
     * @param income  수입
     * @param taxRate 세율 (0.0 ~ 1.0)
     * @return 실제로 플레이어가 받는 금액
     */
    public double applyTownTax(Player player, double income, double taxRate) {
        if (!townyEnabled || !hasTown(player)) {
            return income; // 타운 없으면 전액 지급
        }

        double tax = income * taxRate;
        double afterTax = income - tax;

        if (depositToTownBank(player, tax)) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[TownyHook] " + player.getName() + " 세금 납부: " +
                        String.format("%.0f", tax) + " -> " + getPlayerTown(player));
            }
            return afterTax;
        }

        return income; // 실패 시 전액 지급
    }
}
