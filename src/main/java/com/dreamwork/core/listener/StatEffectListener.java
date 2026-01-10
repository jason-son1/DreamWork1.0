package com.dreamwork.core.listener;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.stat.StatManager.PlayerStats;
import com.dreamwork.core.stat.mechanic.GatheringMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

/**
 * 스탯 효과 리스너 (GatheringMechanic 통합 버전)
 * 
 * <p>
 * 채집 관련 스탯 효과를 적용합니다.
 * </p>
 * <ul>
 * <li>더블 드롭 (LUCK)</li>
 * <li>자동 제련 (INT)</li>
 * <li>내구도 보호 (DEX)</li>
 * </ul>
 */
public class StatEffectListener implements Listener {

    private final DreamWorkCore plugin;
    private final StatManager statManager;
    private final GatheringMechanic gatheringMechanic;

    public StatEffectListener(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.statManager = plugin.getStatManager();
        this.gatheringMechanic = new GatheringMechanic(plugin);
    }

    /**
     * 블록 파괴 시 채집 효과 적용
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // 자동 제련 체크
        if (gatheringMechanic.shouldAutoSmelt(player)) {
            applyAutoSmelt(event, player);
        }

        // 더블 드롭 체크
        if (gatheringMechanic.shouldDoubleDrop(player)) {
            applyDoubleDrop(event, player);
        }
    }

    /**
     * 자동 제련 적용
     */
    private void applyAutoSmelt(BlockBreakEvent event, Player player) {
        Material blockType = event.getBlock().getType();
        Material smelted = gatheringMechanic.getSmeltResult(blockType);

        if (smelted != null) {
            // 드롭 아이템 교체
            event.setDropItems(false);

            Collection<ItemStack> drops = event.getBlock().getDrops(player.getInventory().getItemInMainHand());
            for (ItemStack drop : drops) {
                ItemStack smeltedItem = gatheringMechanic.smeltItem(drop);
                event.getBlock().getWorld().dropItemNaturally(
                        event.getBlock().getLocation(), smeltedItem);
            }

            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Gathering] " + player.getName() + " 자동 제련 발동!");
            }
        }
    }

    /**
     * 더블 드롭 적용
     */
    private void applyDoubleDrop(BlockBreakEvent event, Player player) {
        Collection<ItemStack> drops = event.getBlock().getDrops(player.getInventory().getItemInMainHand());

        for (ItemStack drop : drops) {
            if (drop.getType() != Material.AIR) {
                event.getBlock().getWorld().dropItemNaturally(
                        event.getBlock().getLocation(), drop.clone());
            }
        }

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Gathering] " + player.getName() + " 더블 드롭 발동!");
        }
    }

    /**
     * 내구도 손실 방지
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();

        if (gatheringMechanic.preventDurabilityLoss(player)) {
            event.setCancelled(true);

            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Gathering] " + player.getName() + " 내구도 보호 발동!");
            }
        }
    }

    /**
     * GatheringMechanic 인스턴스를 반환합니다.
     */
    public GatheringMechanic getGatheringMechanic() {
        return gatheringMechanic;
    }
}
