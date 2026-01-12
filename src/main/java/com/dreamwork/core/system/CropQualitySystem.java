package com.dreamwork.core.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 작물 품질 시스템
 * <p>
 * 수확하는 작물에 1~3성 품질을 부여합니다.
 * 높은 품질의 작물은 더 많은 판매가와 효과를 제공합니다.
 * </p>
 * <p>
 * 품질 결정 요소:
 * - 농부 레벨
 * - 행운 스탯
 * - 바이옴 보너스
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class CropQualitySystem implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey qualityKey;

    public CropQualitySystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.qualityKey = new NamespacedKey(plugin, "crop_quality");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (!isCrop(block.getType()))
            return;

        // 다 자란 작물인지 확인
        if (block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() < ageable.getMaximumAge())
                return;
        } else {
            return; // Ageable이 아니면 무시
        }

        Player player = event.getPlayer();
        UserData userData = plugin.getStorageManager().getUserData(player.getUniqueId());

        int farmerLevel = userData != null ? userData.getJobLevel(JobType.FARMER) : 0;
        int luckStat = userData != null ? userData.getLuk() : 0;

        // 품질 결정
        int quality = determineQuality(farmerLevel, luckStat);

        // 드롭 아이템에 품질 적용
        event.setDropItems(false);

        ItemStack drop = getQualityCrop(block.getType(), quality);
        if (drop != null) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);

            // 고품질 알림
            if (quality >= 3) {
                player.sendMessage("§a[농부] §e★★★ 최고 품질§f의 작물을 수확했습니다!");
            }
        }
    }

    /**
     * 품질을 결정합니다 (1~3성).
     */
    private int determineQuality(int farmerLevel, int luckStat) {
        double baseChance = 0.05 + (farmerLevel * 0.002) + (luckStat * 0.005);
        double rand = ThreadLocalRandom.current().nextDouble();

        // 3성 확률
        if (rand < baseChance * 0.2) {
            return 3;
        }
        // 2성 확률
        if (rand < baseChance) {
            return 2;
        }
        // 1성
        return 1;
    }

    /**
     * 품질이 적용된 작물 아이템을 생성합니다.
     */
    private ItemStack getQualityCrop(Material cropType, int quality) {
        Material dropMaterial = getCropDrop(cropType);
        if (dropMaterial == null)
            return null;

        String qualityStars = "★".repeat(quality) + "☆".repeat(3 - quality);
        String qualityColor = switch (quality) {
            case 3 -> "§e";
            case 2 -> "§a";
            default -> "§f";
        };

        String displayName = qualityColor + getCropName(dropMaterial) + " §7[" + qualityStars + "]";

        ItemStack item = new ItemStack(dropMaterial, getDropAmount(quality));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(displayName));
            meta.getPersistentDataContainer().set(qualityKey, PersistentDataType.INTEGER, quality);

            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text(""));
            lore.add(net.kyori.adventure.text.Component.text("§7품질: " + qualityColor + qualityStars));
            lore.add(net.kyori.adventure.text.Component.text("§7판매가 보너스: §e+" + ((quality - 1) * 25) + "%"));

            if (quality == 3) {
                lore.add(net.kyori.adventure.text.Component.text(""));
                lore.add(net.kyori.adventure.text.Component.text("§6최고 품질의 작물입니다!"));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private Material getCropDrop(Material cropType) {
        return switch (cropType) {
            case WHEAT -> Material.WHEAT;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT;
            case NETHER_WART -> Material.NETHER_WART;
            case MELON -> Material.MELON_SLICE;
            case PUMPKIN -> Material.PUMPKIN;
            default -> null;
        };
    }

    private String getCropName(Material material) {
        return switch (material) {
            case WHEAT -> "밀";
            case CARROT -> "당근";
            case POTATO -> "감자";
            case BEETROOT -> "비트";
            case NETHER_WART -> "네더 와트";
            case MELON_SLICE -> "수박 조각";
            case PUMPKIN -> "호박";
            default -> material.name();
        };
    }

    private int getDropAmount(int quality) {
        return switch (quality) {
            case 3 -> ThreadLocalRandom.current().nextInt(3, 6);
            case 2 -> ThreadLocalRandom.current().nextInt(2, 4);
            default -> ThreadLocalRandom.current().nextInt(1, 3);
        };
    }

    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART -> true;
            default -> false;
        };
    }

    /**
     * 작물 품질을 반환합니다.
     */
    public int getQuality(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(qualityKey, PersistentDataType.INTEGER, 0);
    }
}
