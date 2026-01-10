package com.dreamwork.core.listener;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.stat.StatManager.PlayerStats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Random;

/**
 * 스탯 효과 리스너
 * 
 * <p>
 * 스탯이 실제 게임 플레이에 영향을 주도록 처리합니다.
 * - STR: 물리 데미지 증가
 * - DEX: 채집 시 추가 드롭 확률
 * - (이동 속도는 StatManager에서 Attribute로 처리)
 * </p>
 */
public class StatEffectListener implements Listener {

    private final DreamWorkCore plugin;
    private final StatManager statManager;
    private final Random random = new Random();

    public StatEffectListener(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.statManager = plugin.getStatManager();
    }

    /**
     * 물리 데미지 증가 (STR)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        Player attacker = null;

        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null)
            return;

        PlayerStats stats = statManager.getStats(attacker);
        int str = stats.getStr();

        // 데미지 공식: 기본 + (STR * 0.5)
        double bonusDamage = str * 0.5;

        if (bonusDamage > 0) {
            event.setDamage(event.getDamage() + bonusDamage);

            if (plugin.isDebugMode()) {
                // 디버그 로깅
            }
        }
    }

    /**
     * 추가 드롭 (DEX)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerStats stats = statManager.getStats(player);
        int dex = stats.getDex();

        // DEX 1당 0.1% 확률로 추가 드롭 (최대 50%)
        double chance = dex * 0.1;
        if (chance > 50.0)
            chance = 50.0;

        if (random.nextDouble() * 100 < chance) {
            // 추가 드롭 발생
            Collection<ItemStack> drops = event.getBlock().getDrops(player.getInventory().getItemInMainHand());
            for (ItemStack item : drops) {
                if (item.getType() != Material.AIR) {
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item);
                }
            }

            // 효과음재생 등 가능
        }
    }
}
