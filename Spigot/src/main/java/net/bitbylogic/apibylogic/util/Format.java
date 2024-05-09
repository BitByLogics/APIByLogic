package net.bitbylogic.apibylogic.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Format strings with modifiers and placeholders.
 *
 * @deprecated use {@link net.bitbylogic.apibylogic.util.message.Messages#format(String, Placeholder...)}} instead
 */
@Deprecated(forRemoval = true)
public class Format {

    private static final Pattern hexPattern = Pattern.compile("(#[a-fA-F0-9]{6}|#[a-fA-F0-9]{3})");
    private static final Pattern hexPlaceholderPattern = Pattern.compile("%hex_(#.+?)%");

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

        Matcher matcher = hexPlaceholderPattern.matcher(formattedMessage);
        while (matcher.find()) {
            formattedMessage = formattedMessage.replace(matcher.group(), ChatColor.of(matcher.group(1)).toString());
        }

        Matcher hexMatcher = hexPattern.matcher(formattedMessage);
        while (hexMatcher.find()) {
            formattedMessage = formattedMessage.replace(hexMatcher.group(), ChatColor.of(hexMatcher.group()).toString());
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

    public static String[] getPagedList(String header, List<String> data, int page) {
        List<String> text = new ArrayList<>();

        int pages = data.size() / 10.0d % 1 == 0 ? data.size() / 10 : data.size() / 10 + 1;
        int lastPossibleItem = data.size();

        if (page == 0 || page > pages) {
            text.add(format("&cInvalid page!"));
            return text.toArray(new String[]{});
        }

        int startingItem = (page * 10) - 10;
        int lastItem = Math.min(startingItem + 10, lastPossibleItem);
        text.add(format(header));

        for (int i = startingItem; i < lastItem; i++) {
            String item = data.get(i);
            text.add(format("&8| &8» &e" + item));
        }

        text.add(format(String.format("⤷ &7(Page: %s/%s)", page, pages)));
        return text.toArray(new String[]{});
    }

}
