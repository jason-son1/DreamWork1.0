package com.dreamwork.core.job.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.job.JobType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 작물 등급 시스템 (Crop Quality System)
 * 
 * <p>
 * 농부가 작물을 수확할 때 등급(1~3성)을 부여합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class CropQualitySystem implements Listener {

    private final DreamWorkCore plugin;

    public CropQualitySystem(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropHarvest(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isFullyGrownCrop(block))
            return;

        Player player = event.getPlayer();

        // 농부 직업 체크가 굳이 필요 없다면 생략 가능하지만,
        // 다중 직업 시스템이므로 해당 유저가 '농부 스탯(행운)'을 얼마나 투자했는지가 중요.
        // 여기서는 UserData의 Luk 스탯을 가져와서 계산합니다.

        var userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null)
            return;

        // 농부 잡이 아예 없거나 하는 경우엔 등급 시스템 미적용? -> 기획상 모든 유저는 농부이기도 함.
        // 다만 '농부 레벨'이 낮으면 고등급 확률이 낮아야 함.

        event.setDropItems(false); // 기본 드롭 취소하고 커스텀 드롭 처리

        int farmerLevel = userData.getJobLevel(JobType.FARMER);
        int luck = userData.getLuk();

        // 작물 타입에 따른 기본 아이템 결정
        Material cropType = block.getType();
        Material dropType = getDropType(cropType);
        int amount = ThreadLocalRandom.current().nextInt(1, 4); // 1~3개

        // 등급 결정
        int quality = determineQuality(farmerLevel, luck);

        // 아이템 생성 및 드롭
        ItemStack item = createCropItem(dropType, amount, quality);
        block.getWorld().dropItemNaturally(block.getLocation(), item);

        // 씨앗 등 부산물 드롭 (기본 드롭 로직 일부 흉내)
        dropSeeds(block, cropType);

        // 고등급 획득 시 메시지/사운드
        if (quality >= 3) {
            player.sendActionBar(Component.text("§6§l★★★ 황금빛 작물 수확!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
        } else if (quality == 2) {
            player.sendActionBar(Component.text("§a★★ 싱싱한 작물 수확!"));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
        }
    }

    private boolean isFullyGrownCrop(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    private int determineQuality(int level, int luck) {
        double rand = ThreadLocalRandom.current().nextDouble() * 100;

        // 3성 확률: 기본 0% + (레벨 * 0.1)% + (행운 * 0.2)%
        // 예: 50렙, 행운 20 -> 5 + 4 = 9%
        double chance3 = (level * 0.1) + (luck * 0.2);

        // 2성 확률: 기본 10% + (레벨 * 0.3)% + (행운 * 0.5)%
        // 예: 50렙, 행운 20 -> 10 + 15 + 10 = 35%
        double chance2 = 10.0 + (level * 0.3) + (luck * 0.5);

        if (rand < chance3)
            return 3;
        if (rand < chance3 + chance2)
            return 2;
        return 1;
    }

    private ItemStack createCropItem(Material material, int amount, int quality) {
        String name = switch (quality) {
            case 3 -> "§6황금빛 " + getKoreanName(material);
            case 2 -> "§a싱싱한 " + getKoreanName(material);
            default -> "§f" + getKoreanName(material);
        };

        ItemBuilder builder = ItemBuilder.of(material)
                .amount(amount)
                .name(name);

        if (quality > 1) {
            builder.lore("");
            builder.lore("§7등급: " + "★".repeat(quality));
            if (quality == 3) {
                builder.lore("§6[최상급] §7요리 재료로 사용 시 효과 2배");
                builder.enchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1); // 반짝임 효과
                builder.flag(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else if (quality == 2) {
                builder.lore("§a[상급] §7상점 판매가 2배");
            }
        }

        ItemStack item = builder.build();

        // NBT(PDC)에 등급 저장 (추후 요리/판매 시스템에서 식별용)
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "crop_quality"),
                    PersistentDataType.INTEGER,
                    quality);
            item.setItemMeta(meta);
        }

        return item;
    }

    private Material getDropType(Material cropBlock) {
        return switch (cropBlock) {
            case WHEAT -> Material.WHEAT;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT;
            case NETHER_WART -> Material.NETHER_WART;
            default -> cropBlock; // 호박/수박 등은 블록 자체가 드롭되므로 처리 방식 다를 수 있음
        };
    }

    private void dropSeeds(Block block, Material cropType) {
        Material seedType = switch (cropType) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            default -> null; // 당근/감자는 작물 자체가 씨앗
        };

        if (seedType != null) {
            int amount = ThreadLocalRandom.current().nextInt(0, 3);
            if (amount > 0) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(seedType, amount));
            }
        }
    }

    private String getKoreanName(Material material) {
        return switch (material) {
            case WHEAT -> "밀";
            case CARROT -> "당근";
            case POTATO -> "감자";
            case BEETROOT -> "비트";
            case NETHER_WART -> "네더 와트";
            default -> material.name();
        };
    }
}
