package com.dreamwork.core.economy;

import com.dreamwork.core.DreamWorkCore;
import com.dreamwork.core.model.UserData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EconomyCommand implements CommandExecutor, TabCompleter {

    private final DreamWorkCore plugin;

    public EconomyCommand(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            // 자신의 소지금 확인
            if (!(sender instanceof Player player)) {
                plugin.sendMessage(sender, "&c플레이어만 사용할 수 있습니다.");
                return true;
            }
            showBalance(player, player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help", "?" -> {
                plugin.sendMessage(sender, "&6=== DreamWork 경제 시스템 ===");
                plugin.sendMessage(sender, "&e/money &7- 내 소지금 확인");
                plugin.sendMessage(sender, "&e/money pay <플레이어> <금액> &7- 돈 보내기");
                if (sender.hasPermission("dreamwork.admin")) {
                    plugin.sendMessage(sender, "&c/money give <플레이어> <금액> &7- 돈 지급 (관리자)");
                    plugin.sendMessage(sender, "&c/money set <플레이어> <금액> &7- 돈 설정 (관리자)");
                }
            }
            case "pay" -> {
                if (!(sender instanceof Player player)) {
                    plugin.sendMessage(sender, "&c플레이어만 사용할 수 있습니다.");
                    return true;
                }
                if (args.length < 3) {
                    plugin.sendMessage(sender, "&c사용법: /money pay <플레이어> <금액>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.sendMessage(sender, "&c플레이어를 찾을 수 없습니다.");
                    return true;
                }
                if (target.getUniqueId().equals(player.getUniqueId())) {
                    plugin.sendMessage(sender, "&c자신에게 보낼 수 없습니다.");
                    return true;
                }

                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    plugin.sendMessage(sender, "&c올바른 숫자를 입력해주세요.");
                    return true;
                }

                if (amount <= 0) {
                    plugin.sendMessage(sender, "&c0보다 큰 금액을 입력해주세요.");
                    return true;
                }

                UserData senderData = plugin.getStorageManager().getUserData(player.getUniqueId());
                if (senderData == null || !senderData.hasMoney(amount)) {
                    plugin.sendMessage(sender, "&c소지금이 부족합니다.");
                    return true;
                }

                UserData targetData = plugin.getStorageManager().getUserData(target.getUniqueId());
                if (targetData == null) {
                    plugin.sendMessage(sender, "&c상대방 데이터를 불러올 수 없습니다.");
                    return true;
                }

                // 송금 처리
                senderData.removeMoney(amount);
                targetData.addMoney(amount);

                plugin.sendMessage(sender,
                        "&e" + target.getName() + "&a님에게 " + String.format("%.0f", amount) + "D를 보냈습니다.");
                plugin.sendMessage(target,
                        "&e" + sender.getName() + "&a님으로부터 " + String.format("%.0f", amount) + "D를 받았습니다.");
            }
            case "give" -> {
                if (!sender.hasPermission("dreamwork.admin")) {
                    plugin.sendMessage(sender, plugin.getMessage("no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    plugin.sendMessage(sender, "&c사용법: /money give <플레이어> <금액>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.sendMessage(sender, "&c플레이어를 찾을 수 없습니다.");
                    return true;
                }
                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    plugin.sendMessage(sender, "&c올바른 숫자를 입력해주세요.");
                    return true;
                }

                UserData targetData = plugin.getStorageManager().getUserData(target.getUniqueId());
                if (targetData != null) {
                    targetData.addMoney(amount);
                    plugin.sendMessage(sender, "&e" + target.getName() + "&a님에게 " + amount + "D 지급 완료.");
                    plugin.sendMessage(target, "&a관리자로부터 " + amount + "D를 받았습니다.");
                }
            }
            case "set" -> {
                if (!sender.hasPermission("dreamwork.admin")) {
                    plugin.sendMessage(sender, plugin.getMessage("no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    plugin.sendMessage(sender, "&c사용법: /money set <플레이어> <금액>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.sendMessage(sender, "&c플레이어를 찾을 수 없습니다.");
                    return true;
                }
                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    plugin.sendMessage(sender, "&c올바른 숫자를 입력해주세요.");
                    return true;
                }

                UserData targetData = plugin.getStorageManager().getUserData(target.getUniqueId());
                if (targetData != null) {
                    targetData.setMoney(amount);
                    plugin.sendMessage(sender, "&e" + target.getName() + "&a님의 소지금을 " + amount + "D로 설정했습니다.");
                }
            }
            default -> {
                // /money <player> 로 타인 소지금 확인 (관리자 권한 필요할 수도 있으나 일단 허용)
                Player target = Bukkit.getPlayer(subCommand);
                if (target != null) {
                    showBalance(sender, target);
                } else {
                    plugin.sendMessage(sender, "&c알 수 없는 명령어입니다. /money help");
                }
            }
        }
        return true;
    }

    private void showBalance(CommandSender viewer, Player target) {
        UserData data = plugin.getStorageManager().getUserData(target.getUniqueId());
        double balance = (data != null) ? data.getBalance() : 0.0;

        if (viewer.equals(target)) {
            plugin.sendMessage(viewer, "&6내 소지금: &e" + String.format("%.0f", balance) + "D");
        } else {
            plugin.sendMessage(viewer, "&e" + target.getName() + "&6님의 소지금: &e" + String.format("%.0f", balance) + "D");
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("pay");
            completions.add("help");
            if (sender.hasPermission("dreamwork.admin")) {
                completions.add("give");
                completions.add("set");
            }
            return completions;
        }
        return null;
    }
}
