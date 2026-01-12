package com.dreamwork.core;

import com.dreamwork.core.hook.HookManager;
import com.dreamwork.core.item.SetEffectManager;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.JobType;
import com.dreamwork.core.listener.*;
import com.dreamwork.core.job.system.MiningComboSystem;
import com.dreamwork.core.item.custom.UnidentifiedOreItem;
import com.dreamwork.core.manager.Manager;
import com.dreamwork.core.quest.QuestManager;
import com.dreamwork.core.stat.InventoryScanner;
import com.dreamwork.core.stat.StatManager;
import com.dreamwork.core.database.StorageManager;
import com.dreamwork.core.database.AutoSaveScheduler;

import com.dreamwork.core.gui.SmartInventory;
import com.dreamwork.core.gui.provider.JobSelectionProvider;
import com.dreamwork.core.gui.provider.JobStatusProvider;
import com.dreamwork.core.gui.provider.StatProfileProvider;
import com.dreamwork.core.item.ItemFactory;
import com.dreamwork.core.quest.QuestUI;
import com.dreamwork.core.skill.SkillManager;
import com.dreamwork.core.skill.passive.*;
import com.dreamwork.core.stat.resource.ManaRegenTask;
import com.dreamwork.core.ui.ActionBarManager;
import com.dreamwork.core.ui.BossBarManager;
import com.dreamwork.core.ui.ScoreboardHUD;
import com.dreamwork.core.economy.DreamWorkEconomy;
import com.dreamwork.core.economy.LootTableManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * DreamWork Core 메인 플러그인 클래스
 * 
 * <p>
 * 야생 + 타운 + 성장형 RPG 서버를 위한 핵심 플러그인입니다.
 * Manager Pattern을 사용하여 각 기능을 모듈화합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public final class DreamWorkCore extends JavaPlugin {

    /** Singleton 인스턴스 */
    private static DreamWorkCore instance;

    /** 등록된 매니저 목록 */
    private final List<Manager> managers = new ArrayList<>();

    /** 데이터베이스 매니저 */
    private com.dreamwork.core.database.DatabaseManager databaseManager;

    /** 저장소 매니저 */
    private StorageManager storageManager;

    /** 외부 플러그인 연동 매니저 */
    private HookManager hookManager;

    /** 스탯 매니저 */
    private StatManager statManager;

    /** 직업 매니저 */
    private JobManager jobManager;

    /** 퀸스트 매니저 */
    private QuestManager questManager;

    /** 장비 스캐너 */
    private InventoryScanner inventoryScanner;

    /** 스킬 매니저 */
    private SkillManager skillManager;

    private SetEffectManager setEffectManager;
    private com.dreamwork.core.rank.RankManager rankManager;

    /** 아이템 팩토리 */
    private ItemFactory itemFactory;

    /** UI 매니저 */
    private ActionBarManager actionBarManager;

    /** 보스바 매니저 */
    private BossBarManager bossBarManager;

    /** 스코어보드 HUD */
    private ScoreboardHUD scoreboardHUD;

    /** 드롭 테이블 매니저 */
    private LootTableManager lootTableManager;

    /** 상점 매니저 */
    private com.dreamwork.core.economy.shop.ShopManager shopManager;

    /** 채굴 콤보 시스템 */
    private MiningComboSystem miningComboSystem;

    // ...

    /**
     * 외부 플러그인 연동 매니저를 반환합니다.
     * 
     * @return HookManager 인스턴스
     */
    public HookManager getHookManager() {
        return hookManager;
    }

    public com.dreamwork.core.economy.shop.ShopManager getShopManager() {
        return shopManager;
    }
    // ... (fields continue)

    // ... (getInstance)

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        // 설정 파일 초기화
        saveDefaultConfig();

        // 기본 리소스 저장 (jobs/miner.yml)
        saveDefaultResources();

        // 매니저 초기화
        initializeManagers();

        // 매니저 활성화
        enableManagers();

        // 이벤트 리스너 등록
        registerListeners();

        // 자동 저장 태스크 시작 (30초마다, Dirty-Check 기반)
        new AutoSaveScheduler(this, 600L).start();

        // 마나 재생 태스크 (1초마다)
        new ManaRegenTask(this).start();

        // UI 매니저 시작
        actionBarManager = new ActionBarManager(this);
        actionBarManager.start();
        bossBarManager = new BossBarManager(this);
        scoreboardHUD = new ScoreboardHUD(this);
        scoreboardHUD.start();

        // 드롭 테이블 매니저
        lootTableManager = new LootTableManager(this);

        // Vault 경제 연동
        if (setupEconomy()) {
            getLogger().info("Vault 경제 시스템 연동 성공!");
            // 명령어 등록
            getCommand("money").setExecutor(new com.dreamwork.core.economy.EconomyCommand(this));
            getCommand("town").setExecutor(new com.dreamwork.core.town.TownCommand(this));
        } else {
            getLogger().warning("Vault를 찾을 수 없습니다. 경제 기능이 비활성화됩니다.");
        }

        long endTime = System.currentTimeMillis();
        getLogger()
                .info("DreamWork Core v" + getPluginMeta().getVersion() + " 활성화 완료! (" + (endTime - startTime) + "ms)");
    }

    @Override
    public void onDisable() {
        disableManagers();

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("DreamWork Core가 비활성화되었습니다.");
    }

    /**
     * 기본 리소스 파일들을 저장합니다.
     */
    private void saveDefaultResources() {
        // 직업 설정
        saveResource("jobs/miner.yml", false);
        saveResource("jobs/farmer.yml", false);
        saveResource("jobs/hunter.yml", false);
        saveResource("jobs/fisher.yml", false);
        saveResource("jobs/explorer.yml", false);

        // 퀘스트 설정
        saveResource("quests/daily.yml", false);
        saveResource("quests/weekly.yml", false);

        // 데이터 파일
        saveResource("drops.yml", false);
        saveResource("items.yml", false);
        saveResource("sets.yml", false);
    }

    /**
     * 모든 매니저를 초기화합니다.
     */
    private void initializeManagers() {
        // 데이터베이스 매니저 초기화
        this.databaseManager = new com.dreamwork.core.database.DatabaseManager(this);
        this.databaseManager.initialize();

        // 저장소 매니저 초기화
        this.storageManager = new StorageManager(this, databaseManager);

        // 외부 플러그인 연동 매니저
        hookManager = new HookManager(this);
        registerManager(hookManager);

        // 스탯 매니저
        statManager = new StatManager(this);
        registerManager(statManager);

        // 직업 매니저
        jobManager = new JobManager(this);
        registerManager(jobManager);

        // 퀘스트 매니저
        questManager = new QuestManager(this);
        registerManager(questManager);

        // 장비 스캐너
        inventoryScanner = new InventoryScanner(this);

        // 스킬 매니저
        skillManager = new SkillManager(this);
        registerManager(skillManager);

        // 세트 효과 매니저
        setEffectManager = new SetEffectManager(this);
        registerManager(setEffectManager);

        // 랭킹 매니저
        rankManager = new com.dreamwork.core.rank.RankManager(this);
        registerManager(rankManager);

        // 상점 매니저
        shopManager = new com.dreamwork.core.economy.shop.ShopManager(this);
        registerManager(shopManager);

        // 아이템 팩토리
        itemFactory = new ItemFactory(this);

        getLogger().info(managers.size() + "개의 매니저가 초기화되었습니다.");
    }

    /**
     * 매니저를 등록합니다.
     * 
     * @param manager 등록할 매니저
     */
    private void registerManager(Manager manager) {
        managers.add(manager);
        if (isDebugMode()) {
            getLogger().info("[Debug] 매니저 등록: " + manager.getName());
        }
    }

    /**
     * 모든 매니저를 활성화합니다.
     */
    private void enableManagers() {
        for (Manager manager : managers) {
            try {
                manager.onEnable();
                if (isDebugMode()) {
                    getLogger().info("[Debug] 매니저 활성화: " + manager.getName());
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, manager.getName() + " 매니저 활성화 중 오류 발생!", e);
            }
        }
    }

    /**
     * 이벤트 리스너들을 등록합니다.
     */
    private void registerListeners() {
        // 플레이어 데이터 로드/저장 리스너
        getServer().getPluginManager().registerEvents(new com.dreamwork.core.listener.SkillEffectListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);

        // 직업 활동 이벤트 리스너
        getServer().getPluginManager().registerEvents(new JobActivityListener(this), this);

        // 스탯 효과 리스너
        getServer().getPluginManager().registerEvents(new StatEffectListener(this), this);

        // GUI 리스너
        getServer().getPluginManager().registerEvents(new GuiListener(), this);

        // SmartInventory 리스너
        SmartInventory.registerListener(this);

        // 전투 리스너
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);

        // 상호작용 리스너
        getServer().getPluginManager().registerEvents(new InteractionListener(this), this);

        // 장비 스캐너 (리스너)
        getServer().getPluginManager().registerEvents(inventoryScanner, this);

        // 채굴 콤보 시스템
        this.miningComboSystem = new MiningComboSystem(this);
        getServer().getPluginManager().registerEvents(miningComboSystem, this);

        // 미확인 광물 아이템
        getServer().getPluginManager().registerEvents(new UnidentifiedOreItem(this), this);
        getServer().getPluginManager().registerEvents(new com.dreamwork.core.item.custom.DreamStoneItem(this), this);

        // 농부 시스템
        getServer().getPluginManager().registerEvents(new com.dreamwork.core.job.system.CropQualitySystem(this), this);

        // 어부 시스템
        getServer().getPluginManager().registerEvents(new com.dreamwork.core.job.system.FishMeasurementSystem(this),
                this);

        // 탐험가 시스템
        getServer().getPluginManager().registerEvents(new com.dreamwork.core.job.system.AtlasDiscoverySystem(this),
                this);

        // 사냥꾼 시스템
        getServer().getPluginManager().registerEvents(new com.dreamwork.core.job.system.MobTierSystem(this), this);

        // 패시브 스킬 리스너 등록
        getServer().getPluginManager().registerEvents(new MinerVeinSkill(this), this);
        getServer().getPluginManager().registerEvents(new FarmerAutoReplantSkill(this), this);
        getServer().getPluginManager().registerEvents(new FisherMasterAnglerSkill(this), this);
        getServer().getPluginManager().registerEvents(new HunterCriticalSkill(this), this);
        getServer().getPluginManager().registerEvents(new ExplorerTraversalSkill(this), this);

        if (isDebugMode()) {
            getLogger().info("[Debug] 이벤트 리스너 13개 등록 완료");
        }
    }

    /**
     * 모든 매니저를 비활성화합니다. (역순)
     */
    private void disableManagers() {
        for (int i = managers.size() - 1; i >= 0; i--) {
            Manager manager = managers.get(i);
            try {
                manager.onDisable();
                if (isDebugMode()) {
                    getLogger().info("[Debug] 매니저 비활성화: " + manager.getName());
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, manager.getName() + " 매니저 비활성화 중 오류 발생!", e);
            }
        }
    }

    /**
     * 모든 매니저 설정을 리로드합니다.
     */
    public void reloadAllManagers() {
        reloadConfig();
        for (Manager manager : managers) {
            try {
                manager.reload();
                if (isDebugMode()) {
                    getLogger().info("[Debug] 매니저 리로드: " + manager.getName());
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, manager.getName() + " 매니저 리로드 중 오류 발생!", e);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("dreamwork")) {
            if (args.length == 0) {
                sendMessage(sender, "&6DreamWork Core &7v" + getPluginMeta().getVersion());
                sendMessage(sender, "&7사용법: /dw [reload|help|job|stat|quest]");
                return true;
            }

            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "reload" -> {
                    if (!sender.hasPermission("dreamwork.reload")) {
                        sendMessage(sender, getMessage("no-permission"));
                        return true;
                    }
                    reloadAllManagers();
                    sendMessage(sender, getMessage("reload-success"));
                }
                case "help" -> {
                    sendMessage(sender, "&6=== DreamWork Core 도움말 ===");
                    sendMessage(sender, "&e/dw job &7- 직업 선택 창 열기");
                    sendMessage(sender, "&e/dw stat &7- 스탯 프로필 열기");
                    sendMessage(sender, "&e/dw reload &7- 설정 리로드");
                }
                case "job" -> {
                    if (!(sender instanceof org.bukkit.entity.Player player)) {
                        sendMessage(sender, "&c플레이어만 사용할 수 있습니다.");
                        return true;
                    }
                    SmartInventory.builder()
                            .title("직업 선택")
                            .size(27)
                            .provider(new JobSelectionProvider(player, this))
                            .build()
                            .open(player);
                }
                case "stat" -> {
                    if (!(sender instanceof org.bukkit.entity.Player player)) {
                        sendMessage(sender, "&c플레이어만 사용할 수 있습니다.");
                        return true;
                    }
                    SmartInventory.builder()
                            .title("스탯 프로필")
                            .size(36)
                            .provider(new StatProfileProvider(player, this))
                            .build()
                            .open(player);
                }
                case "quest" -> {
                    if (!(sender instanceof org.bukkit.entity.Player player)) {
                        sendMessage(sender, "&c플레이어만 사용할 수 있습니다.");
                        return true;
                    }
                    SmartInventory.builder()
                            .title("퀸스트")
                            .size(27)
                            .provider(new QuestUI(player, this))
                            .build()
                            .open(player);
                }
                case "skill" -> {
                    if (!(sender instanceof org.bukkit.entity.Player player)) {
                        sendMessage(sender, "&c플레이어만 사용할 수 있습니다.");
                        return true;
                    }
                    if (args.length < 2) {
                        sendMessage(sender, "&c사용법: /dw skill <스킬명>");
                        return true;
                    }
                    skillManager.triggerSkill(player, args[1]);
                }
                case "give" -> {
                    if (!sender.hasPermission("dreamwork.give")) {
                        sendMessage(sender, getMessage("no-permission"));
                        return true;
                    }
                    if (args.length < 3) {
                        sendMessage(sender, "&c사용법: /dw give <플레이어> <아이템명> [수량]");
                        return true;
                    }
                    org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sendMessage(sender, "&c플레이어를 찾을 수 없습니다.");
                        return true;
                    }
                    String itemId = args[2];
                    int amount = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
                    org.bukkit.inventory.ItemStack item = itemFactory.createItem(itemId, amount);
                    if (item == null) {
                        sendMessage(sender, "&c아이템을 찾을 수 없습니다: " + itemId);
                        return true;
                    }
                    target.getInventory().addItem(item);
                    sendMessage(sender, "&a" + target.getName() + "에게 " + itemId + " x" + amount + " 지급!");
                }
                case "rank" -> {
                    if (!(sender instanceof org.bukkit.entity.Player player)) {
                        sendMessage(sender, "&c플레이어만 사용할 수 있습니다.");
                        return true;
                    }

                    if (rankManager == null) {
                        sendMessage(sender, "&c랭킹 시스템이 비활성화되어 있습니다.");
                        return true;
                    }

                    // 내 순위
                    int myRank = rankManager.getPlayerRank(player.getUniqueId());
                    String rankStr = (myRank > 0) ? myRank + "위" : "순위권 밖";
                    sendMessage(sender, "&6=== 나의 순위: &e" + rankStr + " &6===");

                    // Top 10
                    sendMessage(sender, "&6=== TOP 10 랭커 ===");
                    java.util.List<com.dreamwork.core.rank.RankManager.RankEntry> topRankers = rankManager
                            .getTopRankers(10);
                    for (int i = 0; i < topRankers.size(); i++) {
                        com.dreamwork.core.rank.RankManager.RankEntry entry = topRankers.get(i);
                        sendMessage(sender, "&e" + (i + 1) + "위. &f" + entry.getName() +
                                " &7(Lv." + entry.getLevel() + ") &8- " + entry.getJob());
                    }
                }
                case "alloy" -> {
                    if (!(sender instanceof org.bukkit.entity.Player player)) {
                        sendMessage(sender, "&c플레이어만 사용할 수 있습니다.");
                        return true;
                    }
                    com.dreamwork.core.gui.SmartInventory.builder()
                            .title("§8합금 제련소")
                            .size(27)
                            .provider(new com.dreamwork.core.gui.provider.AlloyCraftingProvider(player, this))
                            .build()
                            .open(player);
                }
                case "scythe" -> {
                    if (!(sender instanceof org.bukkit.entity.Player player)) {
                        sendMessage(sender, "&c플레이어만 사용할 수 있습니다.");
                        return true;
                    }
                    // 풍요의 낫 지급 (테스트용)
                    org.bukkit.inventory.ItemStack scythe = new com.dreamwork.core.item.custom.ScytheItem(this)
                            .createScythe();
                    player.getInventory().addItem(scythe);
                    sendMessage(sender, "&a풍요의 낫을 지급했습니다.");
                }
                case "shop" -> {
                    if (!(sender instanceof org.bukkit.entity.Player player)) {
                        sendMessage(sender, "&c플레이어만 사용할 수 있습니다.");
                        return true;
                    }
                    String shopId = args.length > 1 ? args[1] : "general";
                    shopManager.openShop(player, shopId);
                }
                default -> sendMessage(sender, getMessage("unknown-command"));
            }
            return true;
        }

        // /내정보 명령어 처리
        if (command.getName().equalsIgnoreCase("내정보"))

        {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sendMessage(sender, "&c플레이어만 사용할 수 있습니다.");
                return true;
            }
            SmartInventory.builder()
                    .title("§6§l내 직업 정보")
                    .size(45)
                    .provider(new JobStatusProvider(player, this))
                    .build()
                    .open(player);
            return true;
        }
        return false;
    }

    /**
     * 메시지를 전송합니다.
     * 
     * @param sender  수신자
     * @param message 메시지 (색상 코드 포함 가능)
     */
    public void sendMessage(CommandSender sender, String message) {
        String prefix = getConfig().getString("messages.prefix", "&8[&6DreamWork&8] &f");
        Component component = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(prefix + message);
        sender.sendMessage(component);
    }

    /**
     * 설정 파일에서 메시지를 가져옵니다.
     * 
     * @param key 메시지 키
     * @return 메시지 문자열
     */
    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "&c메시지를 찾을 수 없습니다: " + key);
    }

    /**
     * 디버그 모드 여부를 반환합니다.
     * 
     * @return 디버그 모드 활성화 상태
     */
    public boolean isDebugMode() {
        return getConfig().getBoolean("general.debug", false);
    }

    /**
     * 저장소 매니저를 반환합니다.
     * 
     * @return StorageManager 인스턴스
     */
    public StorageManager getStorageManager() {
        return storageManager;
    }

    // ==================== Vault Integration ====================

    private static net.milkbowl.vault.economy.Economy econ = null;

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        // DreamWorkEconomy 인스턴스 생성 및 등록
        DreamWorkEconomy economyProvider = new com.dreamwork.core.economy.DreamWorkEconomy(this);
        getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, economyProvider, this,
                org.bukkit.plugin.ServicePriority.Highest);

        // 등록된 프로바이더 확인
        org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = getServer()
                .getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static net.milkbowl.vault.economy.Economy getEconomy() {
        return econ;
    }

    /**
     * 스탯 매니저를 반환합니다.
     * 
     * @return StatManager 인스턴스
     */
    public StatManager getStatManager() {
        return statManager;
    }

    /**
     * 데이터베이스 매니저를 반환합니다.
     * 
     * @return 데이터베이스 매니저
     */
    public com.dreamwork.core.database.DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    // ... EXISTING GETTERS ...
    /**
     * 직업 매니저를 반환합니다.
     * 
     * @return JobManager 인스턴스
     */
    public JobManager getJobManager() {
        return jobManager;
    }

    /**
     * 퀸스트 매니저를 반환합니다.
     * 
     * @return QuestManager 인스턴스
     */
    public QuestManager getQuestManager() {
        return questManager;
    }

    /**
     * 스킬 매니저를 반환합니다.
     * 
     * @return SkillManager 인스턴스
     */
    public SkillManager getSkillManager() {
        return skillManager;
    }

    public SetEffectManager getSetEffectManager() {
        return setEffectManager;
    }

    /**
     * 아이템 팩토리를 반환합니다.
     * 
     * @return ItemFactory 인스턴스
     */
    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    /**
     * 액션바 매니저를 반환합니다.
     * 
     * @return ActionBarManager 인스턴스
     */
    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    /**
     * 보스바 매니저를 반환합니다.
     * 
     * @return BossBarManager 인스턴스
     */
    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    /**
     * 스코어보드 HUD를 반환합니다.
     * 
     * @return ScoreboardHUD 인스턴스
     */
    public ScoreboardHUD getScoreboardHUD() {
        return scoreboardHUD;
    }

    /**
     * 드롭 테이블 매니저를 반환합니다.
     * 
     * @return LootTableManager 인스턴스
     */
    public LootTableManager getLootTableManager() {
        return lootTableManager;
    }

    /**
     * 채굴 콤보 시스템을 반환합니다.
     * 
     * @return MiningComboSystem 인스턴스
     */
    public MiningComboSystem getMiningComboSystem() {
        return miningComboSystem;
    }
}
