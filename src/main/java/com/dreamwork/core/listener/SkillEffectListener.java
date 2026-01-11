package com.dreamwork.core.listener;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.skill.skills.GoldenHook;
import com.dreamwork.core.skill.skills.SuperHeat;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * 스킬 효과 리스너
 * Active/Passive 스킬의 실제 효과를 처리합니다.
 */
public class SkillEffectListener implements Listener {

    private final DreamWorkCore plugin;
    private final JobManager jobManager;
    private final Random random = new Random();

    public SkillEffectListener(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @EventHandler
    public void onMinerBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        Player player = event.getPlayer();
        UserJobData userData = jobManager.getUserJob(player.getUniqueId());

        if (!"miner".equals(userData.getJobId()))
            return;
        int level = userData.getLevel();

        // [Active] SuperHeat (30레벨)
        if (SuperHeat.isActive(player.getUniqueId())) {
            smeltDrops(event);
        }

        // [Passive] GemDetector (80레벨)
        if (level >= 80) {
            if (event.getBlock().getType().name().endsWith("STONE") && random.nextDouble() < 0.001) {
                dropRandomGem(event);
            }
        }
    }

    private void smeltDrops(BlockBreakEvent event) {
        if (event.isDropItems()) {
            Material type = event.getBlock().getType();
            Material result = switch (type) {
                case IRON_ORE, DEEPSLATE_IRON_ORE, RAW_IRON_BLOCK -> Material.IRON_INGOT;
                case GOLD_ORE, DEEPSLATE_GOLD_ORE, RAW_GOLD_BLOCK -> Material.GOLD_INGOT;
                case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
                default -> null;
            };

            if (result != null) {
                event.setDropItems(false);
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(result));
            }
        }
    }

    private void dropRandomGem(BlockBreakEvent event) {
        Material[] gems = { Material.DIAMOND, Material.EMERALD, Material.AMETHYST_SHARD };
        Material gem = gems[random.nextInt(gems.length)];
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(gem));
        event.getPlayer().sendMessage("§e[광부] 반짝이는 보석을 발견했습니다!");
    }

    @EventHandler
    public void onFisherFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH)
            return;
        Player player = event.getPlayer();
        UserJobData userData = jobManager.getUserJob(player.getUniqueId());

        if (!"fisher".equals(userData.getJobId()))
            return;
        int level = userData.getLevel();

        // [Passive] DeepTreasure (80레벨)
        if (level >= 80 && random.nextDouble() < 0.01) {
            ItemStack coin = new ItemStack(Material.GOLD_NUGGET);
            var meta = coin.getItemMeta();
            meta.setDisplayName("§6고대 주화");
            coin.setItemMeta(meta);
            event.getCaught().getWorld().dropItemNaturally(event.getCaught().getLocation(), coin);
            player.sendMessage("§b[낚시꾼] 심해에서 고대 주화를 낚았습니다!");
        }
    }

    @EventHandler
    public void onRodDurability(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        if (event.getItem().getType() != Material.FISHING_ROD)
            return;

        UserJobData userData = jobManager.getUserJob(player.getUniqueId());
        if (!"fisher".equals(userData.getJobId()))
            return;

        // [Passive] SeaLuck (50레벨)
        if (userData.getLevel() >= 50 && random.nextDouble() < 0.1) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunterKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null)
            return;

        UserJobData userData = jobManager.getUserJob(killer.getUniqueId());
        if (!"hunter".equals(userData.getJobId()))
            return;

        // [Passive] HeadHunter (50레벨)
        if (userData.getLevel() >= 50 && random.nextDouble() < 0.05) {
            Material head = getHead(event.getEntityType());
            if (head != null) {
                event.getDrops().add(new ItemStack(head));
                killer.sendMessage("§c[사냥꾼] 적의 머리를 취했습니다!");
            }
        }
    }

    private Material getHead(org.bukkit.entity.EntityType type) {
        return switch (type) {
            case ZOMBIE -> Material.ZOMBIE_HEAD;
            case SKELETON -> Material.SKELETON_SKULL;
            case CREEPER -> Material.CREEPER_HEAD;
            case PIGLIN -> Material.PIGLIN_HEAD;
            case ENDER_DRAGON -> Material.DRAGON_HEAD;
            default -> null;
        };
    }

    @EventHandler
    public void onHunterDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player))
            return;
        if (!(event.getEntity() instanceof LivingEntity victim))
            return;

        UserJobData userData = jobManager.getUserJob(player.getUniqueId());
        if (!"hunter".equals(userData.getJobId()))
            return;

        // [Passive] Massacre (80레벨)
        if (userData.getLevel() >= 80) {
            double healthPercent = victim.getHealth()
                    / victim.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            if (healthPercent <= 0.3) {
                event.setDamage(event.getDamage() * 1.2);
            }
        }
    }
}
