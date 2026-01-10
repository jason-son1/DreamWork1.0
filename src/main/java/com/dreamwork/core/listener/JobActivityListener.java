package com.dreamwork.core.listener;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.JobProvider;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 통합 직업 활동 이벤트 리스너
 * 
 * <p>
 * 모든 직업 관련 행동(블럭 파괴, 낚시, 사냥 등)을 감지하고
 * JobManager로 경험치 획득을 전달합니다.
 * </p>
 * 
 * <h2>지원하는 이벤트:</h2>
 * <ul>
 * <li>BlockBreakEvent - 블럭 파괴 (광부)</li>
 * <li>PlayerFishEvent - 낚시 (낚시꾼)</li>
 * <li>EntityDeathEvent - 몬스터 처치 (사냥꾼)</li>
 * <li>PlayerHarvestBlockEvent - 작물 수확 (농부)</li>
 * </ul>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class JobActivityListener implements Listener {

    private final DreamWorkCore plugin;
    private final JobManager jobManager;

    // 액션 타입 상수
    public static final String ACTION_BLOCK_BREAK = "BLOCK_BREAK";
    public static final String ACTION_FISH_CATCH = "FISH_CATCH";
    public static final String ACTION_MOB_KILL = "MOB_KILL";
    public static final String ACTION_HARVEST = "HARVEST";

    /**
     * JobActivityListener 생성자
     * 
     * @param plugin 플러그인 인스턴스
     */
    public JobActivityListener(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    /**
     * 블럭 파괴 이벤트 처리 (광부)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        processJobAction(player, ACTION_BLOCK_BREAK, blockType.name());
    }

    /**
     * 낚시 이벤트 처리 (낚시꾼)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return; // 물고기를 잡았을 때만 처리
        }

        Player player = event.getPlayer();
        Entity caught = event.getCaught();

        if (caught == null) {
            return;
        }

        // 잡은 아이템 확인
        String target = "FISH"; // 기본값

        if (caught instanceof org.bukkit.entity.Item itemEntity) {
            ItemStack item = itemEntity.getItemStack();
            target = item.getType().name();
        } else {
            target = caught.getType().name();
        }

        processJobAction(player, ACTION_FISH_CATCH, target);
    }

    /**
     * 엔티티 사망 이벤트 처리 (사냥꾼)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) {
            return; // 플레이어가 직접 처치한 경우만
        }

        String mobType = entity.getType().name();

        processJobAction(killer, ACTION_MOB_KILL, mobType);
    }

    /**
     * 작물 수확 이벤트 처리 (농부)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getHarvestedBlock().getType();

        processJobAction(player, ACTION_HARVEST, blockType.name());
    }

    /**
     * 직업 행동을 처리합니다.
     * 
     * <p>
     * 플레이어의 현재 직업을 확인하고, 해당 행동에 대한 경험치를 계산하여
     * JobManager에 전달합니다.
     * </p>
     * 
     * @param player 플레이어
     * @param action 행동 유형
     * @param target 대상 (블럭 종류, 몹 종류 등)
     */
    private void processJobAction(Player player, String action, String target) {
        // 플레이어의 현재 직업 조회
        UserJobData userData = jobManager.getUserJob(player.getUniqueId());

        if (!userData.hasJob()) {
            return; // 직업이 없으면 무시
        }

        // 직업 제공자 조회
        JobProvider job = jobManager.getJob(userData.getJobId());
        if (job == null) {
            return;
        }

        // 경험치 계산
        double exp = job.calculateExp(action, target);

        if (exp > 0) {
            // 경험치 추가
            jobManager.addExp(player, exp);

            // 디버그 로그
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] " + player.getName() +
                        " - Action: " + action + ", Target: " + target + ", Exp: " + exp);
            }
        }
    }
}
