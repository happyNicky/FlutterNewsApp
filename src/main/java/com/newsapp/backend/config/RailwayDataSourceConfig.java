package com.newsapp.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile("prod")
public class RailwayDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(RailwayDataSourceConfig.class);

    @Bean
    @Primary
    public DataSource dataSource() {
        String host = env("MYSQLHOST");
        String port = env("MYSQLPORT");
        String database = env("MYSQLDATABASE");
        String username = env("MYSQLUSER");
        String password = env("MYSQLPASSWORD");

        if (isBlank(host)) {
            MysqlUrlParts parsed = parseMysqlUrl(env("MYSQL_URL"));
            if (parsed != null) {
                host = parsed.host();
                port = parsed.port();
                database = parsed.database();
                username = parsed.username();
                password = parsed.password();
            }
        }

        if (isBlank(host)) {
            throw new IllegalStateException(
                    "Database not configured. On Railway: add a MySQL service and link it to this backend service.");
        }

        if (isBlank(port)) {
            port = "3306";
        }
        if (isBlank(database)) {
            database = "railway";
        }

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        log.info("Connecting to MySQL at {}:{}/{}", host, port, database);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password != null ? password : "");
        dataSource.setConnectionTimeout(30_000);
        return dataSource;
    }

    private static String env(String key) {
        String value = System.getenv(key);
        return value != null ? value.trim() : null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static MysqlUrlParts parseMysqlUrl(String mysqlUrl) {
        if (isBlank(mysqlUrl) || !mysqlUrl.startsWith("mysql://")) {
            return null;
        }

        try {
            URI uri = URI.create(mysqlUrl.replace("mysql://", "http://"));
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 3306;
            String database = uri.getPath() != null && uri.getPath().length() > 1
                    ? uri.getPath().substring(1)
                    : "railway";

            String userInfo = uri.getUserInfo();
            String username = null;
            String password = null;
            if (userInfo != null && !userInfo.isBlank()) {
                int separator = userInfo.indexOf(':');
                if (separator >= 0) {
                    username = decode(userInfo.substring(0, separator));
                    password = decode(userInfo.substring(separator + 1));
                } else {
                    username = decode(userInfo);
                }
            }

            return new MysqlUrlParts(host, String.valueOf(port), database, username, password);
        } catch (IllegalArgumentException ex) {
            log.warn("Could not parse MYSQL_URL: {}", ex.getMessage());
            return null;
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record MysqlUrlParts(
            String host,
            String port,
            String database,
            String username,
            String password
    ) {
    }
}
