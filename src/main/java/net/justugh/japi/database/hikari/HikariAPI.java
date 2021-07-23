package net.justugh.japi.database.hikari;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

public class HikariAPI {

    private final HikariDataSource hikari;

    public HikariAPI(ConfigurationSection section) {
        String address = section.getString("Address");
        String database = section.getString("Database");
        String port = section.getString("Port");
        String username = section.getString("Username");
        String password = section.getString("Password");

        hikari = new HikariDataSource();
        hikari.setMaximumPoolSize(10);
        hikari.setDataSourceClassName("com.mysql.cj.jdbc.MysqlDataSource");
        hikari.addDataSourceProperty("serverName", address);
        hikari.addDataSourceProperty("port", port);
        hikari.addDataSourceProperty("databaseName", database);
        hikari.addDataSourceProperty("user", username);
        hikari.addDataSourceProperty("password", password);
    }

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
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = hikari.getConnection();
            statement = connection.prepareStatement(query);

            int index = 1;
            for (Object argument : arguments) {
                statement.setObject(index++, argument);
            }

            statement.execute();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void executeQuery(String query, Consumer<ResultSet> consumer, Object... arguments) {
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = hikari.getConnection();
            statement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

            int index = 1;
            for (Object argument : arguments) {
                statement.setObject(index++, argument);
            }

            ResultSet result = statement.executeQuery();

            consumer.accept(result);

            result.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
