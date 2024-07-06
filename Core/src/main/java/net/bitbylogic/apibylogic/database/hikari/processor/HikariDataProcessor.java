package net.bitbylogic.apibylogic.database.hikari.processor;

public interface HikariDataProcessor<O> {

    String processObject(O object);

}
