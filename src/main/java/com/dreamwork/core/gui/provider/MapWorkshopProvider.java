package com.dreamwork.core.gui.provider;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.InventoryProvider;
import com.dreamwork.core.gui.SmartInventory;
import com.dreamwork.core.item.ItemBuilder;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 지도 제작소 (Map Workshop) GUI Provider
 * 
 * <p>
 * Plan 2.0 기준:
 * - 좌표 스크롤 거래 게시판
 * - 바이옴 캡슐 판매
 * - 탐험 보고서 납품
 * </p>
 */
public class MapWorkshopProvider extends InventoryProvider {

    private final DreamWorkCore plugin;

    /** 현재 모드 (0: 좌표 스크롤, 1: 바이옴 캡슐, 2: 탐험 보고서) */
    private int currentMode = 0;

    /** 거래 게시판 - 좌표 스크롤 목록 (임시 저장소) */
    private static final Map<UUID, List<CoordinateScrollListing>> SCROLL_LISTINGS = new ConcurrentHashMap<>();

    // PDC 키
    private final NamespacedKey scrollKey;
    private final NamespacedKey biomeCapKey;

    public MapWorkshopProvider(Player player, DreamWorkCore plugin) {
        super(player);
        this.plugin = plugin;
        this.scrollKey = new NamespacedKey(plugin, "coord_scroll");
        this.biomeCapKey = new NamespacedKey(plugin, "biome_capsule");
    }

    @Override
    public void init(Inventory inventory) {
        renderBackground(inventory);
        renderModeButtons(inventory);
        renderContent(inventory);
    }

