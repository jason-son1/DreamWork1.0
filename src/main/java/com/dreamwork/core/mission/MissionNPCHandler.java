package com.dreamwork.core.mission;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.InventoryProvider;
import com.dreamwork.core.gui.SmartInventory;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 미션 NPC 상호작용 핸들러
 * 
 * <p>
 * Plan 2.0 기준:
 * - 미션 NPC와 상호작용하여 미션 GUI 열기
 * - 미션 진행도 확인 및 보상 수령
 * - 승급 신청 기능
 * </p>
 */
public class MissionNPCHandler implements Listener {

    private final DreamWorkCore plugin;

    public MissionNPCHandler(DreamWorkCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * NPC 상호작용 이벤트 처리
     * (Citizens 플러그인 없이도 동작 가능한 기본 구현)
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Citizens NPC 체크 (Citizens 플러그인이 있을 경우 확장 가능)
        // 여기서는 기본적으로 /mission 명령어를 통해 GUI 열기로 대체
    }

    /**
     * 미션 GUI 열기
     */
    public static void openMissionGUI(Player player, DreamWorkCore plugin) {
        SmartInventory.builder()
                .title("§6§l미션 게시판")
                .size(54)
                .provider(new MissionGUIProvider(player, plugin))
                .build()
                .open(player);
    }

    /**
     * 미션 GUI Provider
     */
    public static class MissionGUIProvider extends InventoryProvider {

        private final DreamWorkCore plugin;
        private int currentPage = 0;

        public MissionGUIProvider(Player player, DreamWorkCore plugin) {
            super(player);
            this.plugin = plugin;
        }

        @Override
        public void init(Inventory inventory) {
            renderBackground(inventory);
            renderMissions(inventory);
            renderNavigation(inventory);
        }

        private void renderBackground(Inventory inventory) {
            for (int i = 0; i < 54; i++) {
                inventory.setItem(i, ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                        .name("§8")
                        .build());
            }
        }

        private void renderMissions(Inventory inventory) {
            UUID uuid = player.getUniqueId();
            MissionManager missionManager = plugin.getMissionManager();

            if (missionManager == null) {
                inventory.setItem(22, ItemBuilder.of(Material.BARRIER)
                        .name("§c미션 시스템 비활성화")
                        .build());
                return;
            }

            String playerJob = getPlayerJob(uuid);
            int playerLevel = getPlayerLevel(uuid);

            // 현재 직업의 미션 목록
            List<MissionManager.MissionDefinition> activeMissions = missionManager.getActiveMissions(uuid);

            // 플레이어 정보 표시
            inventory.setItem(4, ItemBuilder.of(getMaterialForJob(playerJob))
                    .name("§6" + getJobDisplayName(playerJob) + " §7Lv." + playerLevel)
                    .lore("")
                    .lore("§7현재 직업의 승급 미션을 확인하세요.")
                    .lore("§7미션을 완료하면 새로운 등급으로 승급할 수 있습니다.")
                    .build());

            if (activeMissions.isEmpty()) {
                inventory.setItem(22, ItemBuilder.of(Material.BOOK)
                        .name("§7진행 중인 미션이 없습니다")
                        .lore("")
                        .lore("§8모든 승급 미션을 완료했거나")
                        .lore("§8직업이 선택되지 않았습니다.")
                        .build());
            } else {
                int slot = 19;
                for (MissionManager.MissionDefinition mission : activeMissions) {
                    if (slot > 43)
                        break;

                    int progress = missionManager.getProgress(uuid, mission.id);
                    boolean completed = progress >= mission.goalAmount;
                    boolean canStart = playerLevel >= mission.requiredLevel - 10;

                    Material mat = completed ? Material.LIME_DYE : (canStart ? Material.YELLOW_DYE : Material.RED_DYE);

                    String status = completed ? "§a[완료]" : (canStart ? "§e[진행 중]" : "§c[잠금]");

                    inventory.setItem(slot, ItemBuilder.of(mat)
                            .name(status + " §f" + mission.displayName)
                            .lore("")
                            .lore("§7진행도: §f" + progress + " / " + mission.goalAmount)
                            .lore("§7필요 레벨: §f" + mission.requiredLevel)
                            .lore("")
                            .lore(completed ? "§a[클릭] 보상 수령" : (canStart ? "§7미션을 진행하세요!" : "§c레벨이 부족합니다"))
                            .build());

                    slot++;
                    if (slot == 26)
                        slot = 28;
                    if (slot == 35)
                        slot = 37;
                }
            }

            // 승급 버튼
            boolean canRankUp = checkCanRankUp(uuid);
            inventory.setItem(49, ItemBuilder.of(canRankUp ? Material.NETHER_STAR : Material.BARRIER)
                    .name(canRankUp ? "§a§l승급하기" : "§7승급 불가")
                    .lore("")
                    .lore(canRankUp ? "§a승급 조건을 충족했습니다!" : "§c승급 미션을 완료하세요.")
                    .lore("")
                    .lore(canRankUp ? "§e[클릭] 승급 신청" : "")
                    .build());
        }

