package net.bitbylogic.apibylogic.database.hikari.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.bitbylogic.apibylogic.database.hikari.annotation.HikariStatementData;
import net.bitbylogic.apibylogic.util.reflection.NamedParameter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Getter
public class HikariColumnData {

    private final String fieldName;
    private final Class<?> fieldClass;
    private final String parentClassName;
    private final HikariStatementData statementData;
    private final List<String> parentObjectFields;

    @Setter
    private HikariStatementData foreignKeyData;
    @Setter
    private HikariTable<?> foreignTable;

    public String getColumnName() {
        if (statementData.columnName().isEmpty()) {
            return statementData.subClass() ? parentClassName + "_" + fieldName : fieldName;
        }

        return statementData.columnName();
    }

    public String getFormattedData() {
        if (foreignKeyData != null) {
            String dataType = foreignKeyData.dataType();

            if (fieldClass.isAssignableFrom(List.class) || fieldClass.isAssignableFrom(Map.class)) {
                dataType = "LONGTEXT";
            }

            return getColumnName() + " " + dataType + " " + (foreignKeyData.allowNull() ? "" : "NOT NULL") + (foreignKeyData.autoIncrement() ? " AUTO_INCREMENT" : "");
        }

        return getColumnName() + " " + statementData.dataType() + " " + (statementData.allowNull() ? "" : "NOT NULL") + (statementData.autoIncrement() ? " AUTO_INCREMENT" : "");
    }

    public NamedParameter asNamedParameter(@Nullable Object value) {
        return new NamedParameter(fieldName, fieldClass, value);
    }

}
