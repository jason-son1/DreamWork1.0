package com.dreamwork.core.gui.provider;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.economy.shop.ShopItem;
import com.dreamwork.core.gui.InventoryProvider;
import com.dreamwork.core.gui.SmartInventory;
import com.dreamwork.core.model.UserData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopUIProvider extends InventoryProvider {

    private final DreamWorkCore plugin;
    private final String shopId;
    private final List<ShopItem> items;
    private final String title;
    private final int size;

    public ShopUIProvider(Player player, DreamWorkCore plugin, String shopId, String title, List<ShopItem> items) {
        super(player);
        this.plugin = plugin;
        this.shopId = shopId;
        this.items = items;
        this.title = title;
        this.size = 54;
    }

    public void open(Player player) {
        SmartInventory.builder()
                .title(title)
                .size(size)
                .provider(this)
                .build()
                .open(player);
    }

    @Override
    public void init(Inventory inv) {
        // 아이템 배치
        int slot = 0;
        for (ShopItem shopItem : items) {
            if (slot >= size)
                break;

            ItemStack displayItem = shopItem.getItemStack().clone();
            ItemMeta meta = displayItem.getItemMeta();
            List<Component> lore = new ArrayList<>();

            if (meta.hasLore()) {
                // 기존 로어 변환 (생략)
            }

            lore.add(Component.text(""));
            if (shopItem.isBuyable()) {
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize("§e[좌클릭] §f구매: §6" + (int) shopItem.getBuyPrice() + "D"));
            } else {
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§c[구매 불가]"));
            }

            if (shopItem.isSellable()) {
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize("§b[우클릭] §f판매: §6" + (int) shopItem.getSellPrice() + "D"));
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7(Shift+우클릭: 모두 판매)"));
            } else {
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§c[판매 불가]"));
            }

            meta.lore(lore);
            displayItem.setItemMeta(meta);

            inv.setItem(slot, displayItem);
            slot++;
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot >= items.size())
            return;

        ShopItem shopItem = items.get(slot);
        Player player = getPlayer();
        UserData userData = plugin.getStorageManager().getUserData(player.getUniqueId());

        if (userData == null)
            return;

        // 구매 로직 (좌클릭)
        if (event.isLeftClick()) {
            if (!shopItem.isBuyable()) {
                player.sendMessage("§c구매할 수 없는 아이템입니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (userData.hasMoney(shopItem.getBuyPrice())) {
                // 인벤토리 공간 확인
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage("§c인벤토리가 가득 찼습니다.");
                    return;
                }

                userData.removeMoney(shopItem.getBuyPrice());
                // 수량 1개 지급
                ItemStack toGive = shopItem.getItemStack().clone();
                toGive.setAmount(1);
                player.getInventory().addItem(toGive);

                player.sendMessage("§a구매 완료: " + itemName(shopItem.getItemStack()));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            } else {
                player.sendMessage("§c돈이 부족합니다.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
        // 판매 로직 (우클릭)
        else if (event.isRightClick()) {
            if (!shopItem.isSellable()) {
                player.sendMessage("§c판매할 수 없는 아이템입니다.");
                return;
            }

            if (event.isShiftClick()) {
                // 모두 판매
                sellAllAndProcess(player, userData, shopItem);
            } else {
                // 1개 판매
                sellOneAndProcess(player, userData, shopItem);
            }
        }
    }

    private void sellOneAndProcess(Player player, UserData userData, ShopItem shopItem) {
        if (removeItemFromInventory(player, shopItem.getItemStack(), 1)) {
            userData.addMoney(shopItem.getSellPrice());
            player.sendMessage(
                    "§e판매 완료: " + itemName(shopItem.getItemStack()) + " (+" + (int) shopItem.getSellPrice() + "D)");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        } else {
            player.sendMessage("§c판매할 아이템이 부족합니다.");
        }
    }

    private void sellAllAndProcess(Player player, UserData userData, ShopItem shopItem) {
        int count = countItem(player, shopItem.getItemStack());
        if (count <= 0) {
            player.sendMessage("§c판매할 아이템이 없습니다.");
            return;
        }

        removeItemFromInventory(player, shopItem.getItemStack(), count);
        double totalEarned = shopItem.getSellPrice() * count;
        userData.addMoney(totalEarned);
        player.sendMessage(
                "§e모두 판매 완료: " + itemName(shopItem.getItemStack()) + " x" + count + " (+" + (int) totalEarned + "D)");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
    }

    private String itemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }

    private int countItem(Player player, ItemStack target) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.isSimilar(target)) {
                count += is.getAmount();
            }
        }
        return count;
    }

    // 유틸리티: 인벤토리에서 아이템 제거
    private boolean removeItemFromInventory(Player player, ItemStack target, int amount) {
        if (!player.getInventory().containsAtLeast(target, amount))
            return false;

        ItemStack toRemove = target.clone();
        toRemove.setAmount(amount);
        player.getInventory().removeItem(toRemove);

        return true;
    }
}
