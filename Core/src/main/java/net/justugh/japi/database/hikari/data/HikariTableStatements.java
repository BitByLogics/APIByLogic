package net.justugh.japi.database.hikari.data;

public abstract class HikariTableStatements {

    public abstract String getTableCreateStatement();
    public abstract String getDataCreateStatement();
    public abstract String getDataStatement();
    public abstract String getSaveStatement();
    public abstract String getDeleteStatement();

    public static HikariTableStatements generateFromHikariObject(Class<? extends HikariObject> object) {
        return null;
    }

}
