package com.cf.analysis.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import java.io.*;
import java.sql.*;
import java.util.Properties;

/**
 * Singleton class quản lý kết nối đến PostgreSQL.
 *
 * SINGLETON PATTERN: Toàn bộ ứng dụng dùng chung MỘT instance duy nhất.
 * Điều này đảm bảo chỉ có một connection pool và tránh tạo quá nhiều connection.
 *
 * Tích hợp Flyway Migration:
 * - Khi gọi runMigrations(), Flyway sẽ tự động:
 *   1. Đọc tất cả file V*__*.sql trong classpath:db/migration
 *   2. So sánh với bảng flyway_schema_history để biết cái nào chưa chạy
 *   3. Apply theo thứ tự version tăng dần
 * - An toàn khi gọi nhiều lần: Flyway bỏ qua migration đã chạy
 *
 * Cách dùng từ bất kỳ class nào:
 *   Connection conn = DatabaseConnection.getInstance().getConnection();
 */
public class DatabaseConnection {

    // File lưu thông tin kết nối DB, tại thư mục chạy ứng dụng
    private static final String CONFIG_FILE = "db-config.properties";

    // Instance duy nhất của class này (Singleton)
    private static DatabaseConnection instance;

    // Đối tượng connection đến PostgreSQL
    private Connection connection;

    // Thông tin kết nối (có giá trị mặc định)
    private String host     = "localhost";
    private String port     = "5432";
    private String database = "codeforces_analysis";
    private String username = "postgres";
    private String password = "";

    // Constructor private: ngăn việc tạo instance từ bên ngoài
    private DatabaseConnection() {
        loadConfig(); // Đọc config đã lưu từ lần trước nếu có
    }

    /**
     * Lấy instance duy nhất. Thread-safe với synchronized.
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Lấy Connection, tự động kết nối lại nếu đã đóng.
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    /**
     * Kết nối đến PostgreSQL với cấu hình hiện tại.
     * @throws SQLException nếu không kết nối được
     */
    public void connect() throws SQLException {
        String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        props.setProperty("connectTimeout", "10");
        props.setProperty("socketTimeout", "30");
        connection = DriverManager.getConnection(url, props);
        System.out.println("✅ Đã kết nối PostgreSQL: " + url);
    }

    /**
     * Kiểm tra kết nối. Trả về true nếu thành công.
     */
    public boolean testConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            connection = null;
            connect();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Test connection thất bại: " + e.getMessage());
            return false;
        }
    }

    /**
     * Chạy Flyway migrations để tạo/cập nhật schema.
     *
     * Flyway sẽ:
     * 1. Tạo bảng flyway_schema_history (nếu chưa có) để theo dõi migrations
     * 2. Quét classpath:db/migration tìm các file V*.sql
     * 3. Chỉ apply những migration chưa có trong flyway_schema_history
     * 4. Validate checksum của migrations đã apply (phát hiện file bị sửa)
     *
     * @throws Exception nếu migration thất bại hoặc DB chưa kết nối
     */
    public void runMigrations() throws Exception {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);

        // Cấu hình Flyway
        Flyway flyway = Flyway.configure()
            .dataSource(jdbcUrl, username, password)
            .locations("classpath:db/migration")       // Tìm file trong src/main/resources/db/migration
            .baselineOnMigrate(true)                   // Cho phép migrate DB chưa có lịch sử
            .baselineVersion("0")                      // Baseline version (trước V1)
            .validateOnMigrate(true)                   // Validate checksum
            .encoding("UTF-8")                         // Encoding file SQL
            .load();

        // Chạy migration và lấy kết quả
        MigrateResult result = flyway.migrate();

        System.out.println("✅ Flyway migration hoàn tất!");
        System.out.println("   - Migrations applied: " + result.migrationsExecuted);
        System.out.println("   - Schema version: " + result.targetSchemaVersion);

        // Log chi tiết từng migration đã apply trong lần này
        result.migrations.forEach(m ->
            System.out.printf("   [V%s] %s%n", m.version, m.description)
        );
    }

    /**
     * Lấy thông tin Flyway migration hiện tại (dùng để debug).
     * @return Số migrations đã apply, hoặc -1 nếu lỗi.
     */
    public int getMigrationCount() {
        try {
            String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
            Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();

            return (int) java.util.Arrays.stream(flyway.info().applied())
                .filter(m -> m.getState().isApplied())
                .count();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Cập nhật thông tin kết nối mới và reset connection.
     * Gọi từ SettingsPanel khi user thay đổi cấu hình.
     */
    public void updateConfig(String host, String port, String database,
                             String username, String password) {
        this.host     = host;
        this.port     = port;
        this.database = database;
        this.username = username;
        this.password = password;
        saveConfig();

        // Đóng connection cũ để force kết nối lại với config mới
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
        connection = null;
    }

    // ==================== Config File ====================

    private void loadConfig() {
        // Đọc từ biến môi trường của hệ điều hành trước (chủ yếu dùng cho Docker)
        host     = System.getenv().getOrDefault("DB_HOST", "localhost");
        port     = System.getenv().getOrDefault("DB_PORT", "5432");
        database = System.getenv().getOrDefault("DB_NAME", "codeforces_analysis");
        username = System.getenv().getOrDefault("DB_USER", "postgres");
        password = System.getenv().getOrDefault("DB_PASSWORD", "");

        File f = new File(CONFIG_FILE);
        if (!f.exists()) return;

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(f)) {
            // Nếu có file db-config.properties thì ưu tiên ghi đè
            props.load(fis);
            host     = props.getProperty("host",     host);
            port     = props.getProperty("port",     port);
            database = props.getProperty("database", database);
            username = props.getProperty("username", username);
            password = props.getProperty("password", password);
        } catch (IOException e) {
            System.err.println("Không đọc được db-config.properties: " + e.getMessage());
        }
    }

    private void saveConfig() {
        Properties props = new Properties();
        props.setProperty("host",     host);
        props.setProperty("port",     port);
        props.setProperty("database", database);
        props.setProperty("username", username);
        props.setProperty("password", password);

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Codeforces Examination Analysis - DB Config");
        } catch (IOException e) {
            System.err.println("Không lưu được db-config.properties: " + e.getMessage());
        }
    }

    // ==================== Getters ====================
    public String getHost()     { return host; }
    public String getPort()     { return port; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