        private void renderNavigation(Inventory inventory) {
            inventory.setItem(45, ItemBuilder.of(Material.ARROW)
                    .name("§f이전 페이지")
                    .build());
            inventory.setItem(53, ItemBuilder.of(Material.ARROW)
                    .name("§f다음 페이지")
                    .build());
            inventory.setItem(8, ItemBuilder.of(Material.BARRIER)
                    .name("§c닫기")
                    .build());
        }

        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);

            int slot = event.getSlot();

            if (slot == 8) {
                player.closeInventory();
            } else if (slot == 49) {
                // 승급 시도
                if (checkCanRankUp(player.getUniqueId())) {
                    performRankUp(player);
                    refresh();
                }
            }
        }

        /**
         * 승급 가능 여부 확인
         */
        private boolean checkCanRankUp(UUID uuid) {
            MissionManager missionManager = plugin.getMissionManager();
            if (missionManager == null)
                return false;

            String job = getPlayerJob(uuid);
            int level = getPlayerLevel(uuid);

            // 레벨별 승급 미션 완료 여부 확인
            String missionId = job + "_rank_" + getNextRankLevel(level);
            return missionManager.isMissionCompleted(uuid, missionId);
        }

        /**
         * 승급 수행
         */
        private void performRankUp(Player player) {
            RankTitleSystem rankSystem = plugin.getRankTitleSystem();
            if (rankSystem != null) {
                rankSystem.promotePlayer(player);
            }

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            player.sendTitle("§a§l승급 완료!", "§f새로운 등급이 되었습니다!", 10, 40, 10);
        }

        private int getNextRankLevel(int currentLevel) {
            if (currentLevel < 10)
                return 10;
            if (currentLevel < 30)
                return 30;
            if (currentLevel < 50)
                return 50;
            return 100;
        }

        private String getPlayerJob(UUID uuid) {
            JobManager jobManager = plugin.getJobManager();
            if (jobManager == null)
                return "";
            UserJobData jobData = jobManager.getUserJob(uuid);
            return jobData.hasJob() ? jobData.getJobId() : "";
        }

        private int getPlayerLevel(UUID uuid) {
            JobManager jobManager = plugin.getJobManager();
            if (jobManager == null)
                return 0;
            UserJobData jobData = jobManager.getUserJob(uuid);
            return jobData.hasJob() ? jobData.getLevel() : 0;
        }

        private Material getMaterialForJob(String job) {
            return switch (job) {
                case "miner" -> Material.IRON_PICKAXE;
                case "farmer" -> Material.IRON_HOE;
                case "fisher" -> Material.FISHING_ROD;
                case "explorer" -> Material.COMPASS;
                case "hunter" -> Material.IRON_SWORD;
                default -> Material.BOOK;
            };
        }

        private String getJobDisplayName(String job) {
            return switch (job) {
                case "miner" -> "광부";
                case "farmer" -> "농부";
                case "fisher" -> "어부";
                case "explorer" -> "탐험가";
                case "hunter" -> "사냥꾼";
                default -> "없음";
            };
        }
    }
}
