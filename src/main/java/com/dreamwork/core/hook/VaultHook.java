package com.dreamwork.core.hook;

import com.dreamwork.core.DreamWorkCore;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

/**
 * Vault 경제 API 연동 래퍼
 * 
 * <p>
 * Vault의 Economy API를 래핑하여 안전하게 사용할 수 있도록 합니다.
 * Vault가 설치되어 있지 않아도 플러그인은 정상 동작합니다.
 * </p>
 * 
 * <h2>사용 예시:</h2>
 * 
 * <pre>{@code
 * VaultHook vault = DreamWorkCore.getInstance().getHookManager().getVaultHook();
 * if (vault != null && vault.isSetup()) {
 *     vault.give(player, 1000); // 1000원 지급
 *     double balance = vault.getBalance(player);
 * }
 * }</pre>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class VaultHook {

    private final DreamWorkCore plugin;

    /** Vault Economy 인스턴스 */
    private Economy economy;

    /** 설정 완료 여부 */
    private boolean setup = false;

    /**
     * VaultHook 생성자
     * 
     * @param plugin 플러그인 인스턴스
     */
    public VaultHook(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Vault Economy 서비스를 설정합니다.
     * 
     * @return 설정 성공 여부
     */
    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        setup = (economy != null);
        return setup;
    }

    /**
     * Economy 설정이 완료되었는지 확인합니다.
     * 
     * @return 설정 완료 여부
     */
    public boolean isSetup() {
        return setup && economy != null;
    }

    /**
     * 플레이어의 잔액을 조회합니다.
     * 
     * @param player 플레이어
     * @return 잔액 (설정되지 않은 경우 0)
     */
    public double getBalance(Player player) {
        if (!isSetup())
            return 0;
        return economy.getBalance(player);
    }

    /**
     * 오프라인 플레이어의 잔액을 조회합니다.
     * 
     * @param player 오프라인 플레이어
     * @return 잔액 (설정되지 않은 경우 0)
     */
    public double getBalance(OfflinePlayer player) {
        if (!isSetup())
            return 0;
        return economy.getBalance(player);
    }

    /**
     * UUID로 플레이어의 잔액을 조회합니다.
     * 
     * @param uuid 플레이어 UUID
     * @return 잔액 (설정되지 않은 경우 0)
     */
    public double getBalance(UUID uuid) {
        if (!isSetup())
            return 0;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return economy.getBalance(player);
    }

    /**
     * 플레이어에게 돈을 지급합니다.
     * 
     * @param player 플레이어
     * @param amount 지급 금액
     * @return 성공 여부
     */
    public boolean give(Player player, double amount) {
        if (!isSetup())
            return false;
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * 오프라인 플레이어에게 돈을 지급합니다.
     * 
     * @param player 오프라인 플레이어
     * @param amount 지급 금액
     * @return 성공 여부
     */
    public boolean give(OfflinePlayer player, double amount) {
        if (!isSetup())
            return false;
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * 플레이어의 돈을 차감합니다.
     * 
     * @param player 플레이어
     * @param amount 차감 금액
     * @return 성공 여부
     */
    public boolean take(Player player, double amount) {
        if (!isSetup())
            return false;
        if (!has(player, amount))
            return false;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * 오프라인 플레이어의 돈을 차감합니다.
     * 
     * @param player 오프라인 플레이어
     * @param amount 차감 금액
     * @return 성공 여부
     */
    public boolean take(OfflinePlayer player, double amount) {
        if (!isSetup())
            return false;
        if (!has(player, amount))
            return false;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * 플레이어가 특정 금액 이상을 보유하고 있는지 확인합니다.
     * 
     * @param player 플레이어
     * @param amount 확인할 금액
     * @return 보유 여부
     */
    public boolean has(Player player, double amount) {
        if (!isSetup())
            return false;
        return economy.has(player, amount);
    }

    /**
     * 오프라인 플레이어가 특정 금액 이상을 보유하고 있는지 확인합니다.
     * 
     * @param player 오프라인 플레이어
     * @param amount 확인할 금액
     * @return 보유 여부
     */
    public boolean has(OfflinePlayer player, double amount) {
        if (!isSetup())
            return false;
        return economy.has(player, amount);
    }

    /**
     * 화폐 단위 이름을 반환합니다 (단수형).
     * 
     * @return 화폐 이름 (예: "원", "Dollar")
     */
    public String getCurrencyName() {
        if (!isSetup())
            return "";
        return economy.currencyNameSingular();
    }

    /**
     * 화폐 단위 이름을 반환합니다 (복수형).
     * 
     * @return 화폐 이름 (예: "원", "Dollars")
     */
    public String getCurrencyNamePlural() {
        if (!isSetup())
            return "";
        return economy.currencyNamePlural();
    }

    /**
     * 금액을 포맷팅하여 반환합니다.
     * 
     * @param amount 금액
     * @return 포맷된 문자열 (예: "$1,000")
     */
    public String format(double amount) {
        if (!isSetup())
            return String.valueOf(amount);
        return economy.format(amount);
    }

    /**
     * Economy 인스턴스를 직접 반환합니다.
     * 
     * <p>
     * <b>주의:</b> null일 수 있으므로 isSetup() 확인 후 사용하세요.
     * </p>
     * 
     * @return Economy 인스턴스
     */
    public Economy getEconomy() {
        return economy;
    }
}
