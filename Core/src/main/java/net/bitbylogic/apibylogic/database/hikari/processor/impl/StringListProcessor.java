package net.bitbylogic.apibylogic.database.hikari.processor.impl;

import net.bitbylogic.apibylogic.database.hikari.processor.HikariFieldProcessor;
import net.bitbylogic.apibylogic.util.ListUtil;

import java.util.List;

public class StringListProcessor implements HikariFieldProcessor<List<String>> {

    @Override
    public Object parseToObject(List<String> o) {
        return ListUtil.listToString(o);
    }

    @Override
    public List<String> parseFromObject(Object o) {
        return (List<String>) ListUtil.stringToList((String) o);
    }

}
