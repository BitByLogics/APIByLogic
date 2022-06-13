package net.justugh.japi.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Placeholder implements StringModifier {

    private final String key, value;

    @Override
    public String modify(String string) {
        return string.replace(key, value);
    }

}