    /**
     * 배경 렌더링
     */
    private void renderBackground(Inventory inventory) {
        ItemStack glass = ItemBuilder.of(Material.BROWN_STAINED_GLASS_PANE)
                .name("§8")
                .build();

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }
    }

    /**
     * 모드 버튼 렌더링
     */
    private void renderModeButtons(Inventory inventory) {
        // 좌표 스크롤 모드
        ItemStack scrollBtn = ItemBuilder.of(currentMode == 0 ? Material.FILLED_MAP : Material.MAP)
                .name(currentMode == 0 ? "§6§l[좌표 스크롤]" : "§7좌표 스크롤")
                .lore("§7좌표 스크롤을 사고팝니다")
                .build();
        inventory.setItem(0, scrollBtn);

        // 바이옴 캡슐 모드
        ItemStack biomeBtn = ItemBuilder.of(currentMode == 1 ? Material.ENDER_EYE : Material.ENDER_PEARL)
                .name(currentMode == 1 ? "§a§l[바이옴 캡슐]" : "§7바이옴 캡슐")
                .lore("§7바이옴 색상을 담은 캡슐")
                .build();
        inventory.setItem(1, biomeBtn);

        // 탐험 보고서 모드
        ItemStack reportBtn = ItemBuilder.of(currentMode == 2 ? Material.WRITTEN_BOOK : Material.BOOK)
                .name(currentMode == 2 ? "§e§l[탐험 보고서]" : "§7탐험 보고서")
                .lore("§7발견한 구조물/유적 보고서")
                .build();
        inventory.setItem(2, reportBtn);

        // 닫기 버튼
        ItemStack closeBtn = ItemBuilder.of(Material.BARRIER)
                .name("§c닫기")
                .build();
        inventory.setItem(8, closeBtn);
    }

    /**
     * 콘텐츠 렌더링
     */
    private void renderContent(Inventory inventory) {
        switch (currentMode) {
            case 0 -> renderScrollContent(inventory);
            case 1 -> renderBiomeCapsuleContent(inventory);
            case 2 -> renderReportContent(inventory);
        }
    }

    /**
     * 좌표 스크롤 거래 게시판 렌더링
     */
    private void renderScrollContent(Inventory inventory) {
        // 내 스크롤 등록 버튼
        inventory.setItem(4, ItemBuilder.of(Material.EMERALD)
                .name("§a스크롤 등록하기")
                .lore("")
                .lore("§7보유한 좌표 스크롤을 거래 게시판에 등록합니다")
                .lore("")
                .lore("§e[클릭] 등록 메뉴")
                .build());

        // 거래 목록 표시
        int slot = 19;
        List<CoordinateScrollListing> allListings = getAllListings();

        if (allListings.isEmpty()) {
            inventory.setItem(22, ItemBuilder.of(Material.GRAY_DYE)
                    .name("§7등록된 스크롤이 없습니다")
                    .lore("")
                    .lore("§8아직 아무도 좌표 스크롤을 등록하지 않았습니다.")
                    .build());
        } else {
            for (CoordinateScrollListing listing : allListings) {
                if (slot > 43)
                    break;

                boolean isOwn = listing.sellerUUID.equals(player.getUniqueId());

                ItemStack item = ItemBuilder.of(Material.FILLED_MAP)
                        .name("§6" + listing.title)
                        .lore("")
                        .lore("§7바이옴: §f" + listing.biome)
                        .lore("§7설명: §f" + listing.description)
                        .lore("")
                        .lore("§7판매자: §f" + listing.sellerName)
                        .lore("§7가격: §e" + listing.price + " Dream")
                        .lore("")
                        .lore(isOwn ? "§c[클릭] 등록 취소" : "§a[클릭] 구매하기")
                        .build();

                inventory.setItem(slot, item);

                slot++;
                if (slot == 26)
                    slot = 28;
                if (slot == 35)
                    slot = 37;
            }
        }
    }

    /**
     * 바이옴 캡슐 콘텐츠 렌더링
     */
    private void renderBiomeCapsuleContent(Inventory inventory) {
        int explorerLevel = getExplorerLevel();

        // 바이옴 캡슐 목록
        Object[][] capsules = {
                { "PLAINS", "평원 캡슐", Material.LIME_DYE, 100, 10 },
                { "FOREST", "숲 캡슐", Material.GREEN_DYE, 150, 20 },
                { "DESERT", "사막 캡슐", Material.YELLOW_DYE, 200, 25 },
                { "JUNGLE", "정글 캡슐", Material.ORANGE_DYE, 300, 30 },
                { "BADLANDS", "메사 캡슐", Material.RED_DYE, 500, 40 },
                { "MUSHROOM_FIELDS", "버섯섬 캡슐", Material.PURPLE_DYE, 1000, 50 },
        };

        int slot = 19;
        for (Object[] cap : capsules) {
            String biomeId = (String) cap[0];
            String name = (String) cap[1];
            Material mat = (Material) cap[2];
            int price = (int) cap[3];
            int reqLevel = (int) cap[4];

            boolean canBuy = explorerLevel >= reqLevel;

            ItemStack item = ItemBuilder.of(mat)
                    .name("§a" + name)
                    .lore("")
                    .lore("§7건축 땅에 이 바이옴의 색깔(잔디, 나뭇잎)을")
                    .lore("§7적용할 수 있는 캡슐입니다.")
                    .lore("")
                    .lore("§7가격: §e" + price + " Dream")
                    .lore("§7필요 레벨: §f" + reqLevel)
                    .lore("")
                    .lore(canBuy ? "§a[클릭] 구매하기" : "§c레벨 부족")
                    .build();

            inventory.setItem(slot, item);

            slot++;
            if (slot == 25)
                slot = 28;
        }
    }

    /**
     * 탐험 보고서 콘텐츠 렌더링
     */
    private void renderReportContent(Inventory inventory) {
        // 보고서 납품 안내
        inventory.setItem(22, ItemBuilder.of(Material.WRITTEN_BOOK)
                .name("§e탐험 보고서 납품")
                .lore("")
                .lore("§7새로운 구조물이나 유적을 발견하면")
                .lore("§7보고서를 작성하여 납품할 수 있습니다.")
                .lore("")
                .lore("§a보상:")
                .lore("  §8- §f마을: §e50 Dream")
                .lore("  §8- §f던전: §e100 Dream")
                .lore("  §8- §f네더 요새: §e200 Dream")
                .lore("  §8- §f엔드 시티: §e500 Dream")
                .lore("")
                .lore("§8* 최초 발견자에게만 보상 지급")
                .build());

        // 내 보고서 목록 버튼
        inventory.setItem(31, ItemBuilder.of(Material.BOOK)
                .name("§6내 보고서 목록")
                .lore("")
                .lore("§7지금까지 작성한 보고서를 확인합니다.")
                .lore("")
                .lore("§e[클릭] 목록 보기")
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getSlot();

        // 모드 버튼
        if (slot == 0) {
            currentMode = 0;
            refresh();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        } else if (slot == 1) {
            currentMode = 1;
            refresh();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        } else if (slot == 2) {
            currentMode = 2;
            refresh();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        } else if (slot == 8) {
            player.closeInventory();
        } else if (currentMode == 0 && slot == 4) {
            // 스크롤 등록
            player.sendMessage("§e[지도 제작소] §f좌표 스크롤 등록 기능은 /scroll register 명령어를 사용하세요.");
            player.closeInventory();
        }
    }

    /**
     * 모든 거래 목록 조회
     */
    private List<CoordinateScrollListing> getAllListings() {
        List<CoordinateScrollListing> all = new ArrayList<>();
        SCROLL_LISTINGS.values().forEach(all::addAll);
        return all;
    }

    /**
     * 탐험가 레벨 반환
     */
    private int getExplorerLevel() {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 0;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"explorer".equals(jobData.getJobId()))
            return 0;
        return jobData.getLevel();
    }

    /**
     * 좌표 스크롤 아이템 생성
     */
    public static ItemStack createCoordinateScroll(DreamWorkCore plugin, String title, String biome,
            int x, int y, int z, String world) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§6좌표 스크롤: " + title);
        meta.setLore(Arrays.asList(
                "",
                "§7바이옴: §f" + biome,
                "§7좌표: §f" + x + ", " + y + ", " + z,
                "§7월드: §f" + world,
                "",
                "§a[우클릭] 나침반에 좌표 저장"));

        // PDC 저장
        NamespacedKey key = new NamespacedKey(plugin, "coord_scroll");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING,
                x + "," + y + "," + z + "," + world);

        meta.setCustomModelData(40001);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * 바이옴 캡슐 아이템 생성
     */
    public static ItemStack createBiomeCapsule(DreamWorkCore plugin, String biomeId, String biomeName,
            Material dyeMat) {
        ItemStack item = new ItemStack(dyeMat);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§a바이옴 캡슐: " + biomeName);
        meta.setLore(Arrays.asList(
                "",
                "§7이 캡슐을 건축 땅에 사용하면",
                "§7" + biomeName + " 바이옴의 색상이 적용됩니다.",
                "",
                "§a[우클릭] 바이옴 적용"));

        // PDC 저장
        NamespacedKey key = new NamespacedKey(plugin, "biome_capsule");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, biomeId);

        meta.setCustomModelData(40010);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * GUI 열기
     */
    public static void open(Player player, DreamWorkCore plugin) {
        SmartInventory.builder()
                .title("§6§l지도 제작소")
                .size(54)
                .provider(new MapWorkshopProvider(player, plugin))
                .build()
                .open(player);
    }

    /**
     * 좌표 스크롤 거래 등록 데이터 클래스
     */
    public static class CoordinateScrollListing {
        public final UUID sellerUUID;
        public final String sellerName;
        public final String title;
        public final String biome;
        public final String description;
        public final int price;
        public final int x, y, z;
        public final String world;

        public CoordinateScrollListing(UUID sellerUUID, String sellerName, String title,
                String biome, String description, int price,
                int x, int y, int z, String world) {
            this.sellerUUID = sellerUUID;
            this.sellerName = sellerName;
            this.title = title;
            this.biome = biome;
            this.description = description;
            this.price = price;
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
        }
    }
}
