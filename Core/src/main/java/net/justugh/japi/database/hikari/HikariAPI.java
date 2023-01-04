package net.justugh.japi.database.hikari;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

public class HikariAPI {

    private final HikariDataSource hikari;

    public HikariAPI(String address, String database, String port, String username, String password) {
        hikari = new HikariDataSource();
        hikari.setMaximumPoolSize(10);
        hikari.setDataSourceClassName("com.mysql.cj.jdbc.MysqlDataSource");
        hikari.addDataSourceProperty("serverName", address);
        hikari.addDataSourceProperty("port", port);
        hikari.addDataSourceProperty("databaseName", database);
        hikari.addDataSourceProperty("user", username);
        hikari.addDataSourceProperty("password", password);
    }

    public void executeStatement(String query, Object... arguments) {
        executeStatement(query, null, arguments);
    }

    public void executeStatement(String query, Consumer<ResultSet> consumer, Object... arguments) {
        try (Connection connection = hikari.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
                int index = 1;
                for (Object argument : arguments) {
                    statement.setObject(index++, argument);
                }

                statement.executeUpdate();
                try (ResultSet result = statement.getGeneratedKeys()) {
                    if (consumer != null) {
                        if (result != null) {
                            result.next();
                        }

                        consumer.accept(result);
                    }
                }

                statement.close();
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void executeQuery(String query, Consumer<ResultSet> consumer, Object... arguments) {
        try (Connection connection = hikari.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                int index = 1;
                for (Object argument : arguments) {
                    statement.setObject(index++, argument);
                }

                try (ResultSet result = statement.executeQuery()) {
                    if (consumer != null) {
                        consumer.accept(result);
                    }
                }

                statement.close();
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
