package com.dreamwork.core.gui;

import com.dreamwork.core.DreamWorkCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SmartInventory GUI 관리 시스템
 * 
 * <p>
 * 모든 커스텀 GUI를 중앙에서 관리하고 이벤트를 라우팅합니다.
 * </p>
 * 
 * <h2>사용법:</h2>
 * 
 * <pre>{@code
 * SmartInventory.builder()
 *         .title("직업 선택")
 *         .size(27)
 *         .provider(new JobSelectionProvider(player))
 *         .build()
 *         .open(player);
 * }</pre>
 */
public class SmartInventory implements Listener {

    private static final Map<UUID, SmartInventory> openInventories = new ConcurrentHashMap<>();

    private final String title;
    private final int size;
    private final InventoryProvider provider;
    private Inventory inventory;

    private SmartInventory(Builder builder) {
        this.title = builder.title;
        this.size = builder.size;
        this.provider = builder.provider;
    }

    /**
     * GUI를 엽니다.
     * 
     * @param player 플레이어
     */
    public void open(Player player) {
        this.inventory = Bukkit.createInventory(null, size, Component.text(title));
        provider.setInventory(inventory);
        provider.init(inventory);

        openInventories.put(player.getUniqueId(), this);
        player.openInventory(inventory);
    }

    /**
     * GUI를 갱신합니다.
     */
    public void refresh() {
        provider.refresh();
    }

    // ==================== 이벤트 핸들러 (Static Listener) ====================

    @EventHandler(priority = EventPriority.HIGH)
    public static void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        SmartInventory smartInv = openInventories.get(player.getUniqueId());
        if (smartInv == null)
            return;

        // 현재 열린 인벤토리와 일치하는지 확인
        if (!event.getInventory().equals(smartInv.inventory))
            return;

        // Interactive 모드가 아닐 때만 자동 취소 및 타 인벤토리 무시
        if (!smartInv.provider.isInteractive()) {
            event.setCancelled(true);

            if (event.getClickedInventory() == null)
                return;
            if (!event.getClickedInventory().equals(smartInv.inventory))
                return;
        }

        smartInv.provider.onClick(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public static void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;

        SmartInventory smartInv = openInventories.remove(player.getUniqueId());
        if (smartInv != null) {
            smartInv.provider.onClose(event);
        }
    }

    // ==================== 리스너 등록 ====================

    /**
     * 이벤트 리스너를 등록합니다.
     * DreamWorkCore.onEnable()에서 호출해야 합니다.
     */
    public static void registerListener(DreamWorkCore plugin) {
        plugin.getServer().getPluginManager().registerEvents(new SmartInventoryListener(), plugin);
    }

    /**
     * 내부 리스너 클래스 (static 메서드 우회용)
     */
    private static class SmartInventoryListener implements Listener {
        @EventHandler(priority = EventPriority.HIGH)
        public void onClick(InventoryClickEvent event) {
            SmartInventory.onInventoryClick(event);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onClose(InventoryCloseEvent event) {
            SmartInventory.onInventoryClose(event);
        }
    }

    // ==================== Builder ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title = "GUI";
        private int size = 27;
        private InventoryProvider provider;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder size(int size) {
            // 9의 배수로 조정
            this.size = Math.max(9, Math.min(54, (size / 9) * 9));
            if (this.size == 0)
                this.size = 9;
            return this;
        }

        public Builder provider(InventoryProvider provider) {
            this.provider = provider;
            return this;
        }

        public SmartInventory build() {
            if (provider == null) {
                throw new IllegalArgumentException("InventoryProvider is required");
            }
            return new SmartInventory(this);
        }
    }
}
