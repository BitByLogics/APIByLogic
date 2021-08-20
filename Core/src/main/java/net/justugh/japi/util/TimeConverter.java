package net.justugh.japi.util;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeConverter {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern LETTER_PATTERN = Pattern.compile("[A-Za-z]");

    /**
     * Check if a string is a valid time string.
     *
     * @param string The string being checked.
     * @return Whether the specified string is valid.
     */
    public static boolean isTimeString(String string) {
        for(String split : string.split(" ")) {
            if(!split.matches("((\\d+)[A-Za-z])")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Convert a long to a human-readable time.
     *
     * @param time The time being converted.
     * @return A readable time string.
     */
    public static String convertToReadableTime(long time) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time);
        seconds = seconds - ((seconds / 60) * 60);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
        minutes = minutes - ((minutes / 60) * 60);
        long hours = TimeUnit.MILLISECONDS.toHours(time);
        hours = hours - ((hours / 24) * 24);
        long days = TimeUnit.MILLISECONDS.toDays(time);
        long months = days / 30;
        days = days - ((days / 30) * 30);
        long years = months / 12;
        months = months - ((months / 12) * 12);

        StringBuilder message = new StringBuilder();

        if(time == -1){message.append("Permanent");}
        if(years > 0){message.append(years).append("y ");}
        if(months > 0){message.append(months).append("m ");}
        if(days > 0) {message.append(days).append("d ");}
        if(hours > 0) {message.append(hours).append("h ");}
        if(minutes > 0) {message.append(minutes).append("m ");}
        if(seconds > 0) {message.append(seconds).append("s");}

        return message.toString().trim();
    }

    /**
     * Converts a string into a time value.
     *
     * @param combindedArgs The string being converted.
     * @return Converted time value.
     */
    public static long convert(String combindedArgs) {
        String[] args = combindedArgs.split(" ");
        long time = 0;

        for (String arg : args) {
            if(arg.matches("((\\d+)[A-Za-z])")) {
                Matcher numberMatcher = NUMBER_PATTERN.matcher(arg);
                numberMatcher.find();
                Matcher letterMatcher = LETTER_PATTERN.matcher(arg);
                letterMatcher.find();
                long tempTime = Long.valueOf(numberMatcher.group(0));
                TimeAbbreviation abbreviation = getAbbreviation(letterMatcher.group(0));
                time+= abbreviation.getTime(tempTime);
            }
        }

        return time;
    }

    /**
     * Get TimeAbbreviation from string.
     *
     * @param data The string being converted.
     * @return Matched TimeAbbreviation instance.
     */
    private static TimeAbbreviation getAbbreviation(String data) {
        for (TimeAbbreviation abbreviation : TimeAbbreviation.values()) {
            for (String prefix : abbreviation.getPrefix()) {
                if(data.equalsIgnoreCase(prefix)) {
                    return abbreviation;
                }
            }
        }

        return null;
    }

    public enum TimeAbbreviation {

        SECOND(1, "s", "second"),
        MINUTE(60, "m", "minute"),
        HOUR(3600, "h", "hour"),
        DAY(86400, "d", "day"),
        WEEK(604800, "w", "week"),
        MONTH(2592000, "mh", "month"),
        YEAR(31556952, "y", "year"),
        PERM(-1, "p");

        String[] prefix;
        long multiplier;

        TimeAbbreviation(long multiplier, String... prefix) {
            this.prefix = prefix;
            this.multiplier = multiplier;
        }

        public String[] getPrefix() {
            return prefix;
        }

        public long getTime(long time) {
            if(multiplier == -1) {
                return -1;
            }

            return (time * multiplier) * 1000;
        }

    }

}