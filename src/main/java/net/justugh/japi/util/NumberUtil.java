package net.justugh.japi.util;

import java.math.BigDecimal;

public class NumberUtil {

    /**
     * Truncate decimal to set decimal places.
     *
     * @param number The decimal to be truncated
     * @param places The amount of places to truncate to
     * @return Truncated decimal
     */
    public static double truncateDecimal(double number, int places) {
        return new BigDecimal(number).setScale(places, number > 0 ? BigDecimal.ROUND_FLOOR : BigDecimal.ROUND_CEILING).doubleValue();
    }

}
