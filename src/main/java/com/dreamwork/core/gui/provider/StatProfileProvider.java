package com.dreamwork.core.gui.provider;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.InventoryProvider;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.stat.StatManager.PlayerStats;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 스탯 프로필 UI Provider
 */
public class StatProfileProvider extends InventoryProvider {

    private final DreamWorkCore plugin;
    private final StatManager statManager;

    // 슬롯 상수
    private static final int SLOT_INFO = 4;
    private static final int SLOT_STR = 10;
    private static final int SLOT_DEX = 12;
    private static final int SLOT_CON = 14;
    private static final int SLOT_INT = 16;
    private static final int SLOT_LUCK = 22;

    public StatProfileProvider(Player player, DreamWorkCore plugin) {
        super(player);
        this.plugin = plugin;
        this.statManager = plugin.getStatManager();
    }

    @Override
    public void init(Inventory inv) {
        PlayerStats stats = statManager.getStats(player);

        // 정보 패널
        inv.setItem(SLOT_INFO, createInfoItem(stats));

        // 스탯 버튼들
        inv.setItem(SLOT_STR, createStatItem(Material.IRON_SWORD, "힘 (STR)",
                stats.getStr(), stats.getBaseStr(), "§c물리 데미지 증가"));
        inv.setItem(SLOT_DEX, createStatItem(Material.FEATHER, "민첩 (DEX)",
                stats.getDex(), stats.getBaseDex(), "§b채집 속도 & 이중 드롭"));
        inv.setItem(SLOT_CON, createStatItem(Material.GOLDEN_APPLE, "체력 (CON)",
                stats.getCon(), stats.getBaseCon(), "§a최대 체력 증가"));
        inv.setItem(SLOT_INT, createStatItem(Material.ENCHANTED_BOOK, "지능 (INT)",
                stats.getInt(), stats.getBaseInt(), "§d마법 데미지 & 마나"));
        inv.setItem(SLOT_LUCK, createStatItem(Material.EMERALD, "행운 (LUCK)",
                stats.getLuck(), stats.getBaseLuck(), "§e희귀 드롭 확률"));
    }

    private ItemStack createInfoItem(PlayerStats stats) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§e[ " + player.getName() + " ]"));
        meta.lore(List.of(
                Component.text(""),
                Component.text("§f남은 스탯 포인트: §b" + stats.getStatPoints()),
                Component.text(""),
                Component.text("§7스탯 아이콘을 클릭하여"),
                Component.text("§7포인트를 투자하세요.")));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatItem(Material material, String name, int total, int base, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        int bonus = total - base;

        meta.displayName(Component.text("§6" + name));
        meta.lore(List.of(
                Component.text(""),
                Component.text("§f총합: §a" + total + " §7(기본: " + base + " + 보너스: " + bonus + ")"),
                Component.text(""),
                Component.text(description),
                Component.text(""),
                Component.text("§e[좌클릭] §f1 포인트 투자"),
                Component.text("§e[Shift+좌클릭] §f5 포인트 투자")));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        PlayerStats stats = statManager.getStats(player);

        int amount = event.isShiftClick() ? 5 : 1;

        if (stats.getStatPoints() < amount) {
            player.sendMessage("§c스탯 포인트가 부족합니다.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            return;
        }

        boolean applied = false;

        switch (slot) {
            case SLOT_STR -> {
                stats.setStr(stats.getBaseStr() + amount);
                applied = true;
            }
            case SLOT_DEX -> {
                stats.setDex(stats.getBaseDex() + amount);
                applied = true;
            }
            case SLOT_CON -> {
                stats.setCon(stats.getBaseCon() + amount);
                applied = true;
            }
            case SLOT_INT -> {
                stats.setInt(stats.getBaseInt() + amount);
                applied = true;
            }
            case SLOT_LUCK -> {
                stats.setLuck(stats.getBaseLuck() + amount);
                applied = true;
            }
        }

        if (applied) {
            stats.setStatPoints(stats.getStatPoints() - amount);
            statManager.recalculateStats(player);
            refresh();
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.2f);
        }
    }
}
