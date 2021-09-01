package net.justugh.japi.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringUtil {

    /**
     * Capitalize each word in a string.
     * "mINecraft iS fun" -> "Minecraft Is Fun"
     *
     * @param string The string to capitalize
     * @return A capitalized variation of the string
     */
    public static String capitalize(String string) {
        return Stream.of(string.toLowerCase().trim().split("\\s"))
                .filter(word -> word.length() > 0)
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

}
