package com.dreamwork.core.economy.shop;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.manager.Manager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager extends Manager {

    private final DreamWorkCore plugin;
    private final Map<String, List<ShopItem>> shops = new HashMap<>();

    public ShopManager(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        initializeShops();
    }

    @Override
    public void onDisable() {
        shops.clear();
    }

    @Override
    public String getName() {
        return "ShopManager";
    }

    public void reload() {
        shops.clear();
        initializeShops();
    }

    private void initializeShops() {
        // 잡화점 (General Store)
        List<ShopItem> general = new ArrayList<>();
        shops.clear();

        // 1. 잡화점 (General)
        general.add(new ShopItem(new ItemStack(Material.BREAD), 50, 10));
        general.add(new ShopItem(new ItemStack(Material.COOKED_BEEF), 100, 20));
        general.add(new ShopItem(new ItemStack(Material.TORCH), 10, 0));
        general.add(new ShopItem(new ItemStack(Material.OAK_LOG), 20, 5));
        shops.put("general", general);

        // 2. 광부 조합 (Miner)
        List<ShopItem> miner = new ArrayList<>();
        miner.add(new ShopItem(new ItemStack(Material.COAL), 20, 10));
        miner.add(new ShopItem(new ItemStack(Material.RAW_IRON), 50, 25));
        miner.add(new ShopItem(new ItemStack(Material.RAW_GOLD), 80, 40));
        miner.add(new ShopItem(new ItemStack(Material.IRON_INGOT), 100, 50));
        miner.add(new ShopItem(new ItemStack(Material.GOLD_INGOT), 150, 75));
        miner.add(new ShopItem(new ItemStack(Material.DIAMOND), 800, 400));
        miner.add(new ShopItem(new ItemStack(Material.EMERALD), 600, 300));
        miner.add(new ShopItem(new ItemStack(Material.AMETHYST_SHARD), 100, 50));
        // 도구 판매
        miner.add(new ShopItem(new ItemStack(Material.IRON_PICKAXE), 500, 100));
        shops.put("miner", miner);

        // 3. 농부 시장 (Farmer)
        List<ShopItem> farmer = new ArrayList<>();
        farmer.add(new ShopItem(new ItemStack(Material.WHEAT), 30, 15));
        farmer.add(new ShopItem(new ItemStack(Material.POTATO), 20, 10));
        farmer.add(new ShopItem(new ItemStack(Material.CARROT), 20, 10));
        farmer.add(new ShopItem(new ItemStack(Material.BEETROOT), 25, 12));
        farmer.add(new ShopItem(new ItemStack(Material.PUMPKIN), 40, 20));
        farmer.add(new ShopItem(new ItemStack(Material.MELON_SLICE), 10, 5));
        // 씨앗류
        farmer.add(new ShopItem(new ItemStack(Material.WHEAT_SEEDS), 10, 2));
        farmer.add(new ShopItem(new ItemStack(Material.PUMPKIN_SEEDS), 20, 5));
        farmer.add(new ShopItem(new ItemStack(Material.MELON_SEEDS), 20, 5));
        shops.put("farmer", farmer);

        // 4. 어부 조합 (Fisher)
        List<ShopItem> fisher = new ArrayList<>();
        fisher.add(new ShopItem(new ItemStack(Material.COD), 40, 20));
        fisher.add(new ShopItem(new ItemStack(Material.SALMON), 50, 25));
        fisher.add(new ShopItem(new ItemStack(Material.TROPICAL_FISH), 100, 50));
        fisher.add(new ShopItem(new ItemStack(Material.PUFFERFISH), 150, 75));
        // 낚시대
        fisher.add(new ShopItem(new ItemStack(Material.FISHING_ROD), 300, 50));
        shops.put("fisher", fisher);
    }

    public List<ShopItem> getShopItems(String shopId) {
        return shops.getOrDefault(shopId, new ArrayList<>());
    }

    public String getShopTitle(String shopId) {
        return switch (shopId) {
            case "general" -> "잡화점";
            case "miner" -> "광부 조합 거래소";
            case "farmer" -> "농부 청과물 시장";
            case "fisher" -> "어부 수산 시장";
            default -> "상점: " + shopId;
        };
    }

    public void openShop(Player player, String shopId) {
        List<ShopItem> items = getShopItems(shopId);
        if (items.isEmpty()) {
            player.sendMessage("§c존재하지 않거나 비어있는 상점입니다: " + shopId);
            return;
        }

        String title = getShopTitle(shopId);
        new com.dreamwork.core.gui.provider.ShopUIProvider(player, plugin, shopId, title, items).open(player);
    }
}
