package com.dreamwork.core.item.custom;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.job.JobType;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * 보스 토템 (Boss Totem) 아이템
 * <p>
 * 사용 시 해당 지역에 보스 몬스터를 소환합니다.
 * 사냥꾼 전용 아이템입니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class BossTotemItem implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey totemKey;
    private final NamespacedKey bossTypeKey;

    public BossTotemItem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.totemKey = new NamespacedKey(plugin, "boss_totem");
        this.bossTypeKey = new NamespacedKey(plugin, "boss_type");
    }

    /**
     * 보스 토템을 생성합니다.
     */
    public ItemStack createBossTotem(BossType bossType) {
        String color = bossType.color;

        ItemStack item = ItemBuilder.of(Material.TOTEM_OF_UNDYING)
                .name(color + "§l" + bossType.displayName + " 토템")
                .lore("")
                .lore("§7이 토템을 사용하면")
                .lore(color + bossType.displayName + "§7을(를) 소환합니다.")
                .lore("")
                .lore("§c⚠ 주의: 강력한 몬스터입니다!")
                .lore("")
                .lore("§e[우클릭]§f 보스 소환")
                .lore("")
                .lore("§8필요 레벨: 사냥꾼 Lv." + bossType.requiredLevel)
                .customModelData(10020 + bossType.ordinal())
                .build();

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(totemKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(bossTypeKey, PersistentDataType.STRING, bossType.name());
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick())
            return;

        ItemStack item = event.getItem();
        if (!isBossTotem(item))
            return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        ItemMeta meta = item.getItemMeta();
        String bossTypeName = meta.getPersistentDataContainer().get(bossTypeKey, PersistentDataType.STRING);

        if (bossTypeName == null)
            return;

        BossType bossType;
        try {
            bossType = BossType.valueOf(bossTypeName);
        } catch (IllegalArgumentException e) {
            return;
        }

        // 레벨 확인
        var userData = plugin.getStorageManager().getUserData(player.getUniqueId());
        int hunterLevel = userData != null ? userData.getJobLevel(JobType.HUNTER) : 0;

        if (hunterLevel < bossType.requiredLevel) {
            player.sendMessage("§c[사냥꾼] 이 토템을 사용하려면 레벨 " + bossType.requiredLevel + " 이상이 필요합니다.");
            return;
        }

        // 보스 소환
        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(3));
        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);

        var boss = player.getWorld().spawnEntity(spawnLoc, bossType.entityType);

        if (boss instanceof org.bukkit.entity.LivingEntity living) {
            living.customName(net.kyori.adventure.text.Component.text(bossType.color + "§l" + bossType.displayName));
            living.setCustomNameVisible(true);

            // 강화된 스탯
            var maxHealth = living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(maxHealth.getValue() * bossType.healthMultiplier);
                living.setHealth(maxHealth.getValue());
            }
        }

        // 파티클 및 사운드
        player.getWorld().spawnParticle(Particle.EXPLOSION, spawnLoc, 3, 0.5, 0.5, 0.5, 0);
        player.getWorld().playSound(spawnLoc, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);

        // 아이템 소모
        item.setAmount(item.getAmount() - 1);

        // 브로드캐스트
        player.sendMessage("§c[사냥꾼] §e" + bossType.displayName + "§c을(를) 소환했습니다!");

        // 경험치 보상 (처치 시)
        boss.setMetadata("dreamwork_boss", new org.bukkit.metadata.FixedMetadataValue(plugin, bossType.name()));
    }

    public boolean isBossTotem(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(totemKey, PersistentDataType.BYTE);
    }

    /**
     * 보스 유형 정의
     */
    public enum BossType {
        ZOMBIE_KING("좀비 왕", EntityType.ZOMBIE, "§2", 25, 5.0),
        SKELETON_LORD("스켈레톤 군주", EntityType.SKELETON, "§f", 35, 4.0),
        CREEPER_PRIME("크리퍼 프라임", EntityType.CREEPER, "§a", 45, 3.0),
        SPIDER_QUEEN("거미 여왕", EntityType.SPIDER, "§8", 55, 6.0),
        ENDERMAN_ELDER("엔더맨 장로", EntityType.ENDERMAN, "§5", 70, 8.0),
        WARDEN_ANCIENT("고대 워든", EntityType.WARDEN, "§0", 90, 2.0);

        public final String displayName;
        public final EntityType entityType;
        public final String color;
        public final int requiredLevel;
        public final double healthMultiplier;

        BossType(String displayName, EntityType entityType, String color, int requiredLevel, double healthMultiplier) {
            this.displayName = displayName;
            this.entityType = entityType;
            this.color = color;
            this.requiredLevel = requiredLevel;
            this.healthMultiplier = healthMultiplier;
        }
    }
}
