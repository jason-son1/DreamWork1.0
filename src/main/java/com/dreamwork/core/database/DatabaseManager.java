package com.dreamwork.core.database;

import com.dreamwork.core.DreamWorkCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 데이터베이스 연결 관리자
 * <p>
 * HikariCP를 사용하여 SQLite 또는 MySQL 데이터베이스 연결을 관리합니다.
 * SQLite가 기본값이며, 소규모 서버에 권장됩니다. (명령.txt 지침 준수)
 * </p>
 * 
 * @author DreamWork Team
 * @since 1.0.0
 */
public class DatabaseManager {

    private final DreamWorkCore plugin;
    private HikariDataSource dataSource;
    private boolean usingSqlite = true;

    public DatabaseManager(DreamWorkCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 데이터베이스 연결을 초기화합니다.
     */
    public void initialize() {
        String type = plugin.getConfig().getString("database.type", "SQLITE");
        HikariConfig config = new HikariConfig();

        if (type.equalsIgnoreCase("MYSQL")) {
            // MySQL 설정 (선택적 - 대규모 서버용)
            config.setJdbcUrl("jdbc:mysql://" +
                    plugin.getConfig().getString("database.host", "localhost") + ":" +
                    plugin.getConfig().getString("database.port", "3306") + "/" +
                    plugin.getConfig().getString("database.database", "minecraft"));
            config.setUsername(plugin.getConfig().getString("database.username", "root"));
            config.setPassword(plugin.getConfig().getString("database.password", ""));
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            usingSqlite = false;
            plugin.getLogger().info("[Database] MySQL 모드로 연결합니다.");
        } else {
            // SQLite: 기본 내부 파일 DB (소규모 서버 권장)
            File folder = plugin.getDataFolder();
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File dbFile = new File(folder, "database.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            usingSqlite = true;
            plugin.getLogger().info("[Database] SQLite 모드로 연결합니다: " + dbFile.getAbsolutePath());
        }

        config.setPoolName("DreamWork-Pool");
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 10));
        config.setConnectionTimeout(plugin.getConfig().getLong("database.connection-timeout", 30000));

        this.dataSource = new HikariDataSource(config);

        createTables();
        migrateSchemaIfNeeded();
    }

    /**
     * 테이블을 생성합니다.
     */
    private void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // 유저 데이터 테이블 (다중 직업 구조)
            String userTable = """
                    CREATE TABLE IF NOT EXISTS dw_users (
                        uuid VARCHAR(36) PRIMARY KEY,
                        name VARCHAR(16),
                        job_data TEXT,
                        str INT DEFAULT 0,
                        dex INT DEFAULT 0,
                        con INT DEFAULT 0,
                        intel INT DEFAULT 0,
                        luk INT DEFAULT 0,
                        stat_points INT DEFAULT 0,
                        current_mana DOUBLE DEFAULT 100.0,
                        last_daily_reset TEXT,
                        quest_data TEXT,
                        phase1_data TEXT,
                        money DOUBLE DEFAULT 0.0
                    )
                    """;
            stmt.execute(userTable);

            // 마을 데이터 테이블 (Town System)
            String townTable = """
                    CREATE TABLE IF NOT EXISTS dw_towns (
                        town_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        town_name VARCHAR(32) UNIQUE,
                        owner_uuid VARCHAR(36),
                        bank_balance DOUBLE DEFAULT 0.0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

            // MySQL 호환성 (AUTOINCREMENT -> AUTO_INCREMENT)
            if (!usingSqlite) {
                townTable = townTable.replace("AUTOINCREMENT", "AUTO_INCREMENT");
            }

            stmt.execute(townTable);

            plugin.getLogger().info("[Database] 테이블 초기화 완료");

        } catch (SQLException e) {
            plugin.getLogger().severe("데이터베이스 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 구버전 스키마를 새 버전으로 마이그레이션합니다.
     * job_data, phase1_data 컬럼이 없으면 추가합니다.
     */
    private void migrateSchemaIfNeeded() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            // job_data 컬럼 존재 확인
            boolean hasJobData = checkColumnExists(meta, "job_data");

            if (!hasJobData) {
                // 구버전 → job_data 컬럼 추가
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE dw_users ADD COLUMN job_data TEXT");
                    plugin.getLogger().info("[Database] job_data 컬럼 추가 완료 (스키마 마이그레이션)");
                }
            }

            // phase1_data 컬럼 존재 확인 (신규)
            boolean hasPhase1Data = checkColumnExists(meta, "phase1_data");

            if (!hasPhase1Data) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE dw_users ADD COLUMN phase1_data TEXT");
                    plugin.getLogger().info("[Database] phase1_data 컬럼 추가 완료 (스키마 마이그레이션)");
                }
            }

            // money 컬럼 존재 확인 (Phase 2)
            boolean hasMoney = checkColumnExists(meta, "money");

            if (!hasMoney) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE dw_users ADD COLUMN money DOUBLE DEFAULT 0.0");
                    plugin.getLogger().info("[Database] money 컬럼 추가 완료 (스키마 마이그레이션)");
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("스키마 마이그레이션 확인 중 오류: " + e.getMessage());
        }
    }

    private boolean checkColumnExists(DatabaseMetaData meta, String columnName) throws SQLException {
        try (ResultSet rs = meta.getColumns(null, null, "dw_users", columnName)) {
            return rs.next();
        }
    }

    /**
     * 데이터베이스 연결을 가져옵니다.
     * 
     * @return Connection
     * @throws SQLException 연결 실패 시
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource가 초기화되지 않았습니다.");
        }
        return dataSource.getConnection();
    }

    /**
     * SQLite 사용 여부를 반환합니다.
     * 
     * @return SQLite 사용 시 true
     */
    public boolean isUsingSqlite() {
        return usingSqlite;
    }

    /**
     * 데이터베이스 연결을 종료합니다.
     */
    public void close() {
        if (dataSource != null) {
            dataSource.close();
            plugin.getLogger().info("[Database] 데이터베이스 연결 종료");
        }
    }
}
