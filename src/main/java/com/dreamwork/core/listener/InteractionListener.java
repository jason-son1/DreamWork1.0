package com.dreamwork.core.listener;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.stat.StatManager.PlayerStats;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Random;

/**
 * 채집/상호작용 리스너
 * 
 * <p>
 * DEX, LUCK 스탯을 기반으로 드롭 보너스를 제공합니다.
 * </p>
 * 
 * <h2>공식:</h2>
 * <ul>
 * <li>더블 드롭 확률: {@code DEX * dexMultiplier}</li>
 * <li>희귀 드롭 확률: {@code LUCK * luckMultiplier}</li>
 * </ul>
 */
public class InteractionListener implements Listener {

    private final DreamWorkCore plugin;
    private final StatManager statManager;
    private final Random random = new Random();

    // 설정 값
    private double dexMultiplier;
    private double luckMultiplier;

    public InteractionListener(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.statManager = plugin.getStatManager();
        loadConfig();
    }

    /**
     * 설정을 로드합니다.
     */
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.dexMultiplier = config.getDouble("interaction.dex-multiplier", 0.002);
        this.luckMultiplier = config.getDouble("interaction.luck-multiplier", 0.001);
    }

    /**
     * 블록 파괴 시 더블 드롭 처리
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerStats stats = statManager.getStats(player);

        // DEX 기반 더블 드롭 확률
        double doubleDropChance = stats.getDex() * dexMultiplier;

        if (random.nextDouble() < doubleDropChance) {
            // 더블 드롭 발생!
            Collection<ItemStack> drops = event.getBlock().getDrops(player.getInventory().getItemInMainHand());
            for (ItemStack drop : drops) {
                if (drop.getType() != Material.AIR) {
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop.clone());
                }
            }

            if (plugin.isDebugMode()) {
                plugin.getLogger()
                        .info("[Interaction] " + player.getName() + " 더블 드롭 발생! (DEX: " + stats.getDex() + ")");
            }
        }

        // LUCK 기반 희귀 아이템 드롭 (보너스 경험치 또는 특수 아이템)
        double rareDropChance = stats.getLuck() * luckMultiplier;
        if (random.nextDouble() < rareDropChance) {
            // 희귀 드롭 보너스 (예: 추가 경험치 오브)
            event.setExpToDrop((int) (event.getExpToDrop() * 1.5) + 1);

            if (plugin.isDebugMode()) {
                plugin.getLogger()
                        .info("[Interaction] " + player.getName() + " 희귀 보너스 발생! (LUCK: " + stats.getLuck() + ")");
            }
        }
    }

    /**
     * 낚시 시 보너스 처리
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH)
            return;
        if (!(event.getCaught() instanceof Item caughtItem))
            return;

        Player player = event.getPlayer();
        PlayerStats stats = statManager.getStats(player);

        // LUCK 기반 희귀 물고기 보너스
        double rareChance = stats.getLuck() * luckMultiplier * 2; // 낚시는 2배

        if (random.nextDouble() < rareChance) {
            // 보너스: 잡은 아이템 수량 증가
            ItemStack caught = caughtItem.getItemStack();
            caught.setAmount(caught.getAmount() + 1);
            caughtItem.setItemStack(caught);

            if (plugin.isDebugMode()) {
                plugin.getLogger()
                        .info("[Interaction] " + player.getName() + " 낚시 보너스 획득! (LUCK: " + stats.getLuck() + ")");
            }
        }
    }
}
