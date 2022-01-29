package net.justugh.japi.database.hikari.data;

import lombok.Getter;
import net.justugh.japi.database.hikari.HikariAPI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
public abstract class HikariTable<O extends HikariObject> {

    private final HikariAPI hikariAPI;
    private final HikariTableStatements statements;
    private final List<O> data;

    public HikariTable(HikariAPI hikariAPI, HikariTableStatements statements) {
        this.hikariAPI = hikariAPI;
        this.statements = statements;
        data = new ArrayList<>();
        hikariAPI.executeStatement(statements.getTableCreateStatement());

        loadData();
    }

    private void loadData() {
        data.clear();

        hikariAPI.executeQuery(statements.getDataStatement(), result -> {
            if (result == null) {
                return;
            }

            try {
                while (result.next()) {
                    data.add(loadObject(result));
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void save(O object, Consumer<ResultSet> result) {
        hikariAPI.executeStatement(statements.getSaveStatement(), result, object.getDataObjects());
    }

    public void delete(O object) {
        hikariAPI.executeStatement(statements.getDeleteStatement(), object.getDataId());
    }

    public abstract O loadObject(ResultSet set) throws SQLException;

    public O getDataById(Object id) {
        return data.stream().filter(o -> o.getDataId() == id).findFirst().orElse(null);
    }

}
