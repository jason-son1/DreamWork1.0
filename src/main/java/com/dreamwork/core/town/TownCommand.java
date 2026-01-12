package com.dreamwork.core.town;

import com.dreamwork.core.DreamWorkCore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 마을 시스템 명령어 처리 (/town, /마을)
 * 
 * <p>
 * 기초적인 마을 생성, 가입, 탈퇴 및 마을 은행 기능을 제공합니다.
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class TownCommand implements CommandExecutor, TabCompleter {

    private final DreamWorkCore plugin;

    public TownCommand(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "&c플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help", "도움말", "?" -> sendHelp(player);
            case "create", "생성" -> createTown(player, args);
            // case "join", "가입" -> joinTown(player, args); // 추후 구현
            // case "leave", "탈퇴" -> leaveTown(player, args); // 추후 구현
            case "bank", "은행" -> handleBank(player, args);
            default -> sendMessage(player, "&c알 수 없는 명령어입니다. /town help를 참고하세요.");
        }

        return true;
    }

    private void sendHelp(Player player) {
        sendMessage(player, "&6=== &eDreamWork Town &6===");
        sendMessage(player, "&e/town create <이름> &7- 새로운 마을을 생성합니다. (비용: 10,000D)");
        // sendMessage(player, "&e/town join <이름> &7- 마을에 가입 신청을 보냅니다.");
        sendMessage(player, "&e/town bank &7- 마을 은행 잔고를 확인합니다.");
        sendMessage(player, "&e/town bank deposit <금액> &7- 마을 은행에 돈을 입금합니다.");
        sendMessage(player, "&e/town bank withdraw <금액> &7- 마을 은행에서 돈을 출금합니다. (촌장 전용)");
    }

    private void createTown(Player player, String[] args) {
        if (args.length < 2) {
            sendMessage(player, "&c사용법: /town create <마을이름>");
            return;
        }

        String townName = args[1];
        if (townName.length() > 16) {
            sendMessage(player, "&c마을 이름은 16자를 초과할 수 없습니다.");
            return;
        }

        // 비용 검사
        Economy econ = DreamWorkCore.getEconomy();
        if (econ == null) {
            sendMessage(player, "&c경제 시스템이 비활성화되어 있습니다.");
            return;
        }

        double cost = 10000.0;
        if (!econ.has(player, cost)) {
            sendMessage(player, "&c마을 생성 비용이 부족합니다. (필요: " + econ.format(cost) + ")");
            return;
        }

        // DB 작업 (비동기)
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // 중복 검사
                try (PreparedStatement checkStmt = conn
                        .prepareStatement("SELECT 1 FROM dw_towns WHERE town_name = ?")) {
                    checkStmt.setString(1, townName);
                    if (checkStmt.executeQuery().next()) {
                        sendMessage(player, "&c이미 존재하는 마을 이름입니다.");
                        return;
                    }
                }

                // 마을 생성
                String sql = "INSERT INTO dw_towns (town_name, owner_uuid, bank_balance) VALUES (?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                    insertStmt.setString(1, townName);
                    insertStmt.setString(2, player.getUniqueId().toString());
                    insertStmt.setDouble(3, 0.0); // 초기 자금 0
                    insertStmt.executeUpdate();
                }

                // 비용 차감 및 메시지 (동기)
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    econ.withdrawPlayer(player, cost);
                    sendMessage(player, "&a성공적으로 마을 '" + townName + "'을(를) 생성했습니다!");
                    sendMessage(player, "&7(생성 비용 " + econ.format(cost) + " 차감됨)");
                });

            } catch (SQLException e) {
                plugin.getLogger().severe("마을 생성 실패: " + e.getMessage());
                sendMessage(player, "&c마을 생성 중 내부 오류가 발생했습니다.");
            }
        });
    }

    private void handleBank(Player player, String[] args) {
        // args[0] = "bank", args[1] = action, args[2] = amount
        if (args.length < 2) {
            // 잔고 확인 (내 마을)
            checkBankBalance(player);
            return;
        }

        String action = args[1].toLowerCase();

        if (args.length < 3) {
            sendMessage(player, "&c사용법: /town bank <deposit|withdraw> <금액>");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sendMessage(player, "&c유효한 금액을 입력해주세요.");
            return;
        }

        if (action.equals("deposit") || action.equals("입금")) {
            depositBank(player, amount);
        } else if (action.equals("withdraw") || action.equals("출금")) {
            withdrawBank(player, amount);
        } else {
            sendMessage(player, "&c알 수 없는 은행 업무입니다.");
        }
    }

    private void checkBankBalance(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Integer townId = getPlayerTownId(player);

            if (townId == null) {
                sendMessage(player, "&c가입된 마을이 없습니다.");
                return;
            }

            double balance = getTownBalance(townId);
            sendMessage(player, "&6[마을 은행] &f현재 잔고: &e" + DreamWorkCore.getEconomy().format(balance));
        });
    }

    private void depositBank(Player player, double amount) {
        Economy econ = DreamWorkCore.getEconomy();
        if (!econ.has(player, amount)) {
            sendMessage(player, "&c소지금이 부족합니다.");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Integer townId = getPlayerTownId(player);
            if (townId == null) {
                sendMessage(player, "&c가입된 마을이 없습니다.");
                return;
            }

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // 입금 처리
                String sql = "UPDATE dw_towns SET bank_balance = bank_balance + ? WHERE town_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setDouble(1, amount);
                    stmt.setInt(2, townId);
                    stmt.executeUpdate();
                }

                // 플레이어 돈 차감 (동기)
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    econ.withdrawPlayer(player, amount);
                    sendMessage(player, "&a마을 은행에 " + econ.format(amount) + "을(를) 입금했습니다.");
                });

            } catch (SQLException e) {
                e.printStackTrace();
                sendMessage(player, "&c입금 처리 중 오류가 발생했습니다.");
            }
        });
    }

    private void withdrawBank(Player player, double amount) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            TownInfo info = getPlayerTownInfo(player);
            if (info == null) {
                sendMessage(player, "&c가입된 마을이 없습니다.");
                return;
            }

            // 권한 체크 (촌장만)
            if (!info.ownerUuid.equals(player.getUniqueId().toString())) {
                sendMessage(player, "&c마을 촌장만 출금할 수 있습니다.");
                return;
            }

            if (info.balance < amount) {
                sendMessage(player, "&c마을 은행 잔고가 부족합니다.");
                return;
            }

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // 출금 처리
                String sql = "UPDATE dw_towns SET bank_balance = bank_balance - ? WHERE town_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setDouble(1, amount);
                    stmt.setInt(2, info.id);
                    stmt.executeUpdate();
                }

                // 플레이어 돈 지급 (동기)
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Economy econ = DreamWorkCore.getEconomy();
                    econ.depositPlayer(player, amount);
                    sendMessage(player, "&e마을 은행에서 " + econ.format(amount) + "을(를) 출금했습니다.");
                });

            } catch (SQLException e) {
                e.printStackTrace();
                sendMessage(player, "&c출금 처리 중 오류가 발생했습니다.");
            }
        });
    }

    // --- Helper Methods (DB Access Helpers - should be in TownManager later) ---

    private record TownInfo(int id, String name, String ownerUuid, double balance) {
    }

    private Integer getPlayerTownId(Player player) {
        // 현재는 '소유자' 기준으로만 찾음 (가입 시스템 미구현)
        try (Connection conn = plugin.getDatabaseManager().getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT town_id FROM dw_towns WHERE owner_uuid = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getInt("town_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private TownInfo getPlayerTownInfo(Player player) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM dw_towns WHERE owner_uuid = ?")) {
            stmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new TownInfo(
                            rs.getInt("town_id"),
                            rs.getString("town_name"),
                            rs.getString("owner_uuid"),
                            rs.getDouble("bank_balance"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private double getTownBalance(int townId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT bank_balance FROM dw_towns WHERE town_id = ?")) {
            stmt.setInt(1, townId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getDouble("bank_balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private void sendMessage(CommandSender sender, String message) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6Town&8] &f");
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message);
        sender.sendMessage(component);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "bank", "help");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("bank")) {
            return Arrays.asList("deposit", "withdraw");
        }
        return Collections.emptyList();
    }
}
