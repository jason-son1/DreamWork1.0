package com.dreamwork.core.economy.shop;

import org.bukkit.inventory.ItemStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 상점에서 취급하는 아이템 정보
 */
@Getter
@RequiredArgsConstructor
public class ShopItem {
    private final ItemStack itemStack;
    private final double buyPrice; // 구매 가격 (유저가 살 때)
    private final double sellPrice; // 판매 가격 (유저가 팔 때)

    public boolean isBuyable() {
        return buyPrice > 0;
    }

    public boolean isSellable() {
        return sellPrice > 0;
    }
}
