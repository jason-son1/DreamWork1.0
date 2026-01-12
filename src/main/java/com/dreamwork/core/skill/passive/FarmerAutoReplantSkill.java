package com.dreamwork.core.skill.passive;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 농부 패시브 스킬: 자동 재파종 (Green Thumb)
 * <p>
 * 다 자란 작물을 수확하면 자동으로 다시 심어줍니다.
 * 레벨에 따라 추가 효과가 있습니다.
 * </p>
 * <p>
 * 레벨별 효과:
 * - Lv.1+: 자동 재파종
 * - Lv.25+: 2단계 성장 상태로 심기
 * - Lv.50+: 3단계 성장 상태로 심기
 * - Lv.75+: 일정 확률로 씨앗 소모 없음
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class FarmerAutoReplantSkill implements Listener {

    private final DreamWorkCore plugin;

    /**
     * 작물과 해당 씨앗 매핑
     */
    private static final Map<Material, Material> CROP_SEEDS = Map.of(
            Material.WHEAT, Material.WHEAT_SEEDS,
            Material.CARROTS, Material.CARROT,
            Material.POTATOES, Material.POTATO,
            Material.BEETROOTS, Material.BEETROOT_SEEDS,
            Material.NETHER_WART, Material.NETHER_WART);

    public FarmerAutoReplantSkill(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        // 작물인지 확인
        if (!CROP_SEEDS.containsKey(blockType)) {
            return;
        }

        // 다 자란 상태인지 확인
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return;
        }

        if (ageable.getAge() < ageable.getMaximumAge()) {
            return; // 아직 다 자라지 않음
        }

        // 플레이어 농부 레벨 확인
        UserData userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null) {
            return;
        }

        int farmerLevel = userData.getJobLevel(JobType.FARMER);

        // 최소 레벨 체크 (레벨 1 이상부터 활성화)
        if (farmerLevel < 1) {
            return;
        }

        Material seedType = CROP_SEEDS.get(blockType);

        // 씨앗 소모 여부 (Lv.75+ 시 25% 확률로 무료)
        boolean freeSeed = farmerLevel >= 75 &&
                java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.25;

        // 씨앗 확인 및 소모
        if (!freeSeed && !consumeSeed(player, seedType)) {
            return; // 씨앗 없음
        }

        // 다음 틱에 재파종 (블록 파괴 후)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // 블록이 공기인지 확인 (다른 플러그인이 이미 처리했을 수 있음)
            if (block.getType() != Material.AIR) {
                // 씨앗 반환 (이미 소모한 경우)
                if (!freeSeed) {
                    player.getInventory().addItem(new ItemStack(seedType, 1));
                }
                return;
            }

            // 농경지 아래에 있는지 확인
            Block below = block.getRelative(BlockFace.DOWN);
            if (below.getType() != Material.FARMLAND && blockType != Material.NETHER_WART) {
                if (!freeSeed) {
                    player.getInventory().addItem(new ItemStack(seedType, 1));
                }
                return;
            }

            // 네더 와트는 소울 샌드 위에만
            if (blockType == Material.NETHER_WART && below.getType() != Material.SOUL_SAND) {
                if (!freeSeed) {
                    player.getInventory().addItem(new ItemStack(seedType, 1));
                }
                return;
            }

            // 작물 심기
            block.setType(blockType);

            // 레벨에 따른 초기 성장 단계
            if (block.getBlockData() instanceof Ageable newAgeable) {
                int startAge = 0;
                if (farmerLevel >= 50) {
                    startAge = Math.min(3, newAgeable.getMaximumAge());
                } else if (farmerLevel >= 25) {
                    startAge = Math.min(2, newAgeable.getMaximumAge());
                }

                if (startAge > 0) {
                    newAgeable.setAge(startAge);
                    block.setBlockData(newAgeable);
                }
            }

            // 효과 메시지 (레벨 25 이상 또는 무료 씨앗일 때만)
            if (freeSeed) {
                player.sendMessage("§a[농부] §f자동 재파종! §7(씨앗 절약)");
            }

        }, 1L);
    }

    /**
     * 플레이어 인벤토리에서 씨앗을 소모합니다.
     */
    private boolean consumeSeed(Player player, Material seedType) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == seedType) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }
}
