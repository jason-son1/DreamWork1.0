package com.dreamwork.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

/**
 * GUI Provider 추상 클래스
 * 
 * <p>
 * 모든 커스텀 GUI는 이 클래스를 상속받아 구현합니다.
 * </p>
 */
public abstract class InventoryProvider {

    protected final Player player;
    protected Inventory inventory;

    public InventoryProvider(Player player) {
        this.player = player;
    }

    /**
     * GUI를 초기화하고 아이템을 배치합니다.
     * 
     * @param inv 인벤토리 인스턴스
     */
    public abstract void init(Inventory inv);

    /**
     * 클릭 이벤트를 처리합니다.
     * 
     * @param event 클릭 이벤트
     */
    public abstract void onClick(InventoryClickEvent event);

    /**
     * GUI가 닫힐 때 호출됩니다.
     * 
     * @param event 닫기 이벤트
     */
    public void onClose(InventoryCloseEvent event) {
        // 기본 구현: 아무것도 하지 않음
    }

    /**
     * GUI를 갱신합니다.
     */
    public void refresh() {
        if (inventory != null) {
            inventory.clear();
            init(inventory);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
