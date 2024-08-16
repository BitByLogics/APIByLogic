package net.bitbylogic.apibylogic.database.hikari.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.apibylogic.database.hikari.annotation.HikariStatementData;
import net.bitbylogic.apibylogic.util.reflection.NamedParameter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AllArgsConstructor
@Getter
public class HikariColumnData {

    private final String fieldName;
    private final Class<?> fieldClass;
    private final boolean list;
    private final String parentClassName;
    private final HikariStatementData statementData;
    private final List<String> parentObjectFields;

    public String getColumnName() {
        if (statementData.columnName().isEmpty()) {
            return statementData.subClass() ? parentClassName + "_" + fieldName : fieldName;
        }

        return statementData.columnName();
    }

    public String getFormattedData() {
        return getColumnName() + " " + statementData.dataType() + " " + (statementData.allowNull() ? "" : "NOT NULL") + (statementData.autoIncrement() ? " AUTO_INCREMENT" : "");
    }

    public NamedParameter asNamedParameter(@Nullable Object value) {
        return new NamedParameter(fieldName, fieldClass, value);
    }

}
