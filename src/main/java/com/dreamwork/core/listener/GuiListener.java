package com.dreamwork.core.listener;

import com.dreamwork.core.gui.InventoryManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * GUI 클릭 이벤트 리스너
 */
public class GuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof InventoryManager gui) {
            // GUI 클릭 시 기본적으로 이벤트 취소 (아이템 이동 방지)
            event.setCancelled(true);

            // 인벤토리 영역 밖 클릭 무시
            if (event.getClickedInventory() == null)
                return;

            // 자신의 인벤토리 클릭 시 무시는 선택사항이지만, 보통 GUI에서는 막음
            // if (event.getClickedInventory().getType() == InventoryType.PLAYER) return;

            gui.onClick(event);
        }
    }
}
