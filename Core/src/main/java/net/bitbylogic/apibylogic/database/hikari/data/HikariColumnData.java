package net.bitbylogic.apibylogic.database.hikari.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.bitbylogic.apibylogic.database.hikari.annotation.HikariStatementData;
import net.bitbylogic.apibylogic.util.reflection.NamedParameter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Getter
public class HikariColumnData {

    private final Field field;
    private final String parentClassName;
    private final HikariStatementData statementData;
    private final List<String> parentObjectFields;

    @Setter
    private HikariStatementData foreignKeyData;
    @Setter
    private HikariTable<?> foreignTable;

    public String getColumnName() {
        if (statementData.columnName().isEmpty()) {
            return statementData.subClass() ? parentClassName + "_" + field.getName() : field.getName();
        }

        return statementData.columnName();
    }

    public NamedParameter asNamedParameter(@Nullable Object value) {
        return new NamedParameter(field.getName(), field.getType(), value);
    }

}
