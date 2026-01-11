package com.dreamwork.core.listener;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.JobProvider;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.job.engine.JobValidator;
import com.dreamwork.core.job.engine.RewardProcessor;
import com.dreamwork.core.job.engine.TriggerManager;
import com.dreamwork.core.job.engine.TriggerType;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 통합 직업 활동 이벤트 리스너 (엔진 기반)
 * 
 * <p>
 * TriggerManager, JobValidator, RewardProcessor를 사용하여
 * 직업 활동을 감지하고 처리합니다.
 * </p>
 */
public class JobActivityListener implements Listener {

    private final DreamWorkCore plugin;
    private final JobManager jobManager;
    private final TriggerManager triggerManager;
    private final JobValidator jobValidator;
    private final RewardProcessor rewardProcessor;

    public JobActivityListener(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.triggerManager = jobManager.getTriggerManager();
        this.jobValidator = jobManager.getJobValidator();
        this.rewardProcessor = jobManager.getRewardProcessor();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        // [광부] 보석 탐지 (Passive)
        if (event.getBlock().getType() == Material.STONE || event.getBlock().getType() == Material.DEEPSLATE) {
            if (plugin.getSkillManager().hasSkill(player, "gem_detector")) {
                if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.001) { // 0.1%
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
                            new ItemStack(Material.EMERALD));
                    player.sendMessage("§b[광부] 반짝이는 보석을 발견했습니다!");
                }
            }
        }

        handleJobEvent(event, player, event.getBlock().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH)
            return;

        Entity caught = event.getCaught();
        String target = "FISH";
        if (caught != null) {
            if (caught instanceof org.bukkit.entity.Item itemEntity) {
                target = itemEntity.getItemStack().getType().name();

                // [어부] 인내심/황금낚싯바늘 등은 여기서 처리하거나 낚시 메커니즘 변경 필요
                // 여기서는 보상 처리만 담당
            } else {
                target = caught.getType().name();
            }
        }

        handleJobEvent(event, event.getPlayer(), target);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null)
            return;

        // [사냥꾼] 헤드헌터 (Passive)
        if (plugin.getSkillManager().hasSkill(killer, "head_hunter")) {
            if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.05) { // 5%
                Material headType = Material.CREEPER_HEAD; // 기본값
                switch (event.getEntity().getType()) {
                    case ZOMBIE -> headType = Material.ZOMBIE_HEAD;
                    case SKELETON -> headType = Material.SKELETON_SKULL;
                    case CREEPER -> headType = Material.CREEPER_HEAD;
                    case PIGLIN -> headType = Material.PIGLIN_HEAD;
                    case ENDER_DRAGON -> headType = Material.DRAGON_HEAD;
                    case WITHER_SKELETON -> headType = Material.WITHER_SKELETON_SKULL;
                    default -> headType = null;
                }

                if (headType != null) {
                    event.getDrops().add(new ItemStack(headType));
                    killer.sendMessage("§c[사냥꾼] 적의 머리를 취했습니다!");
                }
            }
        }

        handleJobEvent(event, killer, event.getEntity().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        handleJobEvent(event, event.getPlayer(), event.getHarvestedBlock().getType().name());
    }

    /**
     * 직업 이벤트를 공통 로직으로 처리합니다.
     */
    private void handleJobEvent(Event event, Player player, String target) {
        // 1. 트리거 식별
        TriggerType trigger = triggerManager.getTriggerFromEvent(event);
        if (trigger == null)
            return;

        // 2. 플레이어 직업 데이터 확인
        UserJobData userData = jobManager.getUserJob(player.getUniqueId());
        if (!userData.hasJob())
            return;

        JobProvider job = jobManager.getJob(userData.getJobId());
        if (job == null)
            return;

        // 3. 유효성 검사
        if (!jobValidator.isValidAction(player, trigger, target, job))
            return;

        // 4. 경험치 계산
        double exp = job.calculateExp(trigger.name(), target);

        // 5. 보상 처리 (경험치 > 0 인 경우)
        if (exp > 0) {
            rewardProcessor.process(job, player, trigger, exp);

            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] Job Action: " + player.getName() +
                        " [" + trigger + "] " + target + " -> " + exp + " EXP");
            }
        }
    }
}
