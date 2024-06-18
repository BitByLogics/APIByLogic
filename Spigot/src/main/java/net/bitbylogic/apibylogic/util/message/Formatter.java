package net.bitbylogic.apibylogic.util.message;

import lombok.Getter;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.database.redis.listener.ListenerComponent;
import net.bitbylogic.apibylogic.util.Placeholder;
import net.bitbylogic.apibylogic.util.StringModifier;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Formatter {

    public final static String RIGHT_ARROW = "»";
    public final static String DOT = "•";
    @Getter
    private static final List<StringModifier> globalModifiers = new ArrayList<>();
    private static final Pattern placeholderPattern = Pattern.compile("%.+?%");
    private static final Pattern formatPattern = Pattern.compile("<([a-zA-Z]+?)#(.+?)>");
    private static final Pattern hexColorExtractor = Pattern.compile("#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})");

    public static void registerGlobalModifier(StringModifier modifier) {
        globalModifiers.add(modifier);
    }

    public static String color(String message) {
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

        Matcher matcher = hexColorExtractor.matcher(coloredMessage);

        while (matcher.find()) {
            String hexColor = matcher.group();
            coloredMessage = coloredMessage.replace(hexColor, ChatColor.of(hexColor).toString());
        }

        return coloredMessage;
    }

    /**
     * Format a string with color & placeholders.
     *
     * @param message   The message.
     * @param modifiers The placeholders being applied.
     * @return The formatted string.
     */
    public static String format(String message, StringModifier... modifiers) {
        String formattedMessage = message;

        for (StringModifier modifier : modifiers) {
            formattedMessage = modifier.modify(formattedMessage);
        }

        for (StringModifier globalModifier : globalModifiers) {
            formattedMessage = globalModifier.modify(formattedMessage);
        }

        Matcher matcher = formatPattern.matcher(formattedMessage);

        while (matcher.find()) {
            FormatCodes code = FormatCodes.match(matcher.group(1));

            if (code == null) {
                continue;
            }

            switch (code) {
                case COLOR:
                    String color = LogicColor.getColor(matcher.group(2));

                    if (color == null) {
                        break;
                    }

                    formattedMessage = formattedMessage.replace(matcher.group(), color);
                    break;
                case HEX:
                    formattedMessage = formattedMessage.replace(matcher.group(), ChatColor.of(matcher.group(2)).toString());
                    break;
                default:
                    break;
            }
        }

        return color(formattedMessage);
    }

    public static String autoFormat(String message, String... replacements) {
        Matcher placeholderMatcher = placeholderPattern.matcher(message);

        int currentIndex = 0;
        while (placeholderMatcher.find()) {
            if (currentIndex > replacements.length) {
                break;
            }

            String placeholder = placeholderMatcher.group();
            message = message.replace(placeholder, replacements[currentIndex++]);
        }

        return format(message);
    }

    public static BaseComponent richFormat(String message, Object... replacements) {
        return TextComponent.fromLegacy(replace(message, replacements));
    }

    public static String replace(String message, Object... replacements) {
        String formattedMessage = message;

        for (Object replacement : replacements) {
            if (!formattedMessage.contains("%s")) {
                break;
            }

            formattedMessage = formattedMessage.replaceFirst("%s", Matcher.quoteReplacement(replacement.toString()));
        }

        return format(formattedMessage);
    }

    public static String command(String command, String description) {
        return replace(" <c#primary>/%s &8─ <c#secondary>%s", command, description);
    }

    public static BaseComponent richCommand(String command, String description) {
        BaseComponent component = richFormat(" <c#primary>/%s", command);
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(richFormat("<c#secondary>%s", description))));

        return component;
    }

    public static String main(String prefix, String message, Object... replacements) {
        return replace("<c#primary>&l%s &8%s <c#secondary>%s", prefix, DOT, replace(message, applyHighlightColor(LogicColor.getColor("primary"),
                LogicColor.getColor("highlight"), replacements)));
    }

    public static String error(String prefix, String message, Object... replacements) {
        return replace("<c#error_primary>&l%s &8%s <c#error_secondary>%s", prefix, DOT, replace(message, applyHighlightColor(LogicColor.getColor("error-secondary"), LogicColor.getColor("error-highlight"), replacements)));
    }

    public static String success(String prefix, String message, Object... replacements) {
        return replace("<c#success_primary>&l%s &8%s <c#success_secondary>%s", prefix, DOT, replace(message, applyHighlightColor(LogicColor.getColor("success-secondary"), LogicColor.getColor("success-highlight"), replacements)));
    }

    public static String listHeader(String prefix, String info, Object... replacements) {
        return replace("<c#primary>&l%s &8%s <c#secondary>%s", prefix, DOT, replace(info, applyHighlightColor(LogicColor.getColor("secondary"), LogicColor.getColor("highlight"), replacements)));
    }

    public static String listItem(String prefix, String info, Object... replacements) {
        return replace("&8| &8» <c#success_primary>%s &8%s <c#success_secondary>%s", prefix, DOT, replace(info, applyHighlightColor(LogicColor.getColor("success-primary"), LogicColor.getColor("success-secondary"), replacements)));
    }

    public static String dottedMessage(String prefix, String info, Object... replacements) {
        return replace("<c#success_primary>%s &8%s <c#success_secondary>%s", prefix, DOT, replace(info, applyHighlightColor(LogicColor.getColor("success-primary"), LogicColor.getColor("success-secondary"), replacements)));
    }

    public static void sendMessage(Player player, String prefix, String message, Placeholder... placeholders) {
        String formattedMessage = LogicColor.getColor("primary") + "&l" + prefix + " &8• " + LogicColor.getColor("secondary") + message;

        for (Placeholder placeholder : placeholders) {
            formattedMessage = placeholder.modify(formattedMessage);
        }

        player.sendMessage(format(formattedMessage));
    }

    public static void sendRedisMessage(UUID player, String message) {
        APIByLogic.getInstance().getRedisClient().sendListenerMessage(
                new ListenerComponent(null, "j-message")
                        .addData("uuid", player).addData("message", message));
    }

    public static void sendRawMessage(Player player, String message, Placeholder... placeholders) {
        String formattedMessage = message;

        for (Placeholder placeholder : placeholders) {
            formattedMessage = placeholder.modify(formattedMessage);
        }

        player.sendMessage(formattedMessage);
    }

    public static Object[] applyHighlightColor(String primaryColor, String highlightColor, Object[] objects) {
        List<String> formattedText = new ArrayList<>();

        for (Object o : objects) {
            formattedText.add(highlightColor + o.toString() + primaryColor);
        }

        return formattedText.toArray(new String[]{});
    }

    public static String[] getPagedList(String header, List<String> data, int page) {
        List<String> text = new ArrayList<>();

        int pages = data.size() / 10.0d % 1 == 0 ? data.size() / 10 : data.size() / 10 + 1;
        int lastPossibleItem = data.size();

        if (page == 0 || page > pages) {
            text.add(Formatter.error(header, "Invalid page!"));
            return text.toArray(new String[]{});
        }

        int startingItem = (page * 10) - 10;
        int lastItem = Math.min(startingItem + 10, lastPossibleItem);
        text.add(main(header, ""));

        for (int i = startingItem; i < lastItem; i++) {
            String item = data.get(i);
            text.add(format(LogicColor.getColor("separator") + "| » " + LogicColor.getColor("highlight") + item));
        }

        text.add(color(replace("⤷ &7(Page: %s/%s)", page, pages)));
        return text.toArray(new String[]{});
    }

}
