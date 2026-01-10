package com.dreamwork.core.gui;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.JobProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 직업 선택 UI
 */
public class JobSelectionUI extends InventoryManager {

    private final JobManager jobManager;

    public JobSelectionUI(DreamWorkCore plugin, Player player) {
        super(plugin, player, 27, "직업 선택");
        this.jobManager = plugin.getJobManager();
    }

    @Override
    protected void setup() {
        inventory.clear();

        int slot = 0;
        for (JobProvider job : jobManager.getJobs().values()) {
            if (slot >= 27)
                break;

            ItemStack icon = createJobIcon(job);
            inventory.setItem(slot++, icon);
        }
    }

    private ItemStack createJobIcon(JobProvider job) {
        Material material = Material.matchMaterial(job.getIcon());
        if (material == null)
            material = Material.PAPER;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§6" + job.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7" + job.getDescription()));
        lore.add(Component.text(""));
        lore.add(Component.text("§e클릭하여 선택"));

        meta.lore(lore);

        // 직업 ID를 PDC 등에 저장하면 좋겠지만 간단하게 DisplayName 등으로 식별하거나
        // 슬롯 인덱스와 리스트 순서를 매칭할 수도 있음.
        // 여기서는 편의상 ItemMeta에 숨겨진 데이터를 넣지 않고,
        // onClick에서 slot으로 다시 찾거나(순서 보장 시) 이름을 비교해야 함.
        // 하지만 이름은 중복 가능성이 있으므로, PersistentDataContainer를 쓰는게 정석.
        // 일단은 JobProvider 객체 자체를 슬롯과 매핑하는 맵을 따로 두는 게 나을 수도 있지만,
        // 여기서는 간단히 Material로 구분하거나(위험), 순서대로 배치했으니 순서대로 찾음.

        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot < 0 || slot >= jobManager.getJobs().size())
            return;

        // 순서대로 찾기 (Map.values() 순서는 보장되지 않을 수 있으므로 setup에서 리스트로 변환 필요)
        // 안전하게 다시 values()를 리스트로 변환하여 접근
        List<JobProvider> jobs = new ArrayList<>(jobManager.getJobs().values());
        if (slot >= jobs.size())
            return;

        JobProvider selectedJob = jobs.get(slot);

        // 직업 변경 시도
        if (jobManager.setUserJob(player.getUniqueId(), selectedJob.getId())) {
            player.sendMessage("§a[DreamWork] " + selectedJob.getDisplayName() + " 직업으로 전직했습니다!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            player.closeInventory();
        } else {
            player.sendMessage("§c직업 변경에 실패했습니다.");
        }
    }
}
