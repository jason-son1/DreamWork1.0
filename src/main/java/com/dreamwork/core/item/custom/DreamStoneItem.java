package com.dreamwork.core.item.custom;

import com.dreamwork.core.DreamWorkCore;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 드림 스톤 (Dream Stone) 드롭 시스템
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class DreamStoneItem implements Listener {

    private final DreamWorkCore plugin;

    /** 기본 드롭 확률 (0.1%) */
    private static final double DROP_CHANCE = 0.001;

    public DreamStoneItem(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isOre(event.getBlock().getType()))
            return;

        Player player = event.getPlayer();

        // 확률 계산
        double chance = DROP_CHANCE;

        // 콤보 보너스 적용
        if (plugin.getMiningComboSystem() != null) {
            int combo = plugin.getMiningComboSystem().getCombo(player.getUniqueId());
            if (combo >= 100) {
                chance *= 3.0; // 100콤보: 3배
            } else if (combo >= 50) {
                chance *= 2.0; // 50콤보: 2배
            }
        }

        if (ThreadLocalRandom.current().nextDouble() < chance) {
            ItemStack item = plugin.getItemFactory().createItem("dream_stone");
            if (item != null) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item);

                player.sendMessage("§d§l[대박!] §5드림 스톤§f을 발견했습니다!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
            }
        }
    }

    private boolean isOre(Material material) {
        return material.name().contains("ORE") || material == Material.ANCIENT_DEBRIS;
    }
}
