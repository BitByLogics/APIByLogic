package net.bitbylogic.apibylogic.util.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
public enum FormatCode {

    COLOR(new String[]{"c"}, 0),
    HEX(new String[]{"h"}, 2),
    GRADIENT(new String[]{"g"}, 1),
    CENTER(new String[]{"cr"}, 4),
    CUSTOM_DATA(new String[]{"cd"}, 3);

    private final String[] aliases;

    @Getter
    private final int priority;

    public static FormatCode match(String string) {
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(string) || (c.aliases != null && Arrays.stream(c.aliases).anyMatch(a -> a.equalsIgnoreCase(string))))
                .findFirst().orElse(null);
    }

}
