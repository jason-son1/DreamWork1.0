package com.dreamwork.core.item.custom;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.item.ItemBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * 워프 스크롤 (Warp Scroll) 아이템
 * <p>
 * 사용 시 저장된 좌표로 순간이동합니다.
 * 탐험가 직업으로 제작 가능합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class WarpScrollItem implements Listener {

    private final DreamWorkCore plugin;
    private final NamespacedKey scrollKey;
    private final NamespacedKey coordXKey;
    private final NamespacedKey coordYKey;
    private final NamespacedKey coordZKey;
    private final NamespacedKey worldKey;

    public WarpScrollItem(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.scrollKey = new NamespacedKey(plugin, "warp_scroll");
        this.coordXKey = new NamespacedKey(plugin, "warp_x");
        this.coordYKey = new NamespacedKey(plugin, "warp_y");
        this.coordZKey = new NamespacedKey(plugin, "warp_z");
        this.worldKey = new NamespacedKey(plugin, "warp_world");
    }

    /**
     * 빈 워프 스크롤을 생성합니다.
     */
    public ItemStack createBlankScroll(int amount) {
        ItemStack item = ItemBuilder.of(Material.PAPER)
                .name("§b§l워프 스크롤 §7(빈)")
                .lore("")
                .lore("§7좌표가 기록되지 않은 스크롤입니다.")
                .lore("")
                .lore("§e[Shift + 우클릭]§f 현재 위치 기록")
                .lore("")
                .lore("§8제작: 탐험가 Lv.25+")
                .customModelData(10002)
                .build();

        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(scrollKey, PersistentDataType.STRING, "blank");
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 좌표가 기록된 워프 스크롤을 생성합니다.
     */
    public ItemStack createWarpScroll(Location location, String label) {
        String coordsText = String.format("§f%s §7(%d, %d, %d)",
                label,
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());

        ItemStack item = ItemBuilder.of(Material.MAP)
                .name("§b§l워프 스크롤")
                .lore("")
                .lore("§7목적지: " + coordsText)
                .lore("")
                .lore("§e[우클릭]§f 목적지로 이동")
                .lore("")
                .lore("§c※ 사용 시 소모됩니다")
                .customModelData(10003)
                .build();

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            var pdc = meta.getPersistentDataContainer();
            pdc.set(scrollKey, PersistentDataType.STRING, "recorded");
            pdc.set(coordXKey, PersistentDataType.DOUBLE, location.getX());
            pdc.set(coordYKey, PersistentDataType.DOUBLE, location.getY());
            pdc.set(coordZKey, PersistentDataType.DOUBLE, location.getZ());
            pdc.set(worldKey, PersistentDataType.STRING, location.getWorld().getName());
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick())
            return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta())
            return;

        ItemMeta meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();

        if (!pdc.has(scrollKey, PersistentDataType.STRING))
            return;

        String scrollType = pdc.get(scrollKey, PersistentDataType.STRING);
        Player player = event.getPlayer();

        event.setCancelled(true);

        if ("blank".equals(scrollType)) {
            // 빈 스크롤 - 좌표 기록
            if (player.isSneaking()) {
                recordLocation(player, item);
            } else {
                player.sendMessage("§c[워프스크롤] §fShift + 우클릭으로 현재 위치를 기록하세요.");
            }
        } else if ("recorded".equals(scrollType)) {
            // 기록된 스크롤 - 텔레포트
            teleportToScroll(player, item, pdc);
        }
    }

    private void recordLocation(Player player, ItemStack item) {
        Location loc = player.getLocation();

        // 새 스크롤로 교체
        ItemStack newScroll = createWarpScroll(loc, loc.getWorld().getName());

        item.setType(newScroll.getType());
        item.setItemMeta(newScroll.getItemMeta());

        player.sendMessage("§b[워프스크롤] §f현재 위치가 기록되었습니다!");
        player.playSound(loc, Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    private void teleportToScroll(Player player, ItemStack item, org.bukkit.persistence.PersistentDataContainer pdc) {
        Double x = pdc.get(coordXKey, PersistentDataType.DOUBLE);
        Double y = pdc.get(coordYKey, PersistentDataType.DOUBLE);
        Double z = pdc.get(coordZKey, PersistentDataType.DOUBLE);
        String worldName = pdc.get(worldKey, PersistentDataType.STRING);

        if (x == null || y == null || z == null || worldName == null) {
            player.sendMessage("§c[워프스크롤] 스크롤이 손상되었습니다.");
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage("§c[워프스크롤] 목적지 월드를 찾을 수 없습니다.");
            return;
        }

        Location target = new Location(world, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());

        // 텔레포트
        player.teleport(target);
        player.sendMessage("§b[워프스크롤] §f목적지로 이동했습니다!");
        player.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.PORTAL, target, 30, 0.5, 1, 0.5, 0.1);

        // 아이템 소모
        item.setAmount(item.getAmount() - 1);
    }

    public boolean isWarpScroll(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        return item.getItemMeta().getPersistentDataContainer().has(scrollKey, PersistentDataType.STRING);
    }
}
