package com.dreamwork.core.skill.skills;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.job.JobManager;
import com.dreamwork.core.job.UserJobData;
import com.dreamwork.core.skill.SkillEffect;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 광부 10레벨 스킬: 광맥 탐지 (Ore Radar)
 * 
 * <p>
 * Plan 2.0 기준:
 * - 광부 전용 곡괭이를 들고 우클릭으로 발동
 * - 반경 N블록 내 광물의 종류와 대략적 매장량을 채팅으로 출력
 * - 레벨에 따라 탐지 반경, 정밀도, 쿨타임이 달라짐
 * </p>
 */
public class OreRadar implements SkillEffect {

    private final DreamWorkCore plugin;

    /** 탐지 가능한 광물 목록 */
    private static final Set<Material> DETECTABLE_ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.ANCIENT_DEBRIS, Material.NETHER_QUARTZ_ORE);

    public OreRadar(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        // 플레이어 레벨에 따른 설정 결정
        int minerLevel = getMinerLevel(player);
        int radius = getRadius(minerLevel);
        InfoLevel infoLevel = getInfoLevel(minerLevel);
        boolean showDirection = minerLevel >= 30;

        // 반경 내 광물 스캔
        Map<Material, Integer> oreCount = scanOres(player.getLocation(), radius);

        // 결과 출력
        displayResults(player, oreCount, infoLevel, showDirection);

        // 이펙트
        player.getWorld().spawnParticle(
                Particle.ENCHANT,
                player.getLocation().add(0, 1, 0),
                50, 2, 1, 2, 0.1);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);

        player.sendMessage(ChatColor.GREEN + "[광맥 탐지] " + ChatColor.WHITE +
                "반경 " + radius + "블록 스캔 완료!");
    }

    /**
     * 반경 내 광물을 스캔하여 종류별 개수를 반환합니다.
     */
    private Map<Material, Integer> scanOres(Location center, int radius) {
        Map<Material, Integer> result = new EnumMap<>(Material.class);

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = Math.max(cy - radius, center.getWorld().getMinHeight()); y <= Math.min(cy + radius,
                    center.getWorld().getMaxHeight()); y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    Block block = center.getWorld().getBlockAt(x, y, z);
                    Material type = block.getType();

                    if (DETECTABLE_ORES.contains(type)) {
                        result.merge(type, 1, Integer::sum);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 스캔 결과를 플레이어에게 표시합니다.
     */
    private void displayResults(Player player, Map<Material, Integer> oreCount,
            InfoLevel infoLevel, boolean showDirection) {
        if (oreCount.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "  주변에서 광물이 감지되지 않았습니다.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "═══ 광맥 탐지 결과 ═══");

        // 광물을 희귀도 순으로 정렬
        List<Map.Entry<Material, Integer>> sorted = new ArrayList<>(oreCount.entrySet());
        sorted.sort((a, b) -> getRarity(b.getKey()) - getRarity(a.getKey()));

        for (Map.Entry<Material, Integer> entry : sorted) {
            Material ore = entry.getKey();
            int count = entry.getValue();

            String oreName = getOreName(ore);
            ChatColor color = getOreColor(ore);

            String countDisplay;
            switch (infoLevel) {
                case BASIC:
                    countDisplay = "감지됨";
                    break;
                case APPROXIMATE:
                    countDisplay = getApproximateCount(count);
                    break;
                case PRECISE:
                default:
                    countDisplay = count + "개";
                    break;
            }

            player.sendMessage(color + "  ◆ " + oreName + ": " + ChatColor.WHITE + countDisplay);
        }

        player.sendMessage(ChatColor.GOLD + "═════════════════════");
    }

    /**
     * 대략적인 수량을 반환합니다.
     */
    private String getApproximateCount(int count) {
        if (count <= 5)
            return "소량";
        if (count <= 15)
            return "중량";
        if (count <= 30)
            return "다량";
        return "대량";
    }

    /**
     * 광물의 한국어 이름을 반환합니다.
     */
    private String getOreName(Material ore) {
        switch (ore) {
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return "석탄";
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return "구리";
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return "철";
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case NETHER_GOLD_ORE:
                return "금";
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return "레드스톤";
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return "청금석";
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return "다이아몬드";
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return "에메랄드";
            case ANCIENT_DEBRIS:
                return "고대 잔해";
            case NETHER_QUARTZ_ORE:
                return "네더 석영";
            default:
                return ore.name();
        }
    }

    /**
     * 광물의 표시 색상을 반환합니다.
     */
    private ChatColor getOreColor(Material ore) {
        switch (ore) {
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return ChatColor.AQUA;
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return ChatColor.GREEN;
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case NETHER_GOLD_ORE:
                return ChatColor.GOLD;
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return ChatColor.RED;
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return ChatColor.BLUE;
            case ANCIENT_DEBRIS:
                return ChatColor.DARK_RED;
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return ChatColor.WHITE;
            default:
                return ChatColor.GRAY;
        }
    }

    /**
     * 광물의 희귀도를 반환합니다 (정렬용).
     */
    private int getRarity(Material ore) {
        switch (ore) {
            case ANCIENT_DEBRIS:
                return 100;
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return 90;
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return 85;
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
                return 50;
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return 40;
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return 30;
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return 20;
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return 10;
            default:
                return 0;
        }
    }

    /**
     * 플레이어의 광부 레벨을 반환합니다.
     */
    private int getMinerLevel(Player player) {
        JobManager jobManager = plugin.getJobManager();
        if (jobManager == null)
            return 1;

        UserJobData jobData = jobManager.getUserJob(player.getUniqueId());
        if (!jobData.hasJob() || !"miner".equals(jobData.getJobId()))
            return 1;
        return jobData.getLevel();
    }

    /**
     * 레벨에 따른 탐지 반경을 반환합니다.
     */
    private int getRadius(int level) {
        if (level >= 50)
            return 30;
        if (level >= 30)
            return 30;
        if (level >= 10)
            return 20;
        return 10;
    }

    /**
     * 레벨에 따른 정보 수준을 결정합니다.
     */
    private InfoLevel getInfoLevel(int level) {
        if (level >= 30)
            return InfoLevel.PRECISE;
        if (level >= 10)
            return InfoLevel.APPROXIMATE;
        return InfoLevel.BASIC;
    }

    /**
     * 레벨에 따른 쿨타임을 반환합니다.
     */
    private int getCooldownForLevel(int level) {
        if (level >= 50)
            return 15;
        if (level >= 30)
            return 30;
        if (level >= 10)
            return 45;
        return 60;
    }

    /**
     * 정보 출력 레벨
     */
    private enum InfoLevel {
        /** 유무만 표시 */
        BASIC,
        /** 대략적 수량 (소량/다량) */
        APPROXIMATE,
        /** 정확한 수량 */
        PRECISE
    }

    @Override
    public String getId() {
        return "ore_radar";
    }

    @Override
    public String getName() {
        return "광맥 탐지";
    }

    @Override
    public String getDescription() {
        return "반경 내 광물의 종류와 매장량을 파악합니다.";
    }

    @Override
    public int getCooldown() {
        return 60; // 기본 쿨타임, 레벨에 따라 달라짐
    }

    @Override
    public int getManaCost() {
        return 10;
    }

    @Override
    public int getRequiredLevel() {
        return 10;
    }

    @Override
    public String getRequiredJob() {
        return "miner";
    }
}
