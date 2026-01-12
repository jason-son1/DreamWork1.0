package com.dreamwork.core.listener;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.JobProvider;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.job.engine.RewardProcessor;
import com.dreamwork.core.job.engine.TriggerManager;
import com.dreamwork.core.job.engine.TriggerType;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 통합 직업 활동 이벤트 리스너 (다중 직업 엔진)
 * <p>
 * 플레이어의 행동을 감지하고, 해당 행동에 맞는 직업에
 * 경험치를 부여합니다.
 * </p>
 * <p>
 * 트리거 → 직업 매핑:
 * - BLOCK_BREAK → MINER (광부)
 * - HARVEST, BREED_ANIMAL → FARMER (농부)
 * - FISH_CATCH → FISHER (어부)
 * - ENTITY_KILL → HUNTER (사냥꾼)
 * - DISCOVER_CHUNK, TRAVEL_DISTANCE → EXPLORER (탐험가)
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class JobActivityListener implements Listener {

    private final DreamWorkCore plugin;
    private final JobManager jobManager;
    private final TriggerManager triggerManager;
    private final RewardProcessor rewardProcessor;

    /**
     * 탐험가 청크 추적 (새 청크 발견 감지용)
     */
    private final Map<UUID, Long> lastChunkCheck = new HashMap<>();
    private final Map<UUID, String> lastChunk = new HashMap<>();

    public JobActivityListener(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.triggerManager = jobManager.getTriggerManager();
        this.rewardProcessor = jobManager.getRewardProcessor();
    }

    /**
     * 트리거 타입에서 해당하는 직업 타입을 반환합니다.
     */
    private JobType getJobTypeForTrigger(TriggerType trigger) {
        return switch (trigger) {
            case BLOCK_BREAK -> JobType.MINER;
            case HARVEST, BREED_ANIMAL -> JobType.FARMER;
            case FISH_CATCH -> JobType.FISHER;
            case ENTITY_KILL, MOB_KILL -> JobType.HUNTER;
            case DISCOVER_CHUNK, DISCOVER_BIOME, DISCOVER_STRUCTURE, TRAVEL_DISTANCE, TRAVEL -> JobType.EXPLORER;
            case CRAFT_ITEM, EAT_FOOD, QUEST_COMPLETE -> null; // 공통 활동은 null 반환
        };
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // 스킬 효과: 보석 탐지 (Passive)
        if (blockType == Material.STONE || blockType == Material.DEEPSLATE) {
            if (plugin.getSkillManager().hasSkill(player, "gem_detector")) {
                if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.001) { // 0.1%
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
                            new ItemStack(Material.EMERALD));
                    player.sendMessage("§b[광부] 반짝이는 보석을 발견했습니다!");
                }
            }
        }

        handleJobEvent(event, player, TriggerType.BLOCK_BREAK, blockType.name());
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
            } else {
                target = caught.getType().name();
            }
        }

        handleJobEvent(event, event.getPlayer(), TriggerType.FISH_CATCH, target);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null)
            return;

        // 스킬 효과: 헤드헌터 (Passive)
        if (plugin.getSkillManager().hasSkill(killer, "head_hunter")) {
            if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.05) { // 5%
                Material headType = switch (event.getEntity().getType()) {
                    case ZOMBIE -> Material.ZOMBIE_HEAD;
                    case SKELETON -> Material.SKELETON_SKULL;
                    case CREEPER -> Material.CREEPER_HEAD;
                    case PIGLIN -> Material.PIGLIN_HEAD;
                    case ENDER_DRAGON -> Material.DRAGON_HEAD;
                    case WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL;
                    default -> null;
                };

                if (headType != null) {
                    event.getDrops().add(new ItemStack(headType));
                    killer.sendMessage("§c[사냥꾼] 적의 머리를 취했습니다!");
                }
            }
        }

        handleJobEvent(event, killer, TriggerType.ENTITY_KILL, event.getEntity().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        handleJobEvent(event, event.getPlayer(), TriggerType.HARVEST,
                event.getHarvestedBlock().getType().name());
    }

    /**
     * 탐험가 청크 발견 감지 (1초 단위 최적화)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 블록 변경이 없으면 무시
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 1초 단위 체크 (성능 최적화)
        long now = System.currentTimeMillis();
        Long lastCheck = lastChunkCheck.get(uuid);
        if (lastCheck != null && (now - lastCheck) < 1000) {
            return;
        }
        lastChunkCheck.put(uuid, now);

        // 청크 변경 감지
        String currentChunk = player.getLocation().getChunk().getX() + "," +
                player.getLocation().getChunk().getZ();
        String previous = lastChunk.get(uuid);

        if (!currentChunk.equals(previous)) {
            lastChunk.put(uuid, currentChunk);

            // 새 청크 발견 시 경험치 부여
            if (previous != null) { // 첫 번째 청크가 아닌 경우만
                handleJobEvent(event, player, TriggerType.DISCOVER_CHUNK, currentChunk);
            }
        }
    }

    /**
     * 직업 이벤트를 처리합니다. (다중 직업 시스템)
     */
    private void handleJobEvent(Event event, Player player, TriggerType trigger, String target) {
        // 1. 트리거에 해당하는 직업 타입 결정
        JobType jobType = getJobTypeForTrigger(trigger);
        if (jobType == null) {
            return; // 매핑되지 않은 트리거
        }

        // 2. 직업 설정 가져오기
        JobProvider job = jobManager.getJob(jobType.getConfigKey());
        if (job == null) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("[Debug] 직업 설정 없음: " + jobType.getConfigKey());
            }
            return;
        }

        // 3. 경험치 계산
        double exp = job.calculateExp(trigger.name(), target);

        // 4. 보상 처리 (경험치 > 0 인 경우)
        if (exp > 0) {
            // 다중 직업 시스템: 해당 직업에 경험치 추가
            jobManager.addExp(player, jobType, exp);

            // 돈 보상 처리
            double money = job.calculateMoney(trigger.name(), target);
            if (money > 0) {
                rewardProcessor.grantMoney(player, money);
            }

            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[Debug] " + player.getName() + " [" +
                        jobType.getDisplayName() + "] " + trigger + " " + target +
                        " -> " + String.format("%.1f", exp) + " EXP");
            }
        }
    }
}
