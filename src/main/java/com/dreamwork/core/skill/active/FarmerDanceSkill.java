package com.dreamwork.core.skill.active;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 농부 액티브 스킬: 풍요의 춤 (Dance of Abundance)
 * <p>
 * 활성화 시 주변 작물이 즉시 성장하고
 * 동물이 번식 상태가 됩니다.
 * </p>
 * <p>
 * 레벨별 효과:
 * - Lv.25+: 5블록 범위, 작물 1단계 성장
 * - Lv.50+: 8블록 범위, 작물 완전 성장
 * - Lv.75+: 12블록 범위, 동물 번식 추가
 * - Lv.100: 15블록 범위, 성장 속도 오라 60초
 * </p>
 * <p>
 * 쿨다운: 300초 (5분)
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class FarmerDanceSkill {

    private final DreamWorkCore plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> growthAura = new HashSet<>();

    private static final long COOLDOWN_MS = 300_000; // 5분

    private static final Set<Material> CROPS = Set.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.NETHER_WART,
            Material.MELON_STEM, Material.PUMPKIN_STEM);

    public FarmerDanceSkill(DreamWorkCore plugin) {
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
                player.sendMessage("§c[농부] 풍요의 춤 쿨다운: §e" + remaining + "초");
                return false;
            }
        }

        // 레벨 확인
        UserData userData = plugin.getStorageManager().getUserData(uuid);
        if (userData == null)
            return false;

        int farmerLevel = userData.getJobLevel(JobType.FARMER);
        if (farmerLevel < 25) {
            player.sendMessage("§c[농부] 풍요의 춤은 레벨 25 이상에서 사용 가능합니다.");
            return false;
        }

        // 스킬 활성화
        cooldowns.put(uuid, System.currentTimeMillis());

        int range = getRange(farmerLevel);
        boolean fullGrowth = farmerLevel >= 50;
        boolean breedAnimals = farmerLevel >= 75;
        boolean hasAura = farmerLevel >= 100;

        // 효과 메시지
        player.sendMessage("§a[농부] §l풍요의 춤! §r§a범위 " + range + "블록");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);

        // 파티클 웨이브
        Location center = player.getLocation();
        for (int r = 1; r <= range; r++) {
            final int radius = r;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle);
                    double x = center.getX() + radius * Math.cos(rad);
                    double z = center.getZ() + radius * Math.sin(rad);
                    center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                            x, center.getY() + 0.5, z, 2, 0.2, 0.2, 0.2, 0);
                }
            }, r * 2L);
        }

        // 작물 성장
        int grownCrops = growCrops(center, range, fullGrowth);

        // 동물 번식
        int bredAnimals = 0;
        if (breedAnimals) {
            bredAnimals = breedNearbyAnimals(player, range);
        }

        // 결과 메시지
        StringBuilder result = new StringBuilder("§a[농부] ");
        if (grownCrops > 0) {
            result.append("§e").append(grownCrops).append("§a개 작물 성장");
        }
        if (bredAnimals > 0) {
            if (grownCrops > 0)
                result.append(", ");
            result.append("§e").append(bredAnimals).append("§a마리 동물 번식");
        }
        if (grownCrops == 0 && bredAnimals == 0) {
            result.append("주변에 성장시킬 대상이 없습니다.");
        }
        player.sendMessage(result.toString());

        // Lv.100 성장 오라
        if (hasAura) {
            growthAura.add(uuid);
            player.sendMessage("§a[농부] §e60초§a간 주변 작물 성장 속도 증가!");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                growthAura.remove(uuid);
                player.sendMessage("§7[농부] 성장 오라가 종료되었습니다.");
            }, 60 * 20L);
        }

        return true;
    }

    /**
     * 주변 작물을 성장시킵니다.
     */
    private int growCrops(Location center, int range, boolean fullGrowth) {
        int count = 0;
        World world = center.getWorld();
        if (world == null)
            return 0;

        for (int x = -range; x <= range; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -range; z <= range; z++) {
                    Block block = world.getBlockAt(
                            center.getBlockX() + x,
                            center.getBlockY() + y,
                            center.getBlockZ() + z);

                    if (!CROPS.contains(block.getType()))
                        continue;

                    if (block.getBlockData() instanceof Ageable ageable) {
                        int currentAge = ageable.getAge();
                        int maxAge = ageable.getMaximumAge();

                        if (currentAge < maxAge) {
                            if (fullGrowth) {
                                ageable.setAge(maxAge);
                            } else {
                                ageable.setAge(Math.min(currentAge + 1, maxAge));
                            }
                            block.setBlockData(ageable);
                            count++;

                            // 파티클
                            world.spawnParticle(Particle.COMPOSTER,
                                    block.getLocation().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0);
                        }
                    }
                }
            }
        }

        return count;
    }

    /**
     * 주변 동물을 번식시킵니다.
     */
    private int breedNearbyAnimals(Player player, int range) {
        int count = 0;
        Collection<Entity> entities = player.getWorld().getNearbyEntities(
                player.getLocation(), range, range, range);

        for (Entity entity : entities) {
            if (entity instanceof Animals animal) {
                if (animal.canBreed() && !animal.isLoveMode()) {
                    animal.setLoveModeTicks(600); // 30초
                    count++;

                    // 파티클
                    animal.getWorld().spawnParticle(Particle.HEART,
                            animal.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0);
                }

                if (count >= 10)
                    break; // 최대 10마리
            }
        }

        return count;
    }

    public boolean hasGrowthAura(Player player) {
        return growthAura.contains(player.getUniqueId());
    }

    private int getRange(int level) {
        if (level >= 100)
            return 15;
        if (level >= 75)
            return 12;
        if (level >= 50)
            return 8;
        return 5;
    }
}
