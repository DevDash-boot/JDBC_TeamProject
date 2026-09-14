package com.tenco.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class util {
    private static final String URL = "jdbc:mysql://localhost:3306/convenience_store?serverTimezone=Asia/Seoul";
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");
    private static final HikariDataSource datasource;

    static {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);

        config.setConnectionTimeout(3000);

        datasource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
            return datasource.getConnection();
    }

    public static void close() {
        if(datasource.isClosed()) datasource.close();
    }
}
