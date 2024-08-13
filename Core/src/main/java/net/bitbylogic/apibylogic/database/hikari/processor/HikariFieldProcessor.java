package net.bitbylogic.apibylogic.database.hikari.processor;

public interface HikariFieldProcessor<O> {

    Object parseToObject(O object);

    O parseFromObject(Object object);

}
