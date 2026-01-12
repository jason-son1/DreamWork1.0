package com.dreamwork.core.skill.active;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 광부 액티브 스킬: 슈퍼 히트 (Super Heat)
 * <p>
 * 활성화 시 일정 시간 동안 채굴 속도가 증가하고
 * 광물이 자동으로 제련됩니다.
 * </p>
 * <p>
 * 레벨별 효과:
 * - Lv.25+: 10초, 채굴 속도 +30%
 * - Lv.50+: 15초, 채굴 속도 +50%, 자동 제련
 * - Lv.75+: 20초, 채굴 속도 +75%, 자동 제련
 * - Lv.100: 30초, 채굴 속도 +100%, 자동 제련, 행운 +1
 * </p>
 * <p>
 * 쿨다운: 180초
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class MinerSuperHeatSkill implements Listener {

    private final DreamWorkCore plugin;
    private final Set<UUID> activeSkill = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final long COOLDOWN_MS = 180_000; // 3분

    /**
     * 자동 제련 매핑
     */
    private static final Map<Material, Material> SMELT_MAP = Map.ofEntries(
            Map.entry(Material.IRON_ORE, Material.IRON_INGOT),
            Map.entry(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT),
            Map.entry(Material.GOLD_ORE, Material.GOLD_INGOT),
            Map.entry(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT),
            Map.entry(Material.COPPER_ORE, Material.COPPER_INGOT),
            Map.entry(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT),
            Map.entry(Material.RAW_IRON_BLOCK, Material.IRON_BLOCK),
            Map.entry(Material.RAW_GOLD_BLOCK, Material.GOLD_BLOCK),
            Map.entry(Material.RAW_COPPER_BLOCK, Material.COPPER_BLOCK),
            Map.entry(Material.COBBLESTONE, Material.STONE),
            Map.entry(Material.SAND, Material.GLASS));

    public MinerSuperHeatSkill(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 스킬을 활성화합니다.
     */
    public boolean activate(Player player) {
        UUID uuid = player.getUniqueId();

        // 쿨다운 확인
        if (cooldowns.containsKey(uuid)) {
            long remaining = (cooldowns.get(uuid) + COOLDOWN_MS - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                player.sendMessage("§c[광부] 슈퍼 히트 쿨다운: §e" + remaining + "초");
                return false;
            }
        }

        // 레벨 확인
        UserData userData = plugin.getStorageManager().getUserData(uuid);
        if (userData == null)
            return false;

        int minerLevel = userData.getJobLevel(JobType.MINER);
        if (minerLevel < 25) {
            player.sendMessage("§c[광부] 슈퍼 히트는 레벨 25 이상에서 사용 가능합니다.");
            return false;
        }

        // 스킬 활성화
        activeSkill.add(uuid);
        cooldowns.put(uuid, System.currentTimeMillis());

        int duration = getDuration(minerLevel);

        // 채굴 속도 버프 (Haste 효과)
        int hasteLevel = getHasteLevel(minerLevel);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.HASTE,
                duration * 20,
                hasteLevel,
                false, false, true));

        // 효과 메시지
        player.sendMessage("§6[광부] §l슈퍼 히트 §r§6활성화! §e" + duration + "초");
        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);

        // 파티클 효과
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!activeSkill.contains(uuid) || !player.isOnline()) {
                task.cancel();
                return;
            }
            player.getWorld().spawnParticle(Particle.FLAME,
                    player.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.02);
        }, 0L, 10L);

        // 자동 비활성화
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeSkill.remove(uuid)) {
                player.sendMessage("§7[광부] 슈퍼 히트가 종료되었습니다.");
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
            }
        }, duration * 20L);

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!activeSkill.contains(uuid))
            return;

        Block block = event.getBlock();
        Material blockType = block.getType();

        // 자동 제련
        if (SMELT_MAP.containsKey(blockType)) {
            UserData userData = plugin.getStorageManager().getUserData(uuid);
            if (userData != null && userData.getJobLevel(JobType.MINER) >= 50) {
                event.setDropItems(false);

                Material smelted = SMELT_MAP.get(blockType);
                int amount = 1;

                // Lv.100 행운 보너스
                if (userData.getJobLevel(JobType.MINER) >= 100) {
                    if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < 0.25) {
                        amount = 2;
                    }
                }

                block.getWorld().dropItemNaturally(block.getLocation(),
                        new ItemStack(smelted, amount));

                // 제련 파티클
                block.getWorld().spawnParticle(Particle.LAVA,
                        block.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0);
            }
        }
    }

    private int getDuration(int level) {
        if (level >= 100)
            return 30;
        if (level >= 75)
            return 20;
        if (level >= 50)
            return 15;
        return 10;
    }

    private int getHasteLevel(int level) {
        if (level >= 100)
            return 2; // Haste III
        if (level >= 75)
            return 1; // Haste II
        if (level >= 50)
            return 1; // Haste II
        return 0; // Haste I
    }

    public boolean isActive(Player player) {
        return activeSkill.contains(player.getUniqueId());
    }
}
