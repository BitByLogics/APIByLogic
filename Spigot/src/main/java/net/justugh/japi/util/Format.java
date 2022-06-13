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
     * @param string    The string being formatted.
     * @param modifiers The modifiers being applied.
     * @return The formatted string.
     */
    public static String format(String string, StringModifier... modifiers) {
        String formattedMessage = string;

        for (StringModifier modifier : modifiers) {
            formattedMessage = modifier.modify(formattedMessage);
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
     * @param target    The target being sent the message.
     * @param config    The configuration.
     * @param path      The message path.
     * @param modifiers The message modifiers.
     */
    public static void sendConfigMessage(CommandSender target, FileConfiguration config, String path, StringModifier... modifiers) {
        target.sendMessage(Format.format(config.getString(path), modifiers));
    }

}
