package com.dreamwork.core.town;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.gui.provider.ForgeProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 대장간 명령어 (/forge)
 * 
 * <p>
 * Plan 2.0 기준:
 * - 미확인 광석 감정
 * - 합금 제작
 * - 장비 수리/강화
 * </p>
 */
public class ForgeCommand implements CommandExecutor, TabCompleter {

    private final DreamWorkCore plugin;

    public ForgeCommand(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c[대장간] 플레이어만 사용할 수 있습니다.");
            return true;
        }

        // 대장간 GUI 열기
        ForgeProvider.open(player, plugin);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>();
    }
}
