package net.justugh.japi.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Format {

    private static final Pattern hexPattern = Pattern.compile("%hex_(#.+?)%");

    /**
     * Format a string with color & placeholders.
     *
     * @param string       The string being formatted.
     * @param placeholders The placeholders being applied.
     * @return The formatted string.
     */
    public static String format(String string, Placeholder... placeholders) {
        String formattedMessage = string;

        for (Placeholder placeholder : placeholders) {
            formattedMessage = formattedMessage.replace(placeholder.getKey(), placeholder.getValue());
        }

        Matcher matcher = hexPattern.matcher(formattedMessage);
        while (matcher.find()) {
            formattedMessage = formattedMessage.replace(matcher.group(), ChatColor.of(matcher.group(1)).toString());
        }

        return ChatColor.translateAlternateColorCodes('&', formattedMessage);
    }


    /**
     * Send a command target a formatted
     * message from the provided configuration.
     *
     * @param target       The target being sent the message.
     * @param config       The configuration.
     * @param path         The message path.
     * @param placeholders The message placeholders.
     */
    public static void sendConfigMessage(CommandSender target, FileConfiguration config, String path, Placeholder... placeholders) {
        target.sendMessage(Format.format(config.getString(path), placeholders));
    }

}
