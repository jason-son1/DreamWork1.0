package com.dreamwork.core.town;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.provider.RestaurantProvider;
import com.dreamwork.core.gui.provider.AquariumProvider;
import com.dreamwork.core.gui.provider.MapWorkshopProvider;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 타운 시설 관련 명령어
 * /restaurant - 레스토랑 GUI
 * /aquarium - 아쿠아리움 납품 GUI
 * /mapworkshop - 지도 제작소 GUI
 */
public class TownFacilityCommand implements CommandExecutor, TabCompleter {

    private final DreamWorkCore plugin;

    public TownFacilityCommand(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.");
            return true;
        }

        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "restaurant", "레스토랑" -> {
                RestaurantProvider.open(player, plugin);
                player.sendMessage("§6[레스토랑] §f레스토랑에 오신 것을 환영합니다!");
            }
            case "aquarium", "아쿠아리움" -> {
                AquariumProvider.open(player, plugin);
                player.sendMessage("§b[아쿠아리움] §f물고기 납품소에 오신 것을 환영합니다!");
            }
            case "mapworkshop", "지도제작소", "map" -> {
                MapWorkshopProvider.open(player, plugin);
                player.sendMessage("§e[지도 제작소] §f지도 제작소에 오신 것을 환영합니다!");
            }
            default -> {
                sender.sendMessage("§c알 수 없는 명령어입니다.");
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
