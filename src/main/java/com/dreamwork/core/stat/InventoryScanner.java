package com.dreamwork.core.stat;

import com.dreamwork.core.DreamWorkCore;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * 플레이어 장비 스캐너
 * 
 * <p>
 * 장비의 Lore에서 스탯을 읽어 PlayerStats에 반영합니다.
 * 장비 변경 이벤트 시 자동으로 스탯을 재계산합니다.
 * </p>
 */
public class InventoryScanner implements Listener {

    private final DreamWorkCore plugin;
    private final StatParser parser;
    private final StatManager statManager;

    public InventoryScanner(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.parser = new StatParser();
        this.statManager = plugin.getStatManager();
    }

    /**
     * 플레이어의 모든 장비를 스캔하여 스탯 보너스를 계산합니다.
     * 
     * @param player 플레이어
     * @return 장비에서 얻은 스탯 배열 [str, dex, con, int, luck]
     */
    public int[] scan(Player player) {
        int[] totalStats = new int[5];
        PlayerInventory inv = player.getInventory();

        // 방어구 스캔 (헬멧, 흉갑, 각반, 부츠)
        for (ItemStack armor : inv.getArmorContents()) {
            addItemStats(armor, totalStats);
        }

        // 메인핸드 & 오프핸드
        addItemStats(inv.getItemInMainHand(), totalStats);
        addItemStats(inv.getItemInOffHand(), totalStats);

        return totalStats;
    }

    /**
     * 아이템에서 스탯을 읽어 합산합니다.
     */
    private void addItemStats(ItemStack item, int[] stats) {
        if (item == null || !item.hasItemMeta())
            return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore())
            return;

        // Adventure API lore -> String 변환
        List<String> loreStrings = new ArrayList<>();
        if (meta.lore() != null) {
            for (var component : meta.lore()) {
                loreStrings.add(PlainTextComponentSerializer.plainText().serialize(component));
            }
        }

        int[] itemStats = parser.parseAllStats(loreStrings);
        for (int i = 0; i < 5; i++) {
            stats[i] += itemStats[i];
        }
    }

    /**
     * 장비 변경 시 스탯 재계산을 스케줄링합니다.
     */
    public void scheduleRecalculate(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    applyEquipmentStats(player);
                }
            }
        }.runTaskLater(plugin, 1L); // 1틱 후 실행 (아이템 이동 완료 후)
    }

    /**
     * 장비 스탯을 PlayerStats에 적용합니다.
     */
    public void applyEquipmentStats(Player player) {
        int[] equipStats = scan(player);

        StatManager.PlayerStats stats = statManager.getStats(player);

        // 장비 보너스 설정 (기존 보너스에 추가가 아닌 교체)
        // recalculateStats에서 직업 보너스와 함께 처리되므로 여기선 직접 호출
        stats.setEquipmentStr(equipStats[0]);
        stats.setEquipmentDex(equipStats[1]);
        stats.setEquipmentCon(equipStats[2]);
        stats.setEquipmentInt(equipStats[3]);
        stats.setEquipmentLuck(equipStats[4]);

        // 바닐라 속성 재적용
        statManager.recalculateStats(player);

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[Debug] 장비 스탯 적용: " + player.getName() +
                    " [STR:" + equipStats[0] + ", DEX:" + equipStats[1] +
                    ", CON:" + equipStats[2] + ", INT:" + equipStats[3] +
                    ", LUCK:" + equipStats[4] + "]");
        }
    }

    // ==================== 이벤트 핸들러 ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        scheduleRecalculate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        scheduleRecalculate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        // 방어구 슬롯이나 핫바 변경 시
        int slot = event.getSlot();
        if (isEquipmentSlot(slot) || event.isShiftClick()) {
            scheduleRecalculate(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        scheduleRecalculate(event.getPlayer());
    }

    /**
     * 슬롯이 장비 슬롯인지 확인합니다.
     */
    private boolean isEquipmentSlot(int slot) {
        // 36-39: 방어구 슬롯, 40: 오프핸드
        return (slot >= 36 && slot <= 40) || slot == -1;
    }
}
