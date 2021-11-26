package net.justugh.japi.database.hikari.data;

import java.util.List;

public abstract class HikariObject {

    public abstract List<Object> getDataObjects();

    public abstract Object getDataId();

}
