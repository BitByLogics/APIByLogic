package net.justugh.japi.util;

import java.util.ArrayList;
import java.util.List;

public class RichTextUtil {


    /**
     * Retrieve data from a string array.
     * <p>
     * Example:
     * <p>
     * Input: Testing! 123 | Wow
     * Output: ["Testing! 123", "Wow"]
     *
     * @param args       The string array.
     * @param startIndex Index to start from.
     * @return A String array of extracted data.
     */
    public static String[] getRichText(String[] args, int startIndex) {
        String combinedString = StringUtil.join(startIndex, args, " ").trim();
        List<String> newString = new ArrayList<>();

        for (String s : combinedString.split("\\|")) {
            newString.add(s.trim());
        }

        return newString.toArray(new String[]{});
    }

}
