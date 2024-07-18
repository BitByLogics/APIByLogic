package net.bitbylogic.apibylogic.database.hikari;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter
public class HikariAPI {

    private final HikariDataSource hikari;

    public HikariAPI(String address, String database, String port, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(Duration.ofSeconds(30).toMillis());
        config.setDataSourceClassName("com.mysql.cj.jdbc.MysqlDataSource");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("serverName", address);
        config.addDataSourceProperty("port", port);
        config.addDataSourceProperty("databaseName", database);
        config.addDataSourceProperty("user", username);
        config.addDataSourceProperty("password", password);

        hikari = new HikariDataSource(config);
    }

    public HikariAPI(File databaseFile) {
        if (!databaseFile.exists()) {
            try {
                databaseFile.createNewFile();
            } catch (IOException e) {
                System.out.println("(HikariAPI): Unable to find database file!");
                hikari = null;
                return;
            }
        }

        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(Duration.ofSeconds(30).toMillis());
        config.setDataSourceClassName("jdbc:sqlite:" + databaseFile);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        hikari = new HikariDataSource(config);
    }

    public void executeStatement(String query, Object... arguments) {
        executeStatement(query, null, arguments);
    }

    public void executeStatement(String query, Consumer<ResultSet> consumer, Object... arguments) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = hikari.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    int index = 1;
                    for (Object argument : arguments) {
                        statement.setObject(index++, argument);
                    }

                    statement.executeUpdate();
                    try (ResultSet result = statement.getGeneratedKeys()) {
                        consumer.accept(result);
                    }
                }
            } catch (SQLException e) {
                // Printed below
            }
        }).handle((unused, e) -> {
            if (e == null) {
                return null;
            }

            System.out.println("(HikariAPI): Issue executing statement: " + query);
            e.printStackTrace();
            return null;
        });
    }

    public void executeQuery(String query, Consumer<ResultSet> consumer, Object... arguments) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = hikari.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                    int index = 1;
                    for (Object argument : arguments) {
                        statement.setObject(index++, argument);
                    }

                    try (ResultSet result = statement.executeQuery()) {
                        if (consumer == null) {
                            return;
                        }

                        consumer.accept(result);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).handle((unused, e) -> {
            if (e == null) {
                return null;
            }

            System.out.println("(HikariAPI): Issue executing statement: " + query);
            e.printStackTrace();
            return null;
        });
    }

}
