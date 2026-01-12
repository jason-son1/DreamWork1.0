package com.dreamwork.core.skill.passive;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.*;

/**
 * 광부 패시브 스킬: 광맥 채굴 (VeinMiner)
 * <p>
 * Shift를 누른 상태로 광물을 캐면 인접한 같은 종류의
 * 광물을 연쇄적으로 채굴합니다.
 * </p>
 * <p>
 * 레벨에 따라 최대 채굴 가능 블록 수가 증가합니다.
 * - Lv.1-24: 3블록
 * - Lv.25-49: 6블록
 * - Lv.50-74: 12블록
 * - Lv.75-99: 24블록
 * - Lv.100: 48블록
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class MinerVeinSkill implements Listener {

    private final DreamWorkCore plugin;

    /**
     * 광맥 채굴 대상 블록 목록
     */
    private static final Set<Material> VEIN_BLOCKS = EnumSet.of(
            // 일반 광물
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            // 네더 광물
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS,
            // 원석 블록
            Material.RAW_IRON_BLOCK, Material.RAW_COPPER_BLOCK, Material.RAW_GOLD_BLOCK);

    /**
     * 중복 이벤트 방지용 (재귀 호출 방지)
     */
    private final Set<UUID> processing = new HashSet<>();

    public MinerVeinSkill(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 재귀 호출 방지
        if (processing.contains(uuid)) {
            return;
        }

        // Shift 키 확인
        if (!player.isSneaking()) {
            return;
        }

        Block block = event.getBlock();
        Material blockType = block.getType();

        // 광맥 채굴 대상 블록인지 확인
        if (!VEIN_BLOCKS.contains(blockType)) {
            return;
        }

        // 플레이어 광부 레벨 확인
        UserData userData = plugin.getStorageManager().getUserData(uuid);
        if (userData == null) {
            return;
        }

        int minerLevel = userData.getJobLevel(JobType.MINER);
        int maxBlocks = getMaxBlocks(minerLevel);

        // 최소 레벨 체크 (레벨 5 이상부터 활성화)
        if (minerLevel < 5) {
            return;
        }

        // 광맥 찾기 (BFS)
        List<Block> vein = findVein(block, blockType, maxBlocks);

        if (vein.size() <= 1) {
            return; // 인접 블록 없음
        }

        // 재귀 방지 플래그 설정
        processing.add(uuid);

        try {
            // 첫 번째 블록은 원래 이벤트에서 처리되므로 제외
            for (int i = 1; i < vein.size(); i++) {
                Block veinBlock = vein.get(i);

                // 블록 파괴 (드롭 포함)
                veinBlock.breakNaturally(player.getInventory().getItemInMainHand());

                // 광부 경험치 추가
                plugin.getJobManager().addExp(player, JobType.MINER,
                        plugin.getJobManager().getJob("miner").calculateExp("BLOCK_BREAK", blockType.name()));
            }

            // 효과 메시지
            player.sendMessage("§b[광부] §f광맥 채굴! §e+" + (vein.size() - 1) + "§f개 추가 채굴");

        } finally {
            processing.remove(uuid);
        }
    }

    /**
     * 레벨에 따른 최대 채굴 블록 수 반환
     */
    private int getMaxBlocks(int level) {
        if (level >= 100)
            return 48;
        if (level >= 75)
            return 24;
        if (level >= 50)
            return 12;
        if (level >= 25)
            return 6;
        return 3;
    }

    /**
     * BFS로 인접한 같은 종류의 블록을 찾습니다.
     */
    private List<Block> findVein(Block start, Material targetType, int maxBlocks) {
        List<Block> result = new ArrayList<>();
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        // 인접 방향 (6방향)
        int[][] directions = {
                { 1, 0, 0 }, { -1, 0, 0 },
                { 0, 1, 0 }, { 0, -1, 0 },
                { 0, 0, 1 }, { 0, 0, -1 }
        };

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block current = queue.poll();
            result.add(current);

            for (int[] dir : directions) {
                Block neighbor = current.getRelative(dir[0], dir[1], dir[2]);

                if (!visited.contains(neighbor) && neighbor.getType() == targetType) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }
}
