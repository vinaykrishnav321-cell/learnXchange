package com.learnxchange.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Pooled JDBC connections, configured from environment variables (or -D system properties):
 *   DB_URL       e.g. jdbc:mysql://host:3306/learnxchange?sslMode=REQUIRED
 *   DB_USER, DB_PASSWORD
 *   DB_POOL_SIZE (optional, default 5)
 */
public final class DBConnection {
    private static volatile HikariDataSource dataSource;

    private DBConnection() { }

    public static Connection get() throws SQLException {
        HikariDataSource ds = dataSource;
        if (ds == null) {
            synchronized (DBConnection.class) {
                if (dataSource == null) dataSource = create();
                ds = dataSource;
            }
        }
        return ds.getConnection();
    }

    private static HikariDataSource create() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(required("DB_URL"));
        cfg.setUsername(required("DB_USER"));
        cfg.setPassword(required("DB_PASSWORD"));
        cfg.setMaximumPoolSize(Integer.parseInt(optional("DB_POOL_SIZE", "5")));
        cfg.setConnectionTimeout(15_000);
        cfg.setPoolName("learnxchange");
        return new HikariDataSource(cfg);
    }

    private static String optional(String key, String fallback) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) v = System.getProperty(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static String required(String key) {
        String v = optional(key, null);
        if (v == null) throw new IllegalStateException("Missing configuration: set the " + key + " environment variable.");
        return v;
    }
}
