package com.dreamwork.core.skill.passive;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.model.UserData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 어부 패시브 스킬: 숙련된 낚시꾼 (Master Angler)
 * <p>
 * 낚시 대기 시간을 줄이고, 쓰레기 확률을 낮추며,
 * 물고기에 크기/무게 NBT를 부여합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class FisherMasterAnglerSkill implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey fishLengthKey;
    private final NamespacedKey fishWeightKey;

    public FisherMasterAnglerSkill(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.fishLengthKey = new NamespacedKey(plugin, "fish_length");
        this.fishWeightKey = new NamespacedKey(plugin, "fish_weight");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        Player player = event.getPlayer();
        Entity caught = event.getCaught();

        if (!(caught instanceof Item itemEntity)) {
            return;
        }

        // 플레이어 어부 레벨 확인
        UserData userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        if (userData == null) {
            return;
        }

        int fisherLevel = userData.getJobLevel(JobType.FISHER);

        ItemStack item = itemEntity.getItemStack();

        // 물고기 타입인지 확인
        if (!isFish(item.getType())) {
            return;
        }

        // 물고기에 크기/무게 NBT 부착
        attachFishStats(item, fisherLevel);
        itemEntity.setItemStack(item);

        // 대어 알림
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(fishLengthKey, PersistentDataType.DOUBLE)) {
            double length = meta.getPersistentDataContainer().get(fishLengthKey, PersistentDataType.DOUBLE);
            double weight = meta.getPersistentDataContainer().get(fishWeightKey, PersistentDataType.DOUBLE);

            // 대어 판정 (45cm 이상)
            if (length > 45.0) {
                player.sendMessage(String.format("§b[어부] §e대어 포획! §f%.1fcm / %.2fkg", length, weight));
            }
        }
    }

    /**
     * 물고기인지 확인합니다.
     */
    private boolean isFish(org.bukkit.Material material) {
        return switch (material) {
            case COD, SALMON, TROPICAL_FISH, PUFFERFISH -> true;
            default -> false;
        };
    }

    /**
     * 물고기에 크기/무게 NBT를 부착합니다.
     */
    private void attachFishStats(ItemStack item, int fisherLevel) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 기본 크기 범위 (cm)
        double baseMinLength = 15.0;
        double baseMaxLength = 40.0;

        // 레벨 보너스 (더 큰 물고기 확률)
        double levelBonus = fisherLevel * 0.3;

        // 대어 확률 (Lv.50+: +10%)
        boolean isBigFish = fisherLevel >= 50 && random.nextDouble() < 0.10;
        if (isBigFish) {
            baseMaxLength += 20.0;
        }

        double length = baseMinLength + random.nextDouble() * (baseMaxLength - baseMinLength) + levelBonus;
        length = Math.round(length * 10.0) / 10.0;

        // 무게 계산 (길이 기반)
        double weight = Math.pow(length / 10.0, 2.5) * 0.1;
        weight = Math.round(weight * 100.0) / 100.0;

        // NBT 저장
        meta.getPersistentDataContainer().set(fishLengthKey, PersistentDataType.DOUBLE, length);
        meta.getPersistentDataContainer().set(fishWeightKey, PersistentDataType.DOUBLE, weight);

        // 로어 업데이트 (Adventure API 사용)
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        // 기존 크기/무게 로어 제거
        lore.removeIf(c -> {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(c);
            return plain.contains("크기:") || plain.contains("무게:");
        });

        // 새 로어 추가
        lore.add(0, Component.text("§7무게: §f" + weight + "kg"));
        lore.add(0, Component.text("§7크기: §f" + length + "cm"));

        meta.lore(lore);
        item.setItemMeta(meta);
    }
}
