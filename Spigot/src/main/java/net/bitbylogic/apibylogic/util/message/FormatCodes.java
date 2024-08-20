package net.bitbylogic.apibylogic.util.message;

import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public enum FormatCodes {

    COLOR(new String[]{"c"}),
    HEX(new String[]{"h"}),
    GRADIENT(new String[]{"g"}),
    CUSTOM_DATA(new String[]{"cd"});

    private final String[] aliases;

    public static FormatCodes match(String string) {
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(string) || (c.aliases != null && Arrays.stream(c.aliases).anyMatch(a -> a.equalsIgnoreCase(string))))
                .findFirst().orElse(null);
    }

}
