package net.bitbylogic.apibylogic.database.hikari;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.apibylogic.database.hikari.data.HikariObject;
import net.bitbylogic.apibylogic.database.hikari.data.statements.HikariStatements;
import net.bitbylogic.apibylogic.database.hikari.data.statements.SQLStatements;
import net.bitbylogic.apibylogic.database.hikari.data.statements.SQLiteStatements;
import net.bitbylogic.apibylogic.util.reflection.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;

@RequiredArgsConstructor
public enum SQLType {

    MYSQL(SQLStatements.class),
    SQLITE(SQLiteStatements.class);

    private final Class<? extends HikariStatements> statementClass;

    public <O extends HikariObject> HikariStatements<O> getStatements(@NonNull String table) {
        try {
            return ReflectionUtil.findAndCallConstructor(statementClass, table);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

}
