package com.dreamwork.core.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 물고기 측정 및 트로피 시스템
 * <p>
 * 잡은 물고기의 크기와 무게를 측정하고
 * 개인/서버 기록을 관리합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class FishTrophySystem implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey fishSizeKey;
    private final NamespacedKey fishWeightKey;

    /** 플레이어별 최대 기록 (UUID -> (물고기 타입 -> 크기)) */
    private final Map<UUID, Map<Material, Double>> playerRecords = new ConcurrentHashMap<>();

    /** 서버 최대 기록 (물고기 타입 -> (크기, 플레이어 이름)) */
    private final Map<Material, FishRecord> serverRecords = new ConcurrentHashMap<>();

    /** 물고기별 기본 크기 범위 (cm) */
    private static final Map<Material, double[]> FISH_SIZE_RANGES = Map.of(
            Material.COD, new double[] { 20, 60 },
            Material.SALMON, new double[] { 40, 90 },
            Material.TROPICAL_FISH, new double[] { 5, 15 },
            Material.PUFFERFISH, new double[] { 15, 40 });

    public FishTrophySystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.fishSizeKey = new NamespacedKey(plugin, "fish_size");
        this.fishWeightKey = new NamespacedKey(plugin, "fish_weight");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH)
            return;

        if (!(event.getCaught() instanceof Item caughtItem))
            return;

        ItemStack fish = caughtItem.getItemStack();
        if (!FISH_SIZE_RANGES.containsKey(fish.getType()))
            return;

        Player player = event.getPlayer();
        var userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        int fisherLevel = userData != null ? userData.getJobLevel(JobType.FISHER) : 0;

        // 크기/무게 생성
        double size = generateSize(fish.getType(), fisherLevel);
        double weight = calculateWeight(fish.getType(), size);

        // NBT 적용
        applyFishData(fish, size, weight);
        caughtItem.setItemStack(fish);

        // 기록 체크
        checkRecord(player, fish.getType(), size);

        // 알림
        String sizeStr = String.format("%.1f", size);
        String weightStr = String.format("%.2f", weight);
        player.sendMessage("§b[어부] §f" + getFishName(fish.getType()) + " §7- §e" + sizeStr + "cm, " + weightStr + "kg");
    }

    /**
     * 물고기 크기를 생성합니다.
     */
    private double generateSize(Material fishType, int level) {
        double[] range = FISH_SIZE_RANGES.get(fishType);
        double base = range[0];
        double max = range[1];

        // 레벨 보너스 (레벨당 0.5% 추가)
        double levelBonus = 1 + (level * 0.005);

        // 정규분포로 크기 생성 (평균에서 약간 위쪽으로)
        double random = ThreadLocalRandom.current().nextGaussian() * 0.2 + 0.5;
        random = Math.max(0, Math.min(1, random));

        double size = base + (max - base) * random * levelBonus;
        return Math.min(size, max * 1.5); // 최대 150%까지
    }

    /**
     * 무게를 계산합니다 (크기^3 비례).
     */
    private double calculateWeight(Material fishType, double size) {
        double densityFactor = switch (fishType) {
            case COD -> 0.00015;
            case SALMON -> 0.00012;
            case TROPICAL_FISH -> 0.00020;
            case PUFFERFISH -> 0.00010;
            default -> 0.00015;
        };
        return size * size * size * densityFactor;
    }

    /**
     * 물고기 아이템에 크기/무게 데이터를 적용합니다.
     */
    private void applyFishData(ItemStack fish, double size, double weight) {
        ItemMeta meta = fish.getItemMeta();
        if (meta == null)
            return;

        String sizeStr = String.format("%.1f", size);
        String weightStr = String.format("%.2f", weight);
        String sizeGrade = getSizeGrade(fish.getType(), size);

        meta.displayName(Component.text("§f" + getFishName(fish.getType()) + " " + sizeGrade));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("§7크기: §f" + sizeStr + " cm"));
        lore.add(Component.text("§7무게: §f" + weightStr + " kg"));
        lore.add(Component.text("§7등급: " + sizeGrade));

        if (isTrophySize(fish.getType(), size)) {
            lore.add(Component.text(""));
            lore.add(Component.text("§6§l★ 트로피 사이즈 ★"));
        }

        meta.lore(lore);
        meta.getPersistentDataContainer().set(fishSizeKey, PersistentDataType.DOUBLE, size);
        meta.getPersistentDataContainer().set(fishWeightKey, PersistentDataType.DOUBLE, weight);
        fish.setItemMeta(meta);
    }

    /**
     * 기록을 체크하고 갱신합니다.
     */
    private void checkRecord(Player player, Material fishType, double size) {
        UUID uuid = player.getUniqueId();

        // 개인 기록
        Map<Material, Double> records = playerRecords.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        Double previousRecord = records.get(fishType);

        if (previousRecord == null || size > previousRecord) {
            records.put(fishType, size);
            if (previousRecord != null) {
                player.sendMessage(
                        "§a§l[신기록!] §f" + getFishName(fishType) + " 개인 기록 갱신: §e" + String.format("%.1f", size) + "cm");
            }
        }

        // 서버 기록
        FishRecord serverRecord = serverRecords.get(fishType);
        if (serverRecord == null || size > serverRecord.size) {
            serverRecords.put(fishType, new FishRecord(size, player.getName()));

            if (serverRecord != null) {
                // 서버 기록 갱신 브로드캐스트
                plugin.getServer().broadcast(Component.text(
                        "§6§l[서버 기록!] §f" + player.getName() + "§7님이 §e" + getFishName(fishType) +
                                " §f" + String.format("%.1f", size) + "cm §7기록 달성!"));
            }
        }
    }

    private String getSizeGrade(Material fishType, double size) {
        double[] range = FISH_SIZE_RANGES.get(fishType);
        double max = range[1];

        if (size >= max * 1.3)
            return "§c§l전설";
        if (size >= max * 1.1)
            return "§6§l대어";
        if (size >= max * 0.9)
            return "§e대형";
        if (size >= max * 0.6)
            return "§a중형";
        return "§7소형";
    }

    private boolean isTrophySize(Material fishType, double size) {
        double[] range = FISH_SIZE_RANGES.get(fishType);
        return size >= range[1] * 1.1;
    }

    private String getFishName(Material material) {
        return switch (material) {
            case COD -> "대구";
            case SALMON -> "연어";
            case TROPICAL_FISH -> "열대어";
            case PUFFERFISH -> "복어";
            default -> material.name();
        };
    }

    public double getPlayerRecord(Player player, Material fishType) {
        Map<Material, Double> records = playerRecords.get(player.getUniqueId());
        return records != null ? records.getOrDefault(fishType, 0.0) : 0.0;
    }

    public FishRecord getServerRecord(Material fishType) {
        return serverRecords.get(fishType);
    }

    public record FishRecord(double size, String playerName) {
    }
}
