package com.dreamwork.core.item.custom;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.job.JobType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 미확인 광물 (Unidentified Ore) 아이템 시스템
 * <p>
 * 광물 채굴 시 낮은 확률로 드롭되며,
 * 감정 시 랜덤 아이템으로 변환됩니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class UnidentifiedOreItem implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey oreKey;

    /** 드롭 확률 (0.1% from design spec) */
    private static final double DROP_CHANCE = 0.001;

    /** 레벨당 추가 확률 */
    private static final double LEVEL_BONUS = 0.0001;

    /** 감정 결과 확률 (기획서 기준: 60% 꽝, 30% 주괴, 9% 다이아/에메랄드, 1% 고대 화석) */
    private static final double INGOT_CHANCE = 0.30; // 30% 철/금/구리 주괴 묶음
    private static final double DIAMOND_CHANCE = 0.09; // 9% 다이아몬드/에메랄드
    private static final double FOSSIL_CHANCE = 0.01; // 1% 고대 화석

    public UnidentifiedOreItem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.oreKey = new NamespacedKey(plugin, "unidentified_ore");
    }

    /**
     * 미확인 광물을 생성합니다.
     */
    public ItemStack createUnidentifiedOre() {
        ItemStack item = ItemBuilder.of(Material.RAW_IRON)
                .name("§6§l미확인 광물")
                .lore("")
                .lore("§7정체를 알 수 없는 돌덩이입니다.")
                .lore("§7감정하면 다양한 아이템이 나올 수 있습니다.")
                .lore("")
                .lore("§e[우클릭]§f 감정하기")
                .lore("")
                .lore("§860% §7자갈/부싯돌 (꽝)")
                .lore("§a30% §7철/금/구리 주괴 묶음")
                .lore("§b9% §7다이아몬드/에메랄드")
                .lore("§51% §7고대 화석 (박물관 기증용)")
                .customModelData(10010)
                .build();

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(oreKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Material blockType = event.getBlock().getType();

        if (!isOre(blockType))
            return;

        Player player = event.getPlayer();
        var userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        int minerLevel = userData != null ? userData.getJobLevel(JobType.MINER) : 0;

        double chance = DROP_CHANCE + (minerLevel * LEVEL_BONUS);

        if (ThreadLocalRandom.current().nextDouble() < chance) {
            ItemStack ore = createUnidentifiedOre();
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), ore);

            player.sendMessage("§e[광부] §f미확인 광물을 발견했습니다!");
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick())
            return;

        ItemStack item = event.getItem();
        if (!isUnidentifiedOre(item))
            return;

        event.setCancelled(true);

        Player player = event.getPlayer();

        // 감정 결과 결정 (확률 기반)
        double roll = ThreadLocalRandom.current().nextDouble();
        ItemStack rewardItem;
        String rewardName;
        String rewardMessage;

        if (roll < FOSSIL_CHANCE) {
            // 1% - 고대 화석 (박물관 기증용 희귀템)
            rewardItem = ItemBuilder.of(Material.BONE)
                    .name("§5§l고대 화석")
                    .lore("")
                    .lore("§7고대 생물의 화석입니다.")
                    .lore("§7박물관에 기증하면 큰 보상을 받을 수 있습니다.")
                    .lore("")
                    .lore("§8[초희귀]")
                    .customModelData(10020)
                    .build();
            rewardName = "고대 화석";
            rewardMessage = "§d§l[대박!] §5고대 화석§f을 발견했습니다!";
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        } else if (roll < FOSSIL_CHANCE + DIAMOND_CHANCE) {
            // 9% - 다이아몬드/에메랄드
            boolean isDiamond = ThreadLocalRandom.current().nextBoolean();
            Material material = isDiamond ? Material.DIAMOND : Material.EMERALD;
            int amount = ThreadLocalRandom.current().nextInt(1, 4); // 1~3개
            rewardItem = new ItemStack(material, amount);
            rewardName = isDiamond ? "다이아몬드" : "에메랄드";
            rewardMessage = "§b[희귀!] §f" + rewardName + " §e" + amount + "개§f를 획득했습니다!";
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        } else if (roll < FOSSIL_CHANCE + DIAMOND_CHANCE + INGOT_CHANCE) {
            // 30% - 철/금/구리 주괴 묶음
            Material material = switch (ThreadLocalRandom.current().nextInt(3)) {
                case 0 -> Material.IRON_INGOT;
                case 1 -> Material.GOLD_INGOT;
                default -> Material.COPPER_INGOT;
            };
            int amount = ThreadLocalRandom.current().nextInt(3, 9); // 3~8개
            rewardItem = new ItemStack(material, amount);
            rewardName = getItemName(material);
            rewardMessage = "§a[성공] §f" + rewardName + " §e" + amount + "개§f를 획득했습니다.";
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        } else {
            // 60% - 꽝 (자갈/부싯돌)
            boolean isGravel = ThreadLocalRandom.current().nextBoolean();
            Material material = isGravel ? Material.GRAVEL : Material.FLINT;
            int amount = ThreadLocalRandom.current().nextInt(1, 4); // 1~3개
            rewardItem = new ItemStack(material, amount);
            rewardName = isGravel ? "자갈" : "부싯돌";
            rewardMessage = "§7[꽝] §f" + rewardName + " §e" + amount + "개§f... 아쉽네요.";
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        }

        // 아이템 소모
        item.setAmount(item.getAmount() - 1);

        // 보상 지급
        player.getInventory().addItem(rewardItem);
        player.sendMessage("§e[감정] " + rewardMessage);
    }

    private boolean isOre(Material material) {
        return material.name().contains("ORE") || material == Material.ANCIENT_DEBRIS;
    }

    public boolean isUnidentifiedOre(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(oreKey, PersistentDataType.BYTE);
    }

    private String getItemName(Material material) {
        return switch (material) {
            case COAL -> "석탄";
            case IRON_INGOT -> "철 주괴";
            case COPPER_INGOT -> "구리 주괴";
            case GOLD_INGOT -> "금 주괴";
            case LAPIS_LAZULI -> "청금석";
            case REDSTONE -> "레드스톤";
            case DIAMOND -> "다이아몬드";
            case EMERALD -> "에메랄드";
            case AMETHYST_SHARD -> "자수정 조각";
            case NETHERITE_SCRAP -> "네더라이트 파편";
            default -> material.name();
        };
    }
}
