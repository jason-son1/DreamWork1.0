package com.dreamwork.core;

import com.dreamwork.core.hook.HookManager;
import com.dreamwork.core.manager.Manager;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.storage.StorageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * DreamWork Core 메인 플러그인 클래스
 * 
 * <p>야생 + 타운 + 성장형 RPG 서버를 위한 핵심 플러그인입니다.
 * Manager Pattern을 사용하여 각 기능을 모듈화합니다.</p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public final class DreamWorkCore extends JavaPlugin {
    
    /** Singleton 인스턴스 */
    private static DreamWorkCore instance;
    
    /** 등록된 매니저 목록 */
    private final List<Manager> managers = new ArrayList<>();
    
    /** 저장소 매니저 */
    private StorageManager storageManager;
    
    /** 외부 플러그인 연동 매니저 */
    private HookManager hookManager;
    
    /** 스탯 매니저 */
    private StatManager statManager;
    
    /**
     * 플러그인 인스턴스를 반환합니다.
     * 
     * @return DreamWorkCore 인스턴스
     * @throws IllegalStateException 플러그인이 초기화되지 않은 경우
     */
    public static DreamWorkCore getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DreamWorkCore가 아직 초기화되지 않았습니다!");
        }
        return instance;
    }
    
    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();
        
        // 설정 파일 초기화
        saveDefaultConfig();
        
        // 매니저 초기화
        initializeManagers();
        
        // 매니저 활성화
        enableManagers();
        
        long endTime = System.currentTimeMillis();
        getLogger().info("DreamWork Core v" + getDescription().getVersion() + " 활성화 완료! (" + (endTime - startTime) + "ms)");
    }
    
    @Override
    public void onDisable() {
        // 매니저 비활성화 (역순)
        disableManagers();
        
        getLogger().info("DreamWork Core 비활성화 완료!");
        instance = null;
    }
    
    /**
     * 모든 매니저를 초기화합니다.
     */
    private void initializeManagers() {
        // 저장소 매니저 (가장 먼저 초기화)
        storageManager = new StorageManager(this);
        registerManager(storageManager);
        
        // 외부 플러그인 연동 매니저
        hookManager = new HookManager(this);
        registerManager(hookManager);
        
        // 스탯 매니저
        statManager = new StatManager(this);
        registerManager(statManager);
        
        getLogger().info(managers.size() + "개의 매니저가 초기화되었습니다.");
    }
    
    /**
     * 매니저를 등록합니다.
     * 
     * @param manager 등록할 매니저
     */
    private void registerManager(Manager manager) {
        managers.add(manager);
        if (isDebugMode()) {
            getLogger().info("[Debug] 매니저 등록: " + manager.getName());
        }
    }
    
    /**
     * 모든 매니저를 활성화합니다.
     */
    private void enableManagers() {
        for (Manager manager : managers) {
            try {
                manager.onEnable();
                if (isDebugMode()) {
                    getLogger().info("[Debug] 매니저 활성화: " + manager.getName());
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, manager.getName() + " 매니저 활성화 중 오류 발생!", e);
            }
        }
    }
    
    /**
     * 모든 매니저를 비활성화합니다. (역순)
     */
    private void disableManagers() {
        for (int i = managers.size() - 1; i >= 0; i--) {
            Manager manager = managers.get(i);
            try {
                manager.onDisable();
                if (isDebugMode()) {
                    getLogger().info("[Debug] 매니저 비활성화: " + manager.getName());
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, manager.getName() + " 매니저 비활성화 중 오류 발생!", e);
            }
        }
    }
    
    /**
     * 모든 매니저 설정을 리로드합니다.
     */
    public void reloadAllManagers() {
        reloadConfig();
        for (Manager manager : managers) {
            try {
                manager.reload();
                if (isDebugMode()) {
                    getLogger().info("[Debug] 매니저 리로드: " + manager.getName());
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, manager.getName() + " 매니저 리로드 중 오류 발생!", e);
            }
        }
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("dreamwork")) {
            if (args.length == 0) {
                sendMessage(sender, "&6DreamWork Core &7v" + getDescription().getVersion());
                sendMessage(sender, "&7사용법: /dw [reload|help]");
                return true;
            }
            
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "reload" -> {
                    if (!sender.hasPermission("dreamwork.reload")) {
                        sendMessage(sender, getMessage("no-permission"));
                        return true;
                    }
                    reloadAllManagers();
                    sendMessage(sender, getMessage("reload-success"));
                }
                case "help" -> {
                    sendMessage(sender, "&6=== DreamWork Core 도움말 ===");
                    sendMessage(sender, "&e/dw reload &7- 설정 리로드");
                    sendMessage(sender, "&e/dw help &7- 도움말 표시");
                }
                default -> sendMessage(sender, getMessage("unknown-command"));
            }
            return true;
        }
        return false;
    }
    
    /**
     * 메시지를 전송합니다.
     * 
     * @param sender 수신자
     * @param message 메시지 (색상 코드 포함 가능)
     */
    public void sendMessage(CommandSender sender, String message) {
        String prefix = getConfig().getString("messages.prefix", "&8[&6DreamWork&8] &f");
        Component component = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(prefix + message);
        sender.sendMessage(component);
    }
    
    /**
     * 설정 파일에서 메시지를 가져옵니다.
     * 
     * @param key 메시지 키
     * @return 메시지 문자열
     */
    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "&c메시지를 찾을 수 없습니다: " + key);
    }
    
    /**
     * 디버그 모드 여부를 반환합니다.
     * 
     * @return 디버그 모드 활성화 상태
     */
    public boolean isDebugMode() {
        return getConfig().getBoolean("general.debug", false);
    }
    
    /**
     * 저장소 매니저를 반환합니다.
     * 
     * @return StorageManager 인스턴스
     */
    public StorageManager getStorageManager() {
        return storageManager;
    }
    
    /**
     * 외부 플러그인 연동 매니저를 반환합니다.
     * 
     * @return HookManager 인스턴스
     */
    public HookManager getHookManager() {
        return hookManager;
    }
    
    /**
     * 스탯 매니저를 반환합니다.
     * 
     * @return StatManager 인스턴스
     */
    public StatManager getStatManager() {
        return statManager;
    }
}
