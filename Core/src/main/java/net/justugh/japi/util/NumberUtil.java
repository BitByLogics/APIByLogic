package net.justugh.japi.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberUtil {

    /**
     * Truncate decimal to set decimal places.
     *
     * @param number The decimal to be truncated.
     * @param places The amount of places to truncate to.
     * @return Truncated decimal.
     */
    public static double truncateDecimal(double number, int places) {
        return new BigDecimal(number).setScale(places, number > 0 ? RoundingMode.FLOOR : RoundingMode.CEILING).doubleValue();
    }

    /**
     * Check if a string is a number.
     *
     * @param string The string to check.
     * @return Whether the specified string is a number.
     */
    public static boolean isNumber(String string) {
        if (string == null) {
            return false;
        }

        int length = string.length();
        if (length == 0) {
            return false;
        }

        int i = 0;
        if (string.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }

        for (; i < length; i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }

        return true;
    }

}
