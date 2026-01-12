package com.dreamwork.core.item.custom;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.job.JobType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * 풍요의 낫 (Scythe of Harvest)
 * 
 * <p>
 * 우클릭 시 BFS 알고리즘으로 연결된 작물을 한 번에 수확합니다.
 * 자동 재파종 기능이 포함됩니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class ScytheItem implements Listener {

    private final DreamWorkCore plugin;

    public ScytheItem(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onScytheUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !isCrop(clickedBlock.getType()))
            return;

        ItemStack item = event.getItem();
        if (!isScythe(item))
            return;

        Player player = event.getPlayer();

        // BFS 수확 실행
        int harvested = harvestArea(player, clickedBlock, 15); // 최대 15개 제한

        if (harvested > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0f, 1.2f);

            // 내구도 감소
            if (item.getItemMeta() instanceof Damageable dmg) {
                // 내구도 처리: 1회당 1 소모 (여러 개 캐도 1 소모로 유저 친화적)
                dmg.setDamage(dmg.getDamage() + 1);
                item.setItemMeta(dmg);

                // 파괴 체크
                if (dmg.getDamage() >= item.getType().getMaxDurability()) {
                    player.getInventory().setItemInMainHand(null);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
                }
            }
        }
    }

    private int harvestArea(Player player, Block startNode, int limit) {
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();

        queue.add(startNode);
        visited.add(startNode);

        int count = 0;

        while (!queue.isEmpty() && count < limit) {
            Block current = queue.poll();

            if (isFullyGrownCrop(current)) {
                // 수확 로직 (BlockBreakEvent 호출하여 다른 플러그인/시스템 연동)
                // 직접 breakNatually 호출하면 이벤트가 발생 안 할 수도 있으므로 breakBlock 사용
                // breakBlock은 권한 체크 및 플러그인 이벤트를 발생시킴
                if (player.breakBlock(current)) {
                    count++;
                }
            }

            // 인접 3x3x1 탐색 (평면)
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0)
                        continue;

                    Block neighbor = current.getRelative(x, 0, z);
                    if (!visited.contains(neighbor) && isCrop(neighbor.getType())) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        return count;
    }

    /**
     * 아이템이 풍요의 낫인지 확인합니다.
     * items.yml에 정의된 ID "scythe_harvest"를 체크하거나, 이름 등을 체크.
     */
    private boolean isScythe(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;

        String itemId = plugin.getItemFactory().getItemId(item);
        if ("scythe_harvest".equals(itemId))
            return true;

        // 이름으로 체크 (Legacy support)
        return item.getItemMeta().getDisplayName().contains("풍요의 낫");
    }

    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS -> true;
            default -> false;
        };
    }

    private boolean isFullyGrownCrop(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    /**
     * 풍요의 낫 아이템 생성 (테스트용/지급용)
     */
    public ItemStack createScythe() {
        return ItemBuilder.of(Material.IRON_HOE)
                .name("§a풍요의 낫")
                .lore("§7우클릭으로 주변의 다 자란 작물을")
                .lore("§7한 번에 수확합니다.")
                .lore("")
                .lore("§e[농부 전용]")
                .customModelData(20010)
                .build(plugin.getItemFactory());
    }
}
