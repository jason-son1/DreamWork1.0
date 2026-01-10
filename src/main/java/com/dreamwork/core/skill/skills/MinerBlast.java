package com.dreamwork.core.skill.skills;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.skill.SkillEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 광부 전용 스킬: 광역 채굴
 * 
 * <p>
 * 주변 3×3×3 범위의 광석을 한 번에 채굴합니다.
 * </p>
 */
public class MinerBlast implements SkillEffect {

    private final DreamWorkCore plugin;

    private static final Set<Material> MINEABLE = Set.of(
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE,
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS, Material.NETHERRACK,
            Material.DIORITE, Material.ANDESITE, Material.GRANITE,
            Material.TUFF, Material.CALCITE);

    public MinerBlast(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || targetBlock.getType().isAir()) {
            player.sendMessage("§c[스킬] 바라보는 블록이 없습니다.");
            return;
        }

        Location center = targetBlock.getLocation();
        List<Block> blocksToBreak = new ArrayList<>();

        // 3×3×3 범위 스캔
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (MINEABLE.contains(block.getType())) {
                        blocksToBreak.add(block);
                    }
                }
            }
        }

        if (blocksToBreak.isEmpty()) {
            player.sendMessage("§c[스킬] 채굴 가능한 블록이 없습니다.");
            return;
        }

        // 이펙트
        player.getWorld().spawnParticle(Particle.EXPLOSION, center, 3, 1, 1, 1);
        player.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);

        // 블록 파괴 및 드롭
        ItemStack tool = player.getInventory().getItemInMainHand();
        int count = 0;

        for (Block block : blocksToBreak) {
            // 드롭 아이템 생성
            for (ItemStack drop : block.getDrops(tool)) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
            block.setType(Material.AIR);
            count++;
        }

        player.sendMessage("§a[스킬] §f" + count + "§a개 블록을 채굴했습니다!");
    }

    @Override
    public String getId() {
        return "miner_blast";
    }

    @Override
    public String getName() {
        return "광부의 폭발";
    }

    @Override
    public String getDescription() {
        return "주변 3×3×3 범위의 광석을 한 번에 채굴합니다.";
    }

    @Override
    public int getCooldown() {
        return 30; // 30초
    }

    @Override
    public int getManaCost() {
        return 20;
    }

    @Override
    public int getRequiredLevel() {
        return 5;
    }

    @Override
    public String getRequiredJob() {
        return "miner";
    }
}
