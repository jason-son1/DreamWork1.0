# 🛠️ DreamWork 서버 마스터 플랜 (Ver 2.0)

> **Slogan:** "야생에서의 땀방울(Work)로 나만의 꿈(Dream)을 건설하라."

## 1. 핵심 철학 및 변경된 방향성

기존 계획이 'RPG 스킬 난사'에 가까웠다면, 변경된 계획은 **'깊이 있는 생활 콘텐츠'**와 **'경제적 상호작용'**에 초점을 맞춥니다.

1. **현실적 성장 (Realistic Progression):** 마법 같은 스킬 대신, 숙련도가 쌓일수록 더 좋은 재료를 얻고, 더 편리한 도구를 사용하는 구조입니다.
    
2. **데이터 기반 아이템 (Data-Driven Items):** 별도의 모드 없이 `PDC(PersistentDataContainer)`와 `NBT`를 활용하여, 같은 물고기나 곡물이라도 등급과 가치를 다르게 매깁니다.
    
3. **타운 중심의 경제 (Town-Centric Economy):** 모든 직업의 최종 결과물(합금, 요리, 살아있는 물고기, 지도, 트로피)은 타운의 특정 건물에서 가공되거나 소비됩니다.
    

## 2. ⚔️ 5대 직업 시스템 상세 (Revised)

모든 유저는 5가지 직업을 모두 보유하며, 행동에 따라 각 직업의 레벨이 오릅니다.

### ⛏️ 1. 광부 (Miner): 대지의 개척자 & 대장장이

- **핵심 역할:** 광물 채굴 및 **'합금(Alloy)'** 제련. 건축가에게 필요한 특수 블록 권한이나 강화된 도구를 공급.
    
