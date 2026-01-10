package com.dreamwork.core.gui;

import com.dreamwork.core.DreamWorkCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * GUI 기본 매니저 (추상 클래스)
 */
public abstract class InventoryManager implements InventoryHolder {

    protected final DreamWorkCore plugin;
    protected final Player player;
    protected Inventory inventory;

    public InventoryManager(DreamWorkCore plugin, Player player, int size, String title) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, size, Component.text(title));
    }

    /**
     * GUI를 엽니다.
     */
    public void open() {
        setup();
        player.openInventory(inventory);
    }

    /**
     * GUI 구성요소를 배치합니다.
     */
    protected abstract void setup();

    /**
     * 클릭 이벤트를 처리합니다.
     */
    public abstract void onClick(InventoryClickEvent event);

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /**
     * 자원을 정리합니다.
     */
    public void close() {
        // 필요 시 구현
    }
}
