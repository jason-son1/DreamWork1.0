package com.dreamwork.core.quest;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.InventoryProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 퀘스트 UI Provider
 */
public class QuestUI extends InventoryProvider {

    private final DreamWorkCore plugin;
    private final QuestManager questManager;

    public QuestUI(Player player, DreamWorkCore plugin) {
        super(player);
        this.plugin = plugin;
        this.questManager = plugin.getQuestManager();
    }

    @Override
    public void init(Inventory inv) {
        Map<String, QuestProgress> progresses = questManager.getPlayerProgress(player.getUniqueId());

        // 빈 상태 메시지
        if (progresses.isEmpty()) {
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = emptyItem.getItemMeta();
            meta.displayName(Component.text("§c진행 중인 퀘스트가 없습니다"));
            meta.lore(List.of(
                    Component.text("§7내일 새로운 퀘스트가 할당됩니다!")));
            emptyItem.setItemMeta(meta);
            inv.setItem(13, emptyItem);
            return;
        }

        int slot = 0;
        for (Map.Entry<String, QuestProgress> entry : progresses.entrySet()) {
            if (slot >= 27)
                break;

            QuestProgress progress = entry.getValue();
            Quest quest = questManager.getQuest(progress.getQuestId());
            if (quest == null)
                continue;

            ItemStack icon = createQuestIcon(quest, progress);
            inv.setItem(slot++, icon);
        }
    }

    private ItemStack createQuestIcon(Quest quest, QuestProgress progress) {
        Material material;
        String statusPrefix;

        switch (progress.getStatus()) {
            case COMPLETED -> {
                material = Material.LIME_DYE;
                statusPrefix = "§a✔ ";
            }
            case REWARDED -> {
                material = Material.GRAY_DYE;
                statusPrefix = "§8✔ ";
            }
            default -> {
                material = Material.YELLOW_DYE;
                statusPrefix = "§e• ";
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(statusPrefix + quest.getName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7" + quest.getDescription()));
        lore.add(Component.text(""));

        // 진행도 표시
        int current = progress.getCurrentProgress();
        int required = quest.getRequirement() != null ? quest.getRequirement().getAmount() : 1;
        double percent = (double) current / required * 100;

        String progressBar = createProgressBar(percent);
        lore.add(Component.text("§f진행: " + progressBar + " §f" + current + "/" + required));

        // 보상 표시
        if (quest.getReward() != null) {
            lore.add(Component.text(""));
            lore.add(Component.text("§6§l[ 보상 ]"));
            if (quest.getReward().getExp() > 0) {
                lore.add(Component.text("§e• 경험치: " + (int) quest.getReward().getExp()));
            }
            for (String itemStr : quest.getReward().getItems()) {
                lore.add(Component.text("§e• " + itemStr));
            }
        }

        // 클릭 안내
        if (progress.getStatus() == QuestProgress.QuestStatus.COMPLETED) {
            lore.add(Component.text(""));
            lore.add(Component.text("§a[클릭하여 보상 수령]"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String createProgressBar(double percent) {
        int filled = (int) (percent / 10);
        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("■");
            } else {
                bar.append("§7□");
            }
        }
        return bar.toString();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        Map<String, QuestProgress> progresses = questManager.getPlayerProgress(player.getUniqueId());
        List<String> questIds = new ArrayList<>(progresses.keySet());

        if (slot < 0 || slot >= questIds.size())
            return;

        String questId = questIds.get(slot);
        QuestProgress progress = progresses.get(questId);

        if (progress.getStatus() == QuestProgress.QuestStatus.COMPLETED) {
            // 보상 수령
            if (questManager.giveReward(player, questId)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                refresh();
            }
        } else {
            player.sendMessage("§c아직 퀘스트를 완료하지 않았습니다.");
        }
    }
}
