package net.bitbylogic.apibylogic.database.hikari.processor.impl;

import net.bitbylogic.apibylogic.database.hikari.processor.HikariFieldProcessor;

public class DefaultHikariFieldProcessor implements HikariFieldProcessor<Object> {

    @Override
    public Object parseToObject(Object fieldValue) {
        if (fieldValue instanceof Boolean) {
            return String.valueOf((Boolean) fieldValue ? 1 : 0);
        }

        return fieldValue.toString();
    }

    @Override
    public Object parseFromObject(Object object) {
        return object;
    }

}
