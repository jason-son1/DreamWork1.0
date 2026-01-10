package com.dreamwork.core.hook;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.manager.Manager;
import org.bukkit.Bukkit;

/**
 * 외부 플러그인 연동 관리 매니저
 * 
 * <p>
 * Vault, PlaceholderAPI 등 외부 플러그인의 활성화 여부를 체크하고
 * 안전한 후킹 시스템을 제공합니다.
 * </p>
 * 
 * <p>
 * <b>Soft Dependency:</b> 외부 플러그인이 없어도 DreamWork Core는 정상 동작합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class HookManager extends Manager {

    private final DreamWorkCore plugin;

    /** Vault 경제 후킹 */
    private VaultHook vaultHook;

    /** PlaceholderAPI 후킹 */
    private PapiHook papiHook;

    /**
     * HookManager 생성자
     * 
     * @param plugin 플러그인 인스턴스
     */
    public HookManager(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        // Vault 후킹
        if (plugin.getConfig().getBoolean("hooks.vault-enabled", true)) {
            setupVault();
        }

        // PlaceholderAPI 후킹
        if (plugin.getConfig().getBoolean("hooks.placeholderapi-enabled", true)) {
            setupPlaceholderAPI();
        }

        enabled = true;
        plugin.getLogger().info("HookManager 활성화 완료");
    }

    @Override
    public void onDisable() {
        enabled = false;

        // PlaceholderAPI 등록 해제
        if (papiHook != null && papiHook.isRegistered()) {
            papiHook.unregister();
        }

        plugin.getLogger().info("HookManager 비활성화 완료");
    }

    @Override
    public void reload() {
        // 설정에 따라 후킹 재설정
        if (plugin.getConfig().getBoolean("hooks.vault-enabled", true) && vaultHook == null) {
            setupVault();
        }

        if (plugin.getConfig().getBoolean("hooks.placeholderapi-enabled", true) && papiHook == null) {
            setupPlaceholderAPI();
        }
    }

    /**
     * Vault 플러그인 연동을 설정합니다.
     */
    private void setupVault() {
        if (!isPluginEnabled("Vault")) {
            plugin.getLogger().info("Vault 플러그인을 찾을 수 없습니다. 경제 기능이 비활성화됩니다.");
            return;
        }

        vaultHook = new VaultHook(plugin);
        if (vaultHook.setup()) {
            plugin.getLogger().info("Vault 연동 완료!");
        } else {
            plugin.getLogger().warning("Vault 경제 서비스를 찾을 수 없습니다. Essentials 등이 설치되어 있는지 확인하세요.");
            vaultHook = null;
        }
    }

    /**
     * PlaceholderAPI 연동을 설정합니다.
     */
    private void setupPlaceholderAPI() {
        if (!isPluginEnabled("PlaceholderAPI")) {
            plugin.getLogger().info("PlaceholderAPI를 찾을 수 없습니다. Placeholder 기능이 비활성화됩니다.");
            return;
        }

        papiHook = new PapiHook(plugin);
        papiHook.register();
        plugin.getLogger().info("PlaceholderAPI 연동 완료!");
    }

    /**
     * 특정 플러그인이 활성화되어 있는지 확인합니다.
     * 
     * @param pluginName 플러그인 이름
     * @return 활성화 여부
     */
    public boolean isPluginEnabled(String pluginName) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }

    /**
     * Vault 후킹이 활성화되어 있는지 확인합니다.
     * 
     * @return Vault 활성화 여부
     */
    public boolean isVaultEnabled() {
        return vaultHook != null && vaultHook.isSetup();
    }

    /**
     * PlaceholderAPI 후킹이 활성화되어 있는지 확인합니다.
     * 
     * @return PAPI 활성화 여부
     */
    public boolean isPapiEnabled() {
        return papiHook != null && papiHook.isRegistered();
    }

    /**
     * Vault 후킹 인스턴스를 반환합니다.
     * 
     * @return VaultHook 인스턴스 (없으면 null)
     */
    public VaultHook getVaultHook() {
        return vaultHook;
    }

    /**
     * PlaceholderAPI 후킹 인스턴스를 반환합니다.
     * 
     * @return PapiHook 인스턴스 (없으면 null)
     */
    public PapiHook getPapiHook() {
        return papiHook;
    }
}
