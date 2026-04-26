package com.cf.analysis.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Singleton DatabaseConnection sử dụng HikariCP connection pool.
 * Tự động chạy Flyway migrations khi khởi tạo.
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;
    private final HikariDataSource dataSource;

    private DatabaseConnection() {
        HikariConfig config = new HikariConfig();

        // Lấy thông tin từ environment variables hoặc sử dụng default
        String dbUrl = getEnvOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/codeforces_analysis");
        String dbUsername = getEnvOrDefault("DB_USERNAME", "postgres");
        String dbPassword = getEnvOrDefault("DB_PASSWORD", "postgres");

        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);

        // HikariCP configuration
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);
        config.setMaxLifetime(1800000); // 30 minutes

        // PostgreSQL optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

        // Chạy Flyway migrations
        runMigrations();
    }

    /**
     * Lấy singleton instance của DatabaseConnection.
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Lấy connection từ pool.
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Chạy Flyway migrations tự động.
     */
    private void runMigrations() {
        try {
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

            MigrateResult result = flyway.migrate();

            System.out.println("✅ Flyway migrations completed successfully!");
            System.out.println("   Migrations executed: " + result.migrationsExecuted);
            System.out.println("   Target version: " + (result.targetSchemaVersion != null ? result.targetSchemaVersion : "latest"));

        } catch (Exception e) {
            System.err.println("❌ Flyway migration failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database migration failed", e);
        }
    }

    /**
     * Đóng connection pool khi shutdown.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Database connection pool closed.");
        }
    }

    /**
     * Lấy giá trị từ environment variable hoặc trả về default.
     */
    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Kiểm tra connection có hoạt động không.
     */
    public boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Lấy thông tin về connection pool.
     */
    public String getPoolStats() {
        return String.format(
            "HikariCP Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }
}