- **주요 스킬 (개편됨):**
    
    - **[액티브] 광맥 탐지 (Ore Radar):** 스킬 발동 시 반경 N블록 내 광물의 종류와 대략적 매장량을 채팅으로 파악. (VeinMiner 삭제됨)
        
    - **[패시브] 지하 적응 (Cave Adaptation):** 깊은 지하(Y좌표 이하)에서 낙하 대미지 감소 및 용암 저항(일시적) 등 생존력 강화.
        
    - **[패시브] 광부의 몰입 (Miner's Trance):** 한 지역에서 오래 채굴할수록 희귀 광물(드림스톤 등) 드롭률 상승.
        
- **전용 아이템 & 경제:**
    
    - **미지의 광석:** 채굴 시 얻는 식별 불가능한 돌. 타운 대장간에서 감정해야 함.
        
    - **합금 (Alloys):** `철+금+드림스톤` 등을 섞어 만듦. 건축 월드 입장권 연장이나 도구 강화의 필수 재료.
        
    - **커스텀 곡괭이:** 레벨에 따라 해금되는 전용 곡괭이 (경도 강화, 행운 특화 등).
        

### 🌾 2. 농부 (Farmer): 대지의 관리자 & 미식가

- **핵심 역할:** 고품질 식량 생산 및 **'버프 요리/주조'**.
    
- **주요 스킬 (개편됨):**
    
    - **[패시브] 녹색 손길 (Green Thumb):** 수확 시 자동으로 씨앗을 재파종. (단순 노동 시간 단축)
        
    - **[패시브] 대지의 기운 (Growth Aura):** 농부 주변 작물의 성장 속도가 빨라짐.
        
    - **[아이템] 풍년의 의식:** '풍요의 뿔피리' 아이템 사용 시 주변 작물 즉시 성장.
        
- **전용 아이템 & 경제:**
    
    - **작물 등급제 (1~3성):** NBT 태그로 `Quality: 3`이 붙은 작물은 상점이 아닌 '요리'에 사용.
        
    - **버프 요리:** 3성 작물로 만든 요리는 단순 포만감이 아닌 스탯 버프(채광 속도, 체력 증가)를 제공.
        
    - **특수 비료:** 속성 비료, 변이 비료 등을 사용하여 황금 작물 재배.
        

### 🎣 3. 어부 (Fisher): 심해의 탐구자 & 항해사

- **핵심 역할:** 희귀 어종 수집 및 **'아쿠아리움 납품'**.
    
- **주요 스킬 (개편됨):**
    
    - **[패시브] 입질 감지 (Sensitivity):** 레벨이 오를수록 낚시 대기 시간(Bite time) 감소.
        
    - **[패시브] 어종 지식:** 해당 수역의 서식 어종 정보를 파악.
        
    - **[아이템] 떡밥 투척 (Chumming):** 떡밥 아이템을 물에 던져 히든 물고기 등장 확률 증가.
        
- **전용 아이템 & 경제:**
    
    - **살아있는 물고기 (Live Fish):** 물 양동이나 살림망에 담아온 물고기는 타운 아쿠아리움에 비싸게 납품.
        
    - **회 (Sashimi):** 즉시 섭취 가능한 포션형 식량. (PVP/레이드 필수품)
        
    - **심해 인양물:** 산호, 프리즈머린 등 건축 자재 수급.
        

### 🗺️ 4. 탐험가 (Adventurer): 지평선의 기록자

- **핵심 역할:** 맵 확장, 특수 지형 발견 및 **'좌표/바이옴 정보 판매'**.
    
- **주요 스킬 (개편됨):**
    
    - **[패시브] 험지 주파:** 흙길, 잔디 등 특정 블록 위에서 이동 속도 증가.
        
    - **[패시브] 육감 (Sixth Sense):** 주변 구조물(유적, 스포너) 방향을 어렴풋이 감지.
        
    - **[아이템] 귀환석 (Recall):** 직접 제작한 귀환석으로 스폰/타운으로 텔레포트 (쿨타임 존재).
        
- **전용 아이템 & 경제:**
    
    - **좌표 스크롤:** "메사 바이옴 좌표 팝니다." (우클릭 시 나침반 타겟 고정).
        
    - **바이옴 캡슐:** 특정 바이옴의 색깔(잔디색, 나뭇잎색)을 건축가의 땅에 적용하는 소모품.
        
    - **탐사 보고서:** 유적 최초 발견 시 타운 NPC에게 보고하여 큰 보상 획득.
        

### 🏹 5. 사냥꾼 (Hunter): 야생의 수호자 & 용병

- **핵심 역할:** 몬스터 소재 공급 및 **'보스 레이드 주최'**.
    
- **주요 스킬 (개편됨):**
    
    - **[패시브] 약점 간파 (Vital Strike):** 확률적 치명타 공격.
        
    - **[패시브] 사냥꾼의 지식 (Slayer Knowledge):** 특정 몹을 많이 잡을수록 해당 몹에 대한 공/방 능력치 상승 (도감 시스템).
        
- **전용 아이템 & 경제:**
    
    - **사냥용 덫/미끼:** 몬스터를 속박하거나 유인하여 사냥 효율 극대화.
        
    - **제압용 목줄:** 딸피(?) 몬스터를 포획하여 펫으로 삼음.
        
    - **박제 트로피:** 보스/엘리트 몹의 머리를 가구 아이템으로 가공하여 판매.
        

## 3. 🏘️ 타운(Town) 인프라 구축 계획

타운은 단순한 주거지가 아니라, 각 직업의 결과물이 모이고 가공되는 **'경제의 순환점'**입니다.

|                     |           |                                                                                                       |
| ------------------- | --------- | ----------------------------------------------------------------------------------------------------- |
| **시설명**             | **담당 직업** | **기능 및 경제 활동**                                                                                        |
| **대장간 (The Forge)** | **광부**    | - 미지의 광석 감정<br><br>  <br><br>- **합금 제련** (일반 화로 불가)<br><br>  <br><br>- 커스텀 곡괭이 수리/강화                  |
| **레스토랑 & 주점**       | **농부**    | - 3성 작물 매입 및 **버프 요리** 판매<br><br>  <br><br>- 술(Brewing) 숙성고 운영<br><br>  <br><br>- 유저들이 모여 식사 시 경험치 버프 |
| **아쿠아리움**           | **어부**    | - **살아있는 물고기** 고가 매입<br><br>  <br><br>- 희귀 어종 전시 (도감작)<br><br>  <br><br>- 생선 손질(회 뜨기) 및 부산물 상점        |
| **지도 제작소**          | **탐험가**   | - **좌표 스크롤** 거래 게시판<br><br>  <br><br>- 서버 전체지도(Dynmap) 안개 걷기 기여도 보상<br><br>  <br><br>- 바이옴 캡슐 판매      |
| **투기장 & 트로피 룸**     | **사냥꾼**   | - 보스 소환 토템 사용 (레이드 장소)<br><br>  <br><br>- 몬스터 **박제(Trophy)** 전시 및 거래<br><br>  <br><br>- 펫 관련 용품 판매    |
| **건축 사무소**          | **공통**    | - **설계도(Schematic)** 저장 및 실체화 요청<br><br>  <br><br>- 건축 자재 대량 주문 (광부/탐험가에게 발주)                         |

## 4. ⚙️ 시스템 구현 (Technical Specification)

자체 플러그인(`DreamWork`) 하나로 핵심 로직을 통제하며, 무거운 외부 플러그인 의존도를 낮춥니다.

### 4.1 데이터 저장 방식 (PDC & NBT)

새로운 아이템을 모드로 추가하는 것이 아니라, 바닐라 아이템에 데이터를 숨겨서 기능을 구현합니다.

- **물고기:** `minecraft:salmon` 아이템에 `{DreamFish: {Type: "King_Salmon", Size: 120.5, Live: true}}` 태그 부여.
    
- **작물:** `minecraft:potato` 아이템에 `{DreamCrop: {Quality: 3}}` 태그 부여.
    
- **귀환석:** `minecraft:stone_button` 아이템에 `{DreamItem: {Type: "Recall_Stone", Owner: "UUID"}}` 태그 부여.
    

### 4.2 직업 및 성장 엔진 (Configurable)

하드코딩을 피하고 `jobs.yml` 등의 설정 파일로 밸런스를 잡습니다.

- **행동 감지:** `BlockBreakEvent`, `PlayerFishEvent` 등을 통합 리스너로 감지.
    
- **조건 체크:** 유저가 해당 직업 도구를 썼는가? (농부의 괭이, 광부의 곡괭이) -> 맞다면 숙련도 증가.
    

### 4.3 경제 순환 (The Loop)

1. **생산:** 야생에서 재료 획득 (데이터 태그가 붙은 특수 재료).
    
2. **가공:** 타운의 특수 시설(대장간, 주방 등)에서만 상위 아이템으로 변환 가능.
    
3. **소비:**
    
    - **건축:** 타운 땅 구매, 설계도 실체화 비용, 특수 블록(바이옴 캡슐 등) 구매.
        
    - **성장:** 더 좋은 도구(합금 곡괭이), 더 빠른 사냥(버프 요리/회)을 위해 소비.
        

## 5. 📅 개발 우선순위 (Roadmap)

1. **Phase 1: 기반 (Infrastructure)**
    
    - LuckPerms, Vault, Towny, Multiverse 설치 및 월드 분리.
        
    - `DreamWork` 플러그인 뼈대 생성 (이벤트 리스너, Config 로더).
        
2. **Phase 2: 직업 로직 이식 (Core Logic)**
    
    - 5개 직업의 경험치 획득 로직 구현.
        
    - **PDC/NBT 시스템 구축:** 작물 등급, 물고기 크기 등이 아이템에 정상적으로 붙는지 테스트.
        
3. **Phase 3: 타운 기능 구현 (Interaction)**
    
    - 타운 내 NPC 및 GUI 상점 구현 (Citizens + ShopGUI+ 활용).
        
    - 대장간/주방 등의 가공 시스템 로직 구현 (특정 위치에서 상호작용 시 아이템 변환).
        
4. **Phase 4: 콘텐츠 채우기 (Content)**
    
    - 미지의 광석, 보물지도, 현상수배 등 '재미 요소' 데이터 입력.
        
    - 건축 월드 ↔ 타운 월드 간의 설계도 시스템 연동 테스트.
      
      
      
      
## Architecture

# 🛠️ DreamWork & Custom Shop 개발 아키텍처

이 문서는 **DreamWork 서버 마스터 플랜 v2**를 구현하기 위한 자체 플러그인(`DreamWork`, `DreamShop`)의 개발 구조도입니다. 기존 플러그인(Essentials, Towny, Vault 등)과의 연동성을 최우선으로 고려했습니다.

## 1. 플러그인 역할 분담 (Dependency Flow)

두 개의 자체 플러그인은 다음과 같은 관계를 가집니다.

1. **DreamWork (Core):**
    
    - **역할:** 직업 레벨 관리, 스탯 계산, **커스텀 아이템 데이터(PDC) 생성**, 스킬 트리거 감지.
        
    - **연동:** Vault(돈 지급), PlaceholderAPI(정보 표시), Towny(타운 내 효과), LuckPerms(칭호).
        
2. **DreamShop (EconomyShop):**
    
    - **역할:** `DreamWork`에서 만든 **특수 데이터 아이템(3성 작물, 살아있는 물고기 등)을 인식**하고 매입/매도하는 GUI 상점.
        
    - **연동:** `DreamWork`(아이템 검증), Vault(돈 차감/지급).
        

## 2. 📁 프로젝트 구조 및 필수 의존성 (pom.xml)

### 2.1 필수 API 의존성 (Dependencies)

`DreamWork` 플러그인이 정상 작동하기 위해 `pom.xml`에 포함해야 할 API들입니다.

```
<dependencies>
    <!-- Spigot API (1.20.x 권장) -->
    <dependency>
        <groupId>org.spigotmc</groupId>
        <artifactId>spigot-api</artifactId>
        <version>1.20.1-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    <!-- Vault API (경제 연동) -->
    <dependency>
        <groupId>com.github.MilkBowl</groupId>
        <artifactId>VaultAPI</artifactId>
        <version>1.7</version>
        <scope>provided</scope>
    </dependency>
    <!-- PlaceholderAPI (PAPI) -->
    <dependency>
        <groupId>me.clip</groupId>
        <artifactId>placeholderapi</artifactId>
        <version>2.11.2</version>
        <scope>provided</scope>
    </dependency>
    <!-- Towny (타운 연동) -->
    <dependency>
        <groupId>com.palmergames.bukkit.towny</groupId>
        <artifactId>towny</artifactId>
        <version>0.99.5.0</version>
        <scope>provided</scope>
    </dependency>
    <!-- Citizens (NPC 연동) -->
    <dependency>
        <groupId>net.citizensnpcs</groupId>
        <artifactId>citizens-main</artifactId>
        <version>2.0.30-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## 3. 🧩 DreamWork (Core) 핵심 모듈 구현

마스터 플랜 v2의 핵심인 **'데이터 기반 아이템'**과 **'직업 스킬'**을 구현하는 로직입니다.

### 3.1 Item Manager: 데이터 주도 아이템 (PDC System)

별도의 모드 없이 아이템에 데이터를 숨겨 등급을 나누는 핵심 클래스입니다.

```
public class DreamItemManager {
    
    private final Plugin plugin;
    // 고유 키 생성 (예: dreamwork:quality, dreamwork:fish_size)
    private final NamespacedKey qualityKey; 
    private final NamespacedKey fishSizeKey;

    public DreamItemManager(Plugin plugin) {
        this.plugin = plugin;
        this.qualityKey = new NamespacedKey(plugin, "quality");
        this.fishSizeKey = new NamespacedKey(plugin, "fish_size");
    }

    // 3성 작물 생성 예시
    public ItemStack createQualityCrop(Material material, int quality) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // 1. PDC에 데이터 저장 (NBT와 유사)
        meta.getPersistentDataContainer().set(qualityKey, PersistentDataType.INTEGER, quality);
        
        // 2. Lore(설명) 및 이름 변경 (시각적 표시)
        String stars = "★".repeat(quality);
        meta.setDisplayName(ChatColor.GOLD + stars + " " + formatName(material));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "등급: " + quality + "성");
        lore.add(ChatColor.GRAY + "특수 요리 재료로 사용됩니다.");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }

    // 아이템 등급 확인 메서드 (상점 플러그인에서 사용)
    public int getQuality(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(qualityKey, PersistentDataType.INTEGER, 0);
    }
}
```

### 3.2 Job Listener: 직업 경험치 및 스킬 트리거

`JobManager`를 통해 중앙에서 이벤트를 관리합니다. 예시로 **광부(Miner)**의 로직을 구현합니다.

```
public class MinerJobListener implements Listener {

