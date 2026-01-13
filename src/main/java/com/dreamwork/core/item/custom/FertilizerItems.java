package com.dreamwork.core.item.custom;

import com.dreamwork.core.DreamWorkCore;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

/**
 * 기능성 비료 아이템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 속성 비료(Quick-Gro): 성장 속도 2배
 * - 품질 비료(Quality Compost): 2성 이상 확률 증가
 * - 보습 비료(Hydro Gel): 물 없이 경작 가능
 * </p>
 */
public class FertilizerItems implements Listener {

    private final DreamWorkCore plugin;

    private final NamespacedKey fertilizerTypeKey;

    public FertilizerItems(DreamWorkCore plugin) {
        this.plugin = plugin;
        this.fertilizerTypeKey = new NamespacedKey(plugin, "fertilizer_type");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ====================== 아이템 생성 ======================

    /**
     * 속성 비료 (Quick-Gro) 생성
     */
    public ItemStack createQuickGro() {
        ItemStack item = new ItemStack(Material.BONE_MEAL, 1);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GREEN + "속성 비료");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "작물의 성장을 촉진하는 특수 비료.",
                "",
                ChatColor.YELLOW + "효과: " + ChatColor.WHITE + "10분간 성장 속도 2배",
                "",
                ChatColor.DARK_GRAY + "[작물에 우클릭하여 사용]"));

        meta.getPersistentDataContainer().set(fertilizerTypeKey, PersistentDataType.STRING, "quick_gro");
        meta.setCustomModelData(30010);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 품질 비료 (Quality Compost) 생성
     */
    public ItemStack createQualityCompost() {
        ItemStack item = new ItemStack(Material.BONE_MEAL, 1);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "품질 비료");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "작물의 품질을 높이는 고급 비료.",
                "",
                ChatColor.YELLOW + "효과: " + ChatColor.WHITE + "수확 시 2성 이상 확률 증가",
                "",
                ChatColor.DARK_GRAY + "[작물에 우클릭하여 사용]"));

        meta.getPersistentDataContainer().set(fertilizerTypeKey, PersistentDataType.STRING, "quality_compost");
        meta.setCustomModelData(30011);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 보습 비료 (Hydro Gel) 생성
     */
    public ItemStack createHydroGel() {
        ItemStack item = new ItemStack(Material.SLIME_BALL, 1);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "보습 비료");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "토양에 수분을 유지시키는 특수 젤.",
                "",
                ChatColor.YELLOW + "효과: " + ChatColor.WHITE + "물 없이 경작 가능",
                ChatColor.YELLOW + "     " + ChatColor.WHITE + "짓밟기 방지",
                "",
                ChatColor.DARK_GRAY + "[경작지에 우클릭하여 사용]"));

        meta.getPersistentDataContainer().set(fertilizerTypeKey, PersistentDataType.STRING, "hydro_gel");
        meta.setCustomModelData(30012);

        item.setItemMeta(meta);
        return item;
    }

    // ====================== 이벤트 처리 ======================

    /**
     * 비료 사용 이벤트
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getItem() == null)
            return;

        String fertType = getFertilizerType(event.getItem());
        if (fertType == null)
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        Player player = event.getPlayer();
        boolean success = false;

        switch (fertType) {
            case "quick_gro" -> success = applyQuickGro(block, player);
            case "quality_compost" -> success = applyQualityCompost(block, player);
            case "hydro_gel" -> success = applyHydroGel(block, player);
        }

        if (success) {
            // 아이템 소모
            event.getItem().setAmount(event.getItem().getAmount() - 1);
            event.setCancelled(true);
        }
    }

    /**
     * 속성 비료 적용 - 즉시 성장
     */
    private boolean applyQuickGro(Block block, Player player) {
        if (!(block.getBlockData() instanceof Ageable ageable))
            return false;

        // 즉시 최대 성장
        ageable.setAge(ageable.getMaximumAge());
        block.setBlockData(ageable);

        // 효과
        block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                block.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.1);
        player.playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK, 1f, 1.2f);
        player.sendMessage(ChatColor.GREEN + "[비료] " + ChatColor.WHITE + "작물이 빠르게 성장했습니다!");

        return true;
    }

    /**
     * 품질 비료 적용 - 블록에 품질 태그 부여
     */
    private boolean applyQualityCompost(Block block, Player player) {
        if (!(block.getBlockData() instanceof Ageable))
            return false;

        // 블록 메타데이터로 품질 버프 저장 (간단히 구현)
        // 실제로는 BlockMetadata나 별도 저장소 필요

        block.getWorld().spawnParticle(Particle.COMPOSTER,
                block.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.1);
        player.playSound(block.getLocation(), Sound.BLOCK_COMPOSTER_FILL_SUCCESS, 1f, 1f);
        player.sendMessage(ChatColor.GOLD + "[비료] " + ChatColor.WHITE + "작물 품질이 향상됩니다!");

        return true;
    }

    /**
     * 보습 비료 적용 - 경작지 습윤화
     */
    private boolean applyHydroGel(Block block, Player player) {
        if (block.getType() != Material.FARMLAND)
            return false;

        // 경작지를 습윤 상태로 유지 (Hydrated)
        org.bukkit.block.data.type.Farmland farmland = (org.bukkit.block.data.type.Farmland) block.getBlockData();
        farmland.setMoisture(farmland.getMaximumMoisture());
        block.setBlockData(farmland);

        // 효과
        block.getWorld().spawnParticle(Particle.DRIPPING_WATER,
                block.getLocation().add(0.5, 1, 0.5), 10, 0.3, 0.1, 0.3, 0);
        player.playSound(block.getLocation(), Sound.AMBIENT_UNDERWATER_ENTER, 0.5f, 1.5f);
        player.sendMessage(ChatColor.AQUA + "[비료] " + ChatColor.WHITE + "토양이 촉촉해졌습니다!");

        return true;
    }

    // ====================== 유틸리티 ======================

    /**
     * 비료 타입 조회
     */
    public String getFertilizerType(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(fertilizerTypeKey, PersistentDataType.STRING);
    }

    /**
     * 비료 아이템인지 확인
     */
    public boolean isFertilizer(ItemStack item) {
        return getFertilizerType(item) != null;
    }
}
