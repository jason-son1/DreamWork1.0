package com.dreamwork.core.skill.skills;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.skill.SkillEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * 공용 스킬: 순간 이동 (대시)
 * 
 * <p>
 * 바라보는 방향으로 빠르게 이동합니다.
 * </p>
 */
public class Dash implements SkillEffect {

    private final DreamWorkCore plugin;
    private final double dashDistance = 8.0;

    public Dash(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player) {
        Location start = player.getLocation();
        Vector direction = start.getDirection().normalize();

        // 대시 목적지 계산 (장애물 체크)
        Location destination = start.clone();
        for (int i = 1; i <= (int) dashDistance; i++) {
            Location nextLoc = start.clone().add(direction.clone().multiply(i));
            if (nextLoc.getBlock().isPassable() &&
                    nextLoc.clone().add(0, 1, 0).getBlock().isPassable()) {
                destination = nextLoc;
            } else {
                break;
            }
        }

        // 이펙트 (시작 위치)
        player.getWorld().spawnParticle(Particle.CLOUD, start, 10, 0.5, 0.5, 0.5, 0.05);

        // 텔레포트
        destination.setYaw(start.getYaw());
        destination.setPitch(start.getPitch());
        player.teleport(destination);

        // 이펙트 (도착 위치)
        player.getWorld().spawnParticle(Particle.CLOUD, destination, 10, 0.5, 0.5, 0.5, 0.05);
        player.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);

        // 잔상 효과 (선택적)
        spawnTrail(start, destination);
    }

    private void spawnTrail(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);

        for (double d = 0; d < distance; d += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.WITCH, point.add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0);
        }
    }

    @Override
    public String getId() {
        return "dash";
    }

    @Override
    public String getName() {
        return "대시";
    }

    @Override
    public String getDescription() {
        return "바라보는 방향으로 " + (int) dashDistance + "블록 순간 이동합니다.";
    }

    @Override
    public int getCooldown() {
        return 10; // 10초
    }

    @Override
    public int getManaCost() {
        return 15;
    }

    @Override
    public int getRequiredLevel() {
        return 1; // 기본 스킬
    }

    @Override
    public String getRequiredJob() {
        return null; // 모든 직업 사용 가능
    }
}