    private final DreamWorkPlugin plugin;
    private final MinerJob minerJob; // 레벨 및 스킬 정보 관리 객체

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // 1. 도구 검증 (광부 곡괭이인지 확인)
        if (!isMinerPickaxe(player.getInventory().getItemInMainHand())) return;

        // 2. [패시브] 광부의 몰입 (Miner's Trance) 체크
        // 플레이어가 한 청크에 얼마나 머물렀는지 체크하여 드롭률 보정
        double dropMultiplier = plugin.getJobManager().getMinerTranceMultiplier(player);

        // 3. 커스텀 아이템 드롭 (미지의 광석 등)
        if (isRareDrop(block.getType(), dropMultiplier)) {
            event.setDropItems(false); // 기본 드롭 취소
            // 커스텀 아이템 드롭 (PDC 데이터가 포함된 아이템)
            block.getWorld().dropItemNaturally(block.getLocation(), 
                plugin.getItemManager().createUnknownOre());
            
            player.sendMessage(ChatColor.GREEN + "✨ 미지의 광석을 발견했습니다!");
        }

        // 4. 경험치 지급
        plugin.getJobManager().addExp(player, "miner", getExpForBlock(block.getType()));
    }
    
    // [액티브] 광맥 탐지 (Ore Radar) - 우클릭 스킬
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;
        Player player = event.getPlayer();
        
        // 광부 전용 아이템(탐지기 등)을 들고 있는지 확인
        if (isOreRadar(event.getItem())) {
            // 쿨타임 체크 후 실행
            plugin.getSkillManager().castOreRadar(player); 
        }
    }
}
```

## 4. 💰 DreamShop (EconomyShop) 구현

기존 `ShopGUI+` 같은 플러그인은 단순한 아이템 ID(Material)만 체크하므로, **PDC 데이터(3성, 살아있는 물고기)**를 구분하지 못합니다. 따라서 자체 상점 플러그인이 필요합니다.

### 4.1 상점 구조 (GUI)

`Inventory` API를 사용하여 GUI를 엽니다.

```
public void openSellMenu(Player player) {
    Inventory gui = Bukkit.createInventory(null, 27, "농산물 거래소 (3성 전문)");

    // GUI 아이콘 세팅...
    // 플레이어 인벤토리 스캔 로직
    player.openInventory(gui);
}
```

### 4.2 매입 로직 (PDC 검증)

플레이어가 아이템을 팔 때, **단순히 감자(POTATO)인지 확인하는 게 아니라, 3성 감자인지 확인**합니다.

```
public void sellItems(Player player) {
    double totalEarnings = 0;
    int count = 0;

    for (ItemStack item : player.getInventory().getContents()) {
        if (item == null) continue;

        // DreamWork 플러그인의 API를 호출하여 데이터 확인
        int quality = DreamWork.getInstance().getItemManager().getQuality(item);

        if (item.getType() == Material.POTATO && quality == 3) {
            // 3성 감자 가격 계산
            double price = 50.0; // 설정 파일에서 로드
            totalEarnings += price * item.getAmount();
            count += item.getAmount();
            
            // 아이템 제거
            player.getInventory().remove(item);
        }
    }
    
    // Vault API로 돈 지급
    Economy economy = DreamWork.getInstance().getVaultEconomy();
    economy.depositPlayer(player, totalEarnings);
    
    player.sendMessage(count + "개의 3성 감자를 팔아 " + totalEarnings + "D를 벌었습니다.");
}
```

## 5. 🔌 외부 플러그인 연동 (Hooks)

### 5.1 Vault (화폐 연동)

`DreamWork`나 `DreamShop`이 시작될 때(`onEnable`), Vault 서비스가 존재하는지 확인하고 객체를 가져와야 합니다.

```
private boolean setupEconomy() {
    if (getServer().getPluginManager().getPlugin("Vault") == null) {
        return false;
    }
    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) {
        return false;
    }
    economy = rsp.getProvider();
    return economy != null;
}
```

### 5.2 PlaceholderAPI (정보 표시)

스코어보드나 탭 목록에 직업 레벨을 띄우기 위해 `PlaceholderExpansion`을 상속받아 구현합니다.

- `%dreamwork_miner_level%` -> 광부 레벨 반환
    
- `%dreamwork_total_rank%` -> 종합 랭크 반환
    

### 5.3 Towny (타운 연동)

특정 스킬(예: 어부의 납품, 농부의 요리)이 **타운 내 특정 청크(Plot)**에서만 작동하도록 제한할 때 사용합니다.

```
public boolean isInTown(Location loc) {
    try {
        TownBlock townBlock = TownyUniverse.getInstance().getTownBlock(WorldCoord.parseWorldCoord(loc));
        return townBlock != null && townBlock.hasTown();
    } catch (Exception e) {
        return false;
    }
}
```

## 6. 🚀 개발 로드맵 (우선순위)

1. **Project Setup:** IDE(IntelliJ)에서 Maven 프로젝트 생성, `spigot-api`, `Vault`, `Towny` 의존성 추가.
    
2. **Core (PDC Manager):** `PersistentDataContainer`를 이용해 일반 아이템에 '등급'과 '데이터'를 입히는 유틸리티 클래스부터 개발하세요. (가장 중요)
    
3. **Miner & Farmer Job:**
    
    - 광부가 돌을 캘 때 확률적으로 PDC가 적용된 '미지의 광석' 드롭.
        
    - 농부가 수확할 때 확률적으로 PDC가 적용된 '3성 작물' 드롭.
        
4. **Custom Shop:**
    
    - `/sell 3star` 같은 명령어를 쳤을 때, 인벤토리에서 PDC 아이템만 쏙 골라내서 팔아지는지 테스트.
        
5. **GUI Integration:**
    
    - 명령어 기반 테스트가 끝나면 GUI(상자창)를 입힙니다.
        
6. **Skills & Balance:**
    
    - 마지막으로 쿨타임, 스킬 효과(광맥 탐지 등), 경험치 테이블을 `config.yml`로 뺍니다.