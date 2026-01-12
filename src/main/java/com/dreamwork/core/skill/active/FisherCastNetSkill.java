package com.dreamwork.core.skill.active;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 어부 액티브 스킬: 투망 (Cast Net)
 * <p>
 * 물 위에서 사용 시 주변 물고기를 한번에 잡습니다.
 * </p>
 * <p>
 * 레벨별 효과:
 * - Lv.25+: 3마리, 일반 물고기만
 * - Lv.50+: 5마리, 희귀 확률 +10%
 * - Lv.75+: 8마리, 희귀 확률 +25%
 * - Lv.100: 12마리, 보물 확률 +5%
 * </p>
 * <p>
 * 쿨다운: 180초 (3분)
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class FisherCastNetSkill {

    private final DreamWorkCore plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final long COOLDOWN_MS = 180_000; // 3분

    private static final List<Material> COMMON_FISH = List.of(
            Material.COD, Material.SALMON);

    private static final List<Material> RARE_FISH = List.of(
            Material.TROPICAL_FISH, Material.PUFFERFISH);

    private static final List<Material> TREASURE = List.of(
            Material.NAUTILUS_SHELL, Material.HEART_OF_THE_SEA,
            Material.ENCHANTED_BOOK, Material.NAME_TAG);

    public FisherCastNetSkill(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 스킬을 활성화합니다.
     */
    public boolean activate(Player player) {
        UUID uuid = player.getUniqueId();

        // 물 위 확인
        if (!isNearWater(player)) {
            player.sendMessage("§c[어부] 투망은 물 근처에서만 사용할 수 있습니다.");
            return false;
        }

        // 쿨다운 확인
        if (cooldowns.containsKey(uuid)) {
            long remaining = (cooldowns.get(uuid) + COOLDOWN_MS - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                player.sendMessage("§c[어부] 투망 쿨다운: §e" + remaining + "초");
                return false;
            }
        }

        // 레벨 확인
        UserData userData = plugin.getStorageManager().getUserData(uuid);
        if (userData == null)
            return false;

        int fisherLevel = userData.getJobLevel(JobType.FISHER);
        if (fisherLevel < 25) {
            player.sendMessage("§c[어부] 투망은 레벨 25 이상에서 사용 가능합니다.");
            return false;
        }

        // 스킬 활성화
        cooldowns.put(uuid, System.currentTimeMillis());

        int fishCount = getFishCount(fisherLevel);
        double rareChance = getRareChance(fisherLevel);
        double treasureChance = getTreasureChance(fisherLevel);

        // 효과 메시지
        player.sendMessage("§b[어부] §l투망! §r§b물고기를 잡는 중...");
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 1.0f);

        // 투망 파티클
        Location center = player.getLocation();
        for (int angle = 0; angle < 360; angle += 20) {
            double rad = Math.toRadians(angle);
            double x = center.getX() + 3 * Math.cos(rad);
            double z = center.getZ() + 3 * Math.sin(rad);
            center.getWorld().spawnParticle(Particle.SPLASH,
                    x, center.getY(), z, 10, 0.5, 0.1, 0.5, 0);
        }

        // 물고기 생성 (지연)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<ItemStack> catches = new ArrayList<>();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (int i = 0; i < fishCount; i++) {
                Material fishType;

                // 보물 확률
                if (random.nextDouble() < treasureChance) {
                    fishType = TREASURE.get(random.nextInt(TREASURE.size()));
                }
                // 희귀 물고기 확률
                else if (random.nextDouble() < rareChance) {
                    fishType = RARE_FISH.get(random.nextInt(RARE_FISH.size()));
                }
                // 일반 물고기
                else {
                    fishType = COMMON_FISH.get(random.nextInt(COMMON_FISH.size()));
                }

                catches.add(new ItemStack(fishType));
            }

            // 아이템 드롭
            for (ItemStack item : catches) {
                Item droppedItem = player.getWorld().dropItemNaturally(
                        player.getLocation().add(random.nextDouble(-2, 2), 0.5, random.nextDouble(-2, 2)),
                        item);
                droppedItem.setPickupDelay(0);
            }

            // 경험치 추가
            double expPerFish = 15.0;
            plugin.getJobManager().addExp(player, JobType.FISHER, fishCount * expPerFish);

            // 결과 메시지
            player.sendMessage("§b[어부] §e" + fishCount + "§b마리를 잡았습니다!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

        }, 20L);

        return true;
    }

    /**
     * 플레이어가 물 근처에 있는지 확인합니다.
     */
    private boolean isNearWater(Player player) {
        Location loc = player.getLocation();
        int range = 3;

        for (int x = -range; x <= range; x++) {
            for (int y = -2; y <= 0; y++) {
                for (int z = -range; z <= range; z++) {
                    Material type = loc.getWorld().getBlockAt(
                            loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z).getType();
                    if (type == Material.WATER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int getFishCount(int level) {
        if (level >= 100)
            return 12;
        if (level >= 75)
            return 8;
        if (level >= 50)
            return 5;
        return 3;
    }

    private double getRareChance(int level) {
        if (level >= 75)
            return 0.25;
        if (level >= 50)
            return 0.10;
        return 0;
    }

    private double getTreasureChance(int level) {
        if (level >= 100)
            return 0.05;
        return 0;
    }
}
