package com.dreamwork.core.job.system;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.*;

/**
 * 어종 지식 (Fish Knowledge) 패시브 시스템
 * 
 * <p>
 * Plan 2.0 기준:
 * - 어부 Lv.30 해금
 * - 낚시 시작 시 해당 바이옴에서 잡을 수 있는 물고기 정보 표시
 * - 레벨에 따라 정보량 증가
 * </p>
 */
public class FishKnowledgeSystem implements Listener {

    private final DreamWorkCore plugin;

    /** 바이옴별 어종 정보 */
    private static final Map<String, List<FishInfo>> BIOME_FISH = new HashMap<>();

    static {
        // 따뜻한 바다
        BIOME_FISH.put("WARM", Arrays.asList(
                new FishInfo("열대어", 40, 5.0, 15),
                new FishInfo("복어", 30, 5.0, 25),
                new FishInfo("참치(커스텀)", 20, 20.0, 120),
                new FishInfo("대구", 10, 0.5, 50)));

        // 차가운 바다
        BIOME_FISH.put("COLD", Arrays.asList(
                new FishInfo("연어", 50, 2.0, 70),
                new FishInfo("대구", 40, 0.5, 50),
                new FishInfo("얼음고기(커스텀)", 10, 10.0, 60)));

        // 일반 바다
        BIOME_FISH.put("NORMAL", Arrays.asList(
                new FishInfo("대구", 50, 0.5, 50),
                new FishInfo("연어", 30, 2.0, 70),
                new FishInfo("열대어", 15, 5.0, 15),
                new FishInfo("복어", 5, 5.0, 25)));

        // 강
        BIOME_FISH.put("RIVER", Arrays.asList(
                new FishInfo("연어", 60, 2.0, 70),
                new FishInfo("대구", 40, 0.5, 50)));

        // 심해
        BIOME_FISH.put("DEEP", Arrays.asList(
                new FishInfo("발광 오징어", 20, 15.0, 100),
                new FishInfo("아귀(커스텀)", 15, 35.0, 80),
                new FishInfo("대구", 40, 0.5, 50),
                new FishInfo("연어", 25, 2.0, 70)));
    }

    public FishKnowledgeSystem(DreamWorkCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 낚시 시작 시 바이옴 물고기 정보 표시
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        // 낚시 시작 시에만
        if (event.getState() != PlayerFishEvent.State.FISHING)
            return;

        Player player = event.getPlayer();
        int level = getFisherLevel(player);

        if (level < 30)
            return;

        int infoLevel = getInfoLevel(level);
        String biomeCategory = getBiomeCategory(player.getLocation().getBlock().getBiome());
        List<FishInfo> fishList = BIOME_FISH.getOrDefault(biomeCategory, Collections.emptyList());

        if (fishList.isEmpty())
            return;

        // 정보 표시
        displayFishInfo(player, fishList, infoLevel);
    }

    /**
     * 물고기 정보를 플레이어에게 표시합니다.
     */
    private void displayFishInfo(Player player, List<FishInfo> fishList, int infoLevel) {
        player.sendMessage("§b═══ 이 지역의 물고기 ═══");

        for (FishInfo fish : fishList) {
            StringBuilder msg = new StringBuilder();
            msg.append("§3◆ §f").append(fish.name);

            if (infoLevel >= 2) {
                msg.append(" §7- §e").append(fish.basePrice).append(" Dream");
            }

            if (infoLevel >= 3) {
                msg.append(" §7(월척: §6").append(fish.crownSize).append("cm+§7)");
            }

            if (infoLevel >= 4) {
                msg.append(" §8[").append(fish.weight).append("%]");
            }

            player.sendMessage(msg.toString());
        }

        player.sendMessage("§b═════════════════════");
    }

    /**
     * 바이옴을 카테고리로 분류합니다.
     */
    private String getBiomeCategory(Biome biome) {
        String name = biome.name();

        if (name.contains("WARM") || name.contains("LUKEWARM")) {
            return "WARM";
        }
        if (name.contains("COLD") || name.contains("FROZEN")) {
            return "COLD";
        }
        if (name.contains("DEEP")) {
            return "DEEP";
        }
        if (name.contains("RIVER")) {
            return "RIVER";
        }
        if (name.contains("OCEAN")) {
            return "NORMAL";
        }

        // 기본값
        return "NORMAL";
    }

    /**
     * 레벨에 따른 정보 수준을 반환합니다.
     */
    private int getInfoLevel(int level) {
        if (level >= 90)
            return 4;
        if (level >= 70)
            return 3;
        if (level >= 50)
            return 2;
        return 1;
    }

    /**
     * 플레이어의 어부 레벨을 반환합니다.
     */
    private int getFisherLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"fisher".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }

    /**
     * 물고기 정보 클래스
     */
    private static class FishInfo {
        final String name;
        final int weight; // 출현 확률 가중치
        final double basePrice; // 기본 시세
        final int crownSize; // 월척 기준 크기 (cm)

        FishInfo(String name, int weight, double basePrice, int crownSize) {
            this.name = name;
            this.weight = weight;
            this.basePrice = basePrice;
            this.crownSize = crownSize;
        }
    }
}
