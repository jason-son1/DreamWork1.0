package com.dreamwork.core.gui.provider;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.InventoryProvider;
import com.dreamwork.core.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 합금 제련 GUI
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class AlloyCraftingProvider extends InventoryProvider {

    private final DreamWorkCore plugin;

    private static final int SLOT_INPUT_1 = 10;
    private static final int SLOT_INPUT_2 = 11;
    private static final int SLOT_INPUT_3 = 12;
    private static final int SLOT_BUTTON = 15;
    private static final int SLOT_RESULT = 16;

    // 레시피: 철10 + 금5 + 드림스톤1
    private static final int AMOUNT_IRON = 10;
    private static final int AMOUNT_GOLD = 5;
    private static final int AMOUNT_STONE = 1;

    public AlloyCraftingProvider(Player player, DreamWorkCore plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override
    public void init(Inventory inv) {
        // 배경 설정
        ItemStack bg = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < inv.getSize(); i++) {
            if (i == SLOT_INPUT_1 || i == SLOT_INPUT_2 || i == SLOT_INPUT_3 || i == SLOT_RESULT)
                continue;
            inv.setItem(i, bg);
        }

        // 버튼 초기 상태
        updateButton(inv, false);
    }

    @Override
    public boolean isInteractive() {
        return true;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        Inventory inv = event.getInventory();
        Inventory clickedInv = event.getClickedInventory();

        // GUI 내부 클릭
        if (clickedInv != null && clickedInv.equals(inv)) {
            // 입력 슬롯과 결과 슬롯은 상호작용 허용
            if (slot == SLOT_INPUT_1 || slot == SLOT_INPUT_2 || slot == SLOT_INPUT_3) {
                event.setCancelled(false);
                plugin.getServer().getScheduler().runTask(plugin, () -> checkRecipe(inv));
                return;
            }

            if (slot == SLOT_RESULT) {
                event.setCancelled(false); // 결과물 가져가기 허용
                return;
            }

            // 제작 버튼 클릭
            if (slot == SLOT_BUTTON) {
                event.setCancelled(true);
                craft(inv);
                return;
            }

            // 그 외 (배경 등) 클릭 금지
            event.setCancelled(true);
        } else {
            // 플레이어 인벤토리 클릭: 허용
            event.setCancelled(false);
            // Shift+Click 등 인벤토리 변경 감지
            plugin.getServer().getScheduler().runTask(plugin, () -> checkRecipe(inv));
        }
    }

    private void checkRecipe(Inventory inv) {
        boolean valid = isRecipeValid(inv);
        updateButton(inv, valid);
    }

    private boolean isRecipeValid(Inventory inv) {
        ItemStack i1 = inv.getItem(SLOT_INPUT_1);
        ItemStack i2 = inv.getItem(SLOT_INPUT_2);
        ItemStack i3 = inv.getItem(SLOT_INPUT_3); // 드림스톤

        return isValidMaterial(i1, Material.IRON_INGOT, AMOUNT_IRON) &&
                isValidMaterial(i2, Material.GOLD_INGOT, AMOUNT_GOLD) &&
                isValidDreamStone(i3, AMOUNT_STONE);
    }

    private void updateButton(Inventory inv, boolean craftable) {
        if (craftable) {
            inv.setItem(SLOT_BUTTON, ItemBuilder.of(Material.ANVIL)
                    .name("§a§l[ 제작하기 ]")
                    .lore("§7클릭하여 합금을 제작합니다.")
                    .build());
        } else {
            inv.setItem(SLOT_BUTTON, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE)
                    .name("§c§l[ 재료 부족 ]")
                    .lore("§f필요 재료:")
                    .lore("§7- 철 주괴 10개")
                    .lore("§7- 금 주괴 5개")
                    .lore("§7- 드림 스톤 1개")
                    .build());
        }
    }

    private void craft(Inventory inv) {
        if (!isRecipeValid(inv)) {
            player.sendMessage("§c재료가 부족합니다.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 0.5f);
            return;
        }

        // 결과 슬롯 확인
        ItemStack currentResult = inv.getItem(SLOT_RESULT);
        if (currentResult != null && currentResult.getType() != Material.AIR) {
            player.sendMessage("§c결과 슬롯을 비워주세요.");
            return;
        }

        // 재료 소모
        consumeItem(inv, SLOT_INPUT_1, AMOUNT_IRON);
        consumeItem(inv, SLOT_INPUT_2, AMOUNT_GOLD);
        consumeItem(inv, SLOT_INPUT_3, AMOUNT_STONE);

        // 결과 지급
        ItemStack result = plugin.getItemFactory().createItem("reinforced_alloy");
        inv.setItem(SLOT_RESULT, result);

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);
        player.sendMessage("§a강화된 합금을 제작했습니다!");

        // 버튼 갱신
        checkRecipe(inv);
    }

    private void consumeItem(Inventory inv, int slot, int amount) {
        ItemStack item = inv.getItem(slot);
        if (item == null)
            return;

        item.setAmount(item.getAmount() - amount);
        inv.setItem(slot, item);
    }

    private boolean isValidMaterial(ItemStack item, Material material, int minAmount) {
        return item != null && item.getType() == material && item.getAmount() >= minAmount;
    }

    private boolean isValidDreamStone(ItemStack item, int minAmount) {
        if (item == null || item.getAmount() < minAmount)
            return false;

        String itemId = plugin.getItemFactory().getItemId(item);
        return "dream_stone".equals(itemId);
    }

    @Override
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        // 아이템 돌려주기
        returnItem(event.getInventory(), SLOT_INPUT_1);
        returnItem(event.getInventory(), SLOT_INPUT_2);
        returnItem(event.getInventory(), SLOT_INPUT_3);
        returnItem(event.getInventory(), SLOT_RESULT);
    }

    private void returnItem(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item != null && item.getType() != Material.AIR) {
            player.getInventory().addItem(item).values().forEach(
                    leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }
}
