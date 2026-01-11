package com.dreamwork.core.database;

import com.dreamwork.core.DreamWorkCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final DreamWorkCore plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        String type = plugin.getConfig().getString("database.type", "SQLITE");
        HikariConfig config = new HikariConfig();

        if (type.equalsIgnoreCase("MYSQL")) {
            config.setJdbcUrl("jdbc:mysql://" +
                    plugin.getConfig().getString("database.host", "localhost") + ":" +
                    plugin.getConfig().getString("database.port", "3306") + "/" +
                    plugin.getConfig().getString("database.database", "minecraft"));
            config.setUsername(plugin.getConfig().getString("database.username", "root"));
            config.setPassword(plugin.getConfig().getString("database.password", ""));
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            // SQLite: 내부 파일 DB
            File folder = plugin.getDataFolder();
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File dbFile = new File(folder, "database.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }

        config.setPoolName("DreamWork-Pool");
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 10));
        config.setConnectionTimeout(plugin.getConfig().getLong("database.connection-timeout", 30000));

        this.dataSource = new HikariDataSource(config);

        createTables();
    }

    private void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // 유저 데이터 테이블 (JSON으로 퀘스트 데이터 저장)
            String userTable = "CREATE TABLE IF NOT EXISTS dw_users (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(16)," +
                    "job_id VARCHAR(32)," +
                    "job_level INT," +
                    "job_exp DOUBLE," +
                    "str INT, dex INT, con INT, intel INT, luk INT, stat_points INT," +
                    "last_daily_reset TEXT," +
                    "quest_data TEXT," +
                    "current_mana DOUBLE" +
                    ")";

            stmt.execute(userTable);

        } catch (SQLException e) {
            plugin.getLogger().severe("데이터베이스 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized.");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
