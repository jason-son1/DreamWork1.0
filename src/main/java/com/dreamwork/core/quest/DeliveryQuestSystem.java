package com.dreamwork.core.quest;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 배달 퀘스트 시스템
 * <p>
 * 탐험가가 아이템을 특정 NPC/위치에 배달하는 퀘스트입니다.
 * 거리에 따라 보상이 책정됩니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class DeliveryQuestSystem {

    private final DreamWorkCore plugin;

    /** 플레이어별 활성 배달 퀘스트 */
    private final Map<UUID, DeliveryQuest> activeQuests = new ConcurrentHashMap<>();

    /** 배달 가능 아이템 풀 */
    private static final List<DeliveryItem> DELIVERY_ITEMS = List.of(
            new DeliveryItem("편지", Material.PAPER, 10, 50),
            new DeliveryItem("소포", Material.CHEST, 30, 100),
            new DeliveryItem("귀중품", Material.GOLD_INGOT, 50, 200),
            new DeliveryItem("긴급 물품", Material.ENDER_PEARL, 100, 500),
            new DeliveryItem("비밀 문서", Material.MAP, 200, 1000));

    /** 배달지 풀 (NPC 이름으로 가상 표현) */
    private static final List<String> DELIVERY_RECIPIENTS = List.of(
            "§e마을 이장 김철수", "§e대장장이 박영수", "§e상점 주인 이미영",
            "§e광부 조합장 최동훈", "§e농부 대표 정수진", "§e어부 촌장 한지민");

    public DeliveryQuestSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 새로운 배달 퀘스트를 생성합니다.
     */
    public boolean startQuest(Player player) {
        UUID uuid = player.getUniqueId();

        if (activeQuests.containsKey(uuid)) {
            player.sendMessage("§c[배달] 이미 진행 중인 배달이 있습니다.");
            return false;
        }

        var userData = plugin.getStorageManager().getUserData(uuid);
        int explorerLevel = userData != null ? userData.getJobLevel(JobType.EXPLORER) : 0;

        if (explorerLevel < 10) {
            player.sendMessage("§c[배달] 배달 퀘스트는 탐험가 레벨 10 이상부터 가능합니다.");
            return false;
        }

        // 랜덤 퀘스트 생성
        DeliveryItem item = DELIVERY_ITEMS.get(ThreadLocalRandom.current().nextInt(DELIVERY_ITEMS.size()));
        String recipient = DELIVERY_RECIPIENTS.get(ThreadLocalRandom.current().nextInt(DELIVERY_RECIPIENTS.size()));

        // 목적지 생성 (현재 위치에서 일정 거리)
        int distance = 200 + (explorerLevel * 10);
        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        Location destination = player.getLocation().clone().add(
                Math.cos(angle) * distance,
                0,
                Math.sin(angle) * distance);
        destination.setY(player.getWorld().getHighestBlockYAt(destination) + 1);

        // 보상 계산
        int baseReward = item.baseReward + (distance / 10);
        double expReward = item.baseExp + (distance * 0.5);

        DeliveryQuest quest = new DeliveryQuest(
                item.name, item.material, recipient, destination,
                baseReward, expReward, System.currentTimeMillis());

        activeQuests.put(uuid, quest);

        // 배달 아이템 지급
        ItemStack deliveryItem = new ItemStack(item.material);
        var meta = deliveryItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§e[배달] §f" + item.name));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("§7수령인: " + recipient));
            lore.add(Component.text("§7목적지: §f" + (int) destination.getX() + ", " + (int) destination.getZ()));
            lore.add(Component.text(""));
            lore.add(Component.text("§c※ 배달 퀘스트 아이템입니다"));
            meta.lore(lore);
            deliveryItem.setItemMeta(meta);
        }
        player.getInventory().addItem(deliveryItem);

        // 안내 메시지
        player.sendMessage("§a[배달] §f새로운 배달 퀘스트!");
        player.sendMessage("§7  아이템: §e" + item.name);
        player.sendMessage("§7  수령인: " + recipient);
        player.sendMessage("§7  목적지: §f" + (int) destination.getX() + ", " + (int) destination.getZ());
        player.sendMessage("§7  거리: §e" + distance + " blocks");
        player.sendMessage("§7  보상: §e" + baseReward + " D, " + (int) expReward + " XP");

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_CARTOGRAPHER, 1.0f, 1.0f);

        return true;
    }

    /**
     * 배달을 완료합니다.
     */
    public boolean completeQuest(Player player) {
        UUID uuid = player.getUniqueId();

        DeliveryQuest quest = activeQuests.get(uuid);
        if (quest == null) {
            player.sendMessage("§c[배달] 진행 중인 배달이 없습니다.");
            return false;
        }

        // 거리 확인
        double distance = player.getLocation().distance(quest.destination);
        if (distance > 10) {
            player.sendMessage("§c[배달] 목적지에서 너무 멀리 있습니다. §7(" + (int) distance + " blocks 남음)");
            return false;
        }

        // 배달 아이템 확인 및 제거
        boolean hasItem = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == quest.material &&
                    item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                item.setAmount(item.getAmount() - 1);
                hasItem = true;
                break;
            }
        }

        if (!hasItem) {
            player.sendMessage("§c[배달] 배달 아이템이 없습니다!");
            return false;
        }

        // 퀘스트 완료
        activeQuests.remove(uuid);

        // 보상 지급
        if (plugin.getHookManager().isVaultEnabled()) {
            plugin.getHookManager().getVaultHook().deposit(player, quest.moneyReward);
        }
        plugin.getJobManager().addExp(player, JobType.EXPLORER, quest.expReward);

        // 완료 시간 보너스
        long elapsed = System.currentTimeMillis() - quest.startTime;
        int bonusExp = 0;
        if (elapsed < 120000) { // 2분 이내
            bonusExp = 100;
            player.sendMessage("§6[보너스] §f빠른 배달! +100 XP");
        } else if (elapsed < 300000) { // 5분 이내
            bonusExp = 50;
            player.sendMessage("§6[보너스] §f신속 배달! +50 XP");
        }
        if (bonusExp > 0) {
            plugin.getJobManager().addExp(player, JobType.EXPLORER, bonusExp);
        }

        // 완료 메시지
        player.sendMessage("§a§l[배달 완료!]");
        player.sendMessage("§7  보상: §e" + quest.moneyReward + " D, " + (int) quest.expReward + " XP");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);

        return true;
    }

    /**
     * 배달 퀘스트를 취소합니다.
     */
    public void cancelQuest(Player player) {
        if (activeQuests.remove(player.getUniqueId()) != null) {
            player.sendMessage("§e[배달] 배달이 취소되었습니다.");
        }
    }

    public boolean hasActiveQuest(Player player) {
        return activeQuests.containsKey(player.getUniqueId());
    }

    public DeliveryQuest getActiveQuest(Player player) {
        return activeQuests.get(player.getUniqueId());
    }

    private record DeliveryItem(String name, Material material, int baseExp, int baseReward) {
    }

    public record DeliveryQuest(String itemName, Material material, String recipient,
            Location destination, int moneyReward, double expReward, long startTime) {
    }
}
