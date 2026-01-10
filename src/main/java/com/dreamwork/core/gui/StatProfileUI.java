package com.dreamwork.core.gui;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.stat.StatManager.PlayerStats;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 스탯 프로필 및 투자 UI
 */
public class StatProfileUI extends InventoryManager {

    private final StatManager statManager;

    // 버튼 슬롯 상수
    private static final int SLOT_STR = 10;
    private static final int SLOT_DEX = 12;
    private static final int SLOT_CON = 14;
    private static final int SLOT_INT = 16;
    private static final int SLOT_LUCK = 22; // 중앙 하단

    public StatProfileUI(DreamWorkCore plugin, Player player) {
        super(plugin, player, 36, "스탯 프로필");
        this.statManager = plugin.getStatManager();
    }

    @Override
    protected void setup() {
        inventory.clear();

        PlayerStats stats = statManager.getStats(player);

        // 정보 표시
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("§e[ 내 정보 ]"));
        infoMeta.lore(List.of(
                Component.text("§f남은 스탯 포인트: §b" + stats.getStatPoints()),
                Component.text("§7클릭하여 스탯을 올리세요.")));
        info.setItemMeta(infoMeta);
        inventory.setItem(4, info);

        // 스탯 버튼 생성
        inventory.setItem(SLOT_STR, createStatIcon(Material.IRON_SWORD, "힘 (STR)", stats.getStr(), stats.getBaseStr()));
        inventory.setItem(SLOT_DEX, createStatIcon(Material.FEATHER, "민첩 (DEX)", stats.getDex(), stats.getBaseDex()));
        inventory.setItem(SLOT_CON,
                createStatIcon(Material.GOLDEN_APPLE, "체력 (CON)", stats.getCon(), stats.getBaseCon()));
        inventory.setItem(SLOT_INT,
                createStatIcon(Material.ENCHANTED_BOOK, "지능 (INT)", stats.getInt(), stats.getBaseInt()));
        inventory.setItem(SLOT_LUCK,
                createStatIcon(Material.EMERALD, "행운 (LUCK)", stats.getLuck(), stats.getBaseLuck()));
    }

    private ItemStack createStatIcon(Material material, String name, int total, int base) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§6" + name));

        int bonus = total - base;

        meta.lore(List.of(
                Component.text("§7Total: §f" + total + " §7(Base: " + base + " + Bonus: " + bonus + ")"),
                Component.text(""),
                Component.text("§e[좌클릭] §f1 포인트 투자")));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        PlayerStats stats = statManager.getStats(player);

        if (stats.getStatPoints() <= 0) {
            player.sendMessage("§c스탯 포인트가 부족합니다.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            return;
        }

        boolean changed = false;

        switch (slot) {
            case SLOT_STR -> {
                stats.setStr(stats.getBaseStr() + 1);
                changed = true;
            }
            case SLOT_DEX -> {
                stats.setDex(stats.getBaseDex() + 1);
                changed = true;
            }
            case SLOT_CON -> {
                stats.setCon(stats.getBaseCon() + 1);
                changed = true;
            }
            case SLOT_INT -> {
                stats.setInt(stats.getBaseInt() + 1);
                changed = true;
            }
            case SLOT_LUCK -> {
                stats.setLuck(stats.getBaseLuck() + 1);
                changed = true;
            }
        }

        if (changed) {
            // 포인트 차감 및 재계산
            stats.setStatPoints(stats.getStatPoints() - 1);
            statManager.recalculateStats(player);

            // 화면 갱신
            setup();

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.2f);
        }
    }
}
