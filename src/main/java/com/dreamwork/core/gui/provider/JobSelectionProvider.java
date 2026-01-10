package com.dreamwork.core.gui.provider;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.InventoryProvider;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.JobProvider;
import com.dreamwork.core.job.UserJobData;
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

/**
 * 직업 선택 UI Provider
 */
public class JobSelectionProvider extends InventoryProvider {

    private final DreamWorkCore plugin;
    private final JobManager jobManager;
    private final List<JobProvider> jobList;

    public JobSelectionProvider(Player player, DreamWorkCore plugin) {
        super(player);
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.jobList = new ArrayList<>(jobManager.getJobs().values());
    }

    @Override
    public void init(Inventory inv) {
        UserJobData currentJob = jobManager.getUserJob(player.getUniqueId());

        for (int i = 0; i < jobList.size() && i < 27; i++) {
            JobProvider job = jobList.get(i);
            ItemStack icon = createJobIcon(job, currentJob);
            inv.setItem(i, icon);
        }
    }

    private ItemStack createJobIcon(JobProvider job, UserJobData currentJob) {
        Material material = Material.matchMaterial(job.getIcon());
        if (material == null)
            material = Material.PAPER;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        boolean isCurrentJob = currentJob.hasJob() && currentJob.getJobId().equals(job.getId());

        // 타이틀
        String prefix = isCurrentJob ? "§a✔ " : "§6";
        meta.displayName(Component.text(prefix + job.getDisplayName()));

        // Lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7" + job.getDescription()));
        lore.add(Component.text(""));
        lore.add(Component.text("§e최대 레벨: §f" + job.getMaxLevel()));

        if (isCurrentJob) {
            lore.add(Component.text(""));
            lore.add(Component.text("§aLv." + currentJob.getLevel() + " §7(" +
                    String.format("%.0f", currentJob.getCurrentExp()) + " EXP)"));
            lore.add(Component.text(""));
            lore.add(Component.text("§a현재 직업입니다"));
        } else {
            lore.add(Component.text(""));
            lore.add(Component.text("§e클릭하여 전직"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot < 0 || slot >= jobList.size())
            return;

        JobProvider selectedJob = jobList.get(slot);

        // 현재 직업이면 무시
        UserJobData currentJob = jobManager.getUserJob(player.getUniqueId());
        if (currentJob.hasJob() && currentJob.getJobId().equals(selectedJob.getId())) {
            player.sendMessage("§c이미 해당 직업입니다.");
            return;
        }

        // 직업 변경
        if (jobManager.setUserJob(player.getUniqueId(), selectedJob.getId())) {
            player.sendMessage("§a[DreamWork] §f" + selectedJob.getDisplayName() + "§a 직업으로 전직했습니다!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            player.closeInventory();
        } else {
            player.sendMessage("§c직업 변경에 실패했습니다.");
        }
    }
}
