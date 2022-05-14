package net.justugh.japi.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringUtil {

    /**
     * Joins a String array starting from the specified
     * index using the specified delimiter.
     *
     * @param index     The index to start from.
     * @param array     The array to join.
     * @param delimiter The delimiter that separates each element.
     * @return The joined array as a single String.
     */
    public static String join(int index, String[] array, String delimiter) {
        if (index >= array.length) {
            throw new IndexOutOfBoundsException("Specified index is greater than array length");
        }

        String[] newArray = new String[array.length - index];

        for (int i = index; i < array.length; i++) {
            newArray[i - index] = array[i];
        }

        return String.join(delimiter, newArray);
    }

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

    /**
     * Calculate the time it takes to read a string
     * in seconds.
     *
     * @param string       The string to read
     * @param readingSpeed The speed of reading in wpm
     * @return A long of time in seconds
     */
    public static long calculateReadingTime(String string, int readingSpeed) {
        double numberOfWords = string.split(" ").length;
        double readingTime = (numberOfWords / readingSpeed);
        return (long) (readingTime * 60000);
    }

}
