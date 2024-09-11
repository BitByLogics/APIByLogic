package net.bitbylogic.apibylogic.util.message.format;

import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.database.redis.listener.ListenerComponent;
import net.bitbylogic.apibylogic.util.Placeholder;
import net.bitbylogic.apibylogic.util.StringModifier;
import net.bitbylogic.apibylogic.util.TimeConverter;
import net.bitbylogic.apibylogic.util.message.DefaultFontInfo;
import net.bitbylogic.apibylogic.util.message.LogicColor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;

public class Formatter {

    @Getter
    private static final List<StringModifier> globalModifiers = new ArrayList<>();

    @Getter
    private static final HashMap<String, Long> messageCooldowns = new HashMap<>();

    @Getter
    private static FormatConfig config;

    public static void registerConfig(@NonNull File configFile) {
        config = new FormatConfig(configFile);
    }

    public static void registerGlobalModifier(StringModifier... modifiers) {
        globalModifiers.addAll(Arrays.asList(modifiers));
    }

    public static String color(String message) {
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

        Matcher matcher = config.getHexPattern().matcher(coloredMessage);

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

        List<FormatData> formatData = new ArrayList<>();
        List<FormatData> temporaryData = new ArrayList<>();

        temporaryData.add(new FormatData(null, null, null, formattedMessage));

        while (!temporaryData.isEmpty()) {
            FormatData data = temporaryData.removeFirst();
            String stringToAnalyze = data.getContents();

            List<FormatData> newData = findFormatMatches(stringToAnalyze);

            if (data.getEntireMatch() != null) {
                newData.forEach(nd -> nd.setParentData(data));
            }

            formatData.addAll(newData);
            temporaryData.addAll(newData);
        }

        formatData.sort((dataOne, dataTwo) -> {
            FormatCode codeOne = dataOne.getCode();
            FormatCode codeTwo = dataTwo.getCode();
            return Integer.compare(codeOne.getPriority(), codeTwo.getPriority());
        });

        for (FormatData data : formatData) {
            formattedMessage = data.format(formattedMessage);
        }

        return color(formattedMessage);
    }

    private static List<FormatData> findFormatMatches(@NonNull String string) {
        List<FormatData> formatData = new ArrayList<>();
        Matcher matcher = config.getFormatPattern().matcher(string);

        while (matcher.find()) {
            String codeIdentifier = matcher.group(1) == null ? matcher.group(3) : matcher.group(1);
            String codeData = matcher.group(4);
            String text = matcher.group(2) == null ? matcher.group(5) : matcher.group(2);

            FormatCode code = FormatCode.match(codeIdentifier);

            if (code == null) {
                continue;
            }

            formatData.add(new FormatData(matcher.group(), code, codeData, text));
        }

        return formatData;
    }

    public static String autoFormat(String message, Object... replacements) {
        Matcher placeholderMatcher = config.getPlaceholderPattern().matcher(message);

        int currentIndex = 0;
        while (placeholderMatcher.find()) {
            if (currentIndex > replacements.length) {
                break;
            }

            String placeholder = placeholderMatcher.group();
            message = message.replace(placeholder, replacements[currentIndex++].toString());
        }

        return format(message);
    }

    public static BaseComponent richFormat(String message, Object... replacements) {
        return TextComponent.fromLegacy(replace(message, replacements));
    }

    public static String replace(String message, Object... replacements) {
        String formattedMessage = message;

        for (Object replacement : replacements) {
            if (replacement instanceof StringModifier) {
                formattedMessage = ((StringModifier) replacement).modify(formattedMessage);
                continue;
            }

            if (!formattedMessage.contains("%s")) {
                break;
            }

            formattedMessage = formattedMessage.replaceFirst("%s", Matcher.quoteReplacement(replacement.toString()));
        }

        return format(formattedMessage);
    }

    public static String command(String command, String description) {
        return replace(config.getConfigValue("Command"),
                new Placeholder("%command%", command),
                new Placeholder("%description%", description)
        );
    }

    public static BaseComponent richCommand(String command, String description) {
        BaseComponent component = richFormat(config.getConfigValue("Rich-Format.Text"), new Placeholder("%command%", command));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(richFormat(config.getConfigValue("Rich-Format.Hover"), new Placeholder("%description%", description)))));

        return component;
    }

    public static String main(String prefix, String message, Object... replacements) {
        return replace(config.getConfigValue("Main"),
                new Placeholder("%prefix%", prefix),
                new Placeholder("%message%", replace(message, applyHighlightColor(LogicColor.getColor("primary"), LogicColor.getColor("highlight"), replacements))));
    }

    public static String error(String prefix, String message, Object... replacements) {
        return replace(config.getConfigValue("Error"),
                new Placeholder("%prefix%", prefix),
                new Placeholder("%message%", replace(message, applyHighlightColor(LogicColor.getColor("error-secondary"), LogicColor.getColor("error-highlight"), replacements))));
    }

    public static String success(String prefix, String message, Object... replacements) {
        return replace(config.getConfigValue("Success"),
                new Placeholder("%prefix%", prefix),
                new Placeholder("%message%", replace(message, applyHighlightColor(LogicColor.getColor("success-secondary"), LogicColor.getColor("success-highlight"), replacements))));
    }

    public static String listHeader(String prefix, String info, Object... replacements) {
        return replace(config.getConfigValue("List.Header"),
                new Placeholder("%prefix%", prefix),
                new Placeholder("%info%", replace(info, applyHighlightColor(LogicColor.getColor("secondary"), LogicColor.getColor("highlight"), replacements))));
    }

    public static String listItem(String prefix, String message, Object... replacements) {
        return replace(config.getConfigValue("List.Item"),
                new Placeholder("%prefix%", prefix),
                new Placeholder("%message%", replace(message, applyHighlightColor(LogicColor.getColor("success-primary"), LogicColor.getColor("success-secondary"), replacements))));
    }

    public static String dottedMessage(String prefix, String message, Object... replacements) {
        return replace(config.getConfigValue("Dotted-Message"),
                new Placeholder("%prefix%", prefix),
                new Placeholder("%message%", replace(message, applyHighlightColor(LogicColor.getColor("success-primary"), LogicColor.getColor("success-secondary"), replacements))));
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
                new ListenerComponent(null, "abl-message")
                        .addData("uuid", player).addData("message", message));
    }

    public static void sendRawMessage(Player player, String message, Placeholder... placeholders) {
        String formattedMessage = message;

        for (Placeholder placeholder : placeholders) {
            formattedMessage = placeholder.modify(formattedMessage);
        }

        player.sendMessage(formattedMessage);
    }

    public static void sendCooldownMessage(Player player, String message, String id, String cooldown) {
        if (player == null) {
            return;
        }

        if (messageCooldowns.containsKey(id) && messageCooldowns.get(id) - System.currentTimeMillis() > 0) {
            return;
        }

        long cooldownTime = TimeConverter.convert(cooldown);
        messageCooldowns.put(id, System.currentTimeMillis() + cooldownTime);

        player.sendMessage(format(message));
    }

    public static Object[] applyHighlightColor(String primaryColor, String highlightColor, Object[] objects) {
        List<Object> formattedReplacements = new ArrayList<>();

        for (Object o : objects) {
            if (o instanceof Placeholder placeholder) {
                for (String key : placeholder.getPlaceholderMap().keySet()) {
                    placeholder.getPlaceholderMap().compute(key,
                            (k, value) -> highlightColor + value + primaryColor);
                }

                formattedReplacements.add(o);
                continue;
            }

            formattedReplacements.add(highlightColor + o.toString() + primaryColor);
        }

        return formattedReplacements.toArray();
    }

    public static String[] getPagedList(String header, List<String> data, int page) {
        List<String> text = new ArrayList<>();

        int pages = data.size() / 10.0d % 1 == 0 ? data.size() / 10 : data.size() / 10 + 1;
        int lastPossibleItem = data.size();

        if (page == 0 || page > pages) {
            text.add(Formatter.error(header, config.getConfigValue("Paged.Invalid-Page")));
            return text.toArray(new String[]{});
        }

        int startingItem = (page * 10) - 10;
        int lastItem = Math.min(startingItem + 10, lastPossibleItem);
        text.add(main(header, ""));

        for (int i = startingItem; i < lastItem; i++) {
            String item = data.get(i);
            text.add(format(config.getConfigValue("Paged.Item"), new Placeholder("%text%", item)));
        }

        text.add(format(config.getConfigValue("Paged.Footer"),
                new Placeholder("%current-page%", page),
                new Placeholder("%pages%", pages)));
        return text.toArray(new String[]{});
    }

    public static String applyGradientToText(String text, String[] colors) {
        int steps = text.length();
        String[] gradientColors = generateGradientColors(colors, steps);
        StringBuilder gradientText = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            String color = gradientColors[i];
            gradientText.append(color).append(ch);
        }

        return gradientText.toString();
    }

    private static String[] generateGradientColors(String[] colors, int steps) {
        List<String> stylePrefixes = new ArrayList<>();
        List<Color> colorList = new ArrayList<>();

        for (String color : colors) {
            if (!color.startsWith("&")) {
                colorList.add(Color.decode(color));
                continue;
            }

            stylePrefixes.add(color);
        }

        int numColors = colorList.size();
        String[] gradientColors = new String[steps];

        for (int i = 0; i < steps; i++) {
            float ratio = (float) i / (steps - 1);
            int segment = Math.min(numColors - 2, (int) (ratio * (numColors - 1)));
            float segmentRatio = (ratio * (numColors - 1)) - segment;

            Color startColor = colorList.get(segment);
            Color endColor = colorList.get(segment + 1);

            int r = (int) (startColor.getRed() + segmentRatio * (endColor.getRed() - startColor.getRed()));
            int g = (int) (startColor.getGreen() + segmentRatio * (endColor.getGreen() - startColor.getGreen()));
            int b = (int) (startColor.getBlue() + segmentRatio * (endColor.getBlue() - startColor.getBlue()));

            StringBuilder formattedColor = new StringBuilder(String.format("#%02X%02X%02X", r, g, b));

            // Append all style prefixes to the color
            for (String stylePrefix : stylePrefixes) {
                formattedColor.append(stylePrefix);
            }

            gradientColors[i] = formattedColor.toString();
        }

        return gradientColors;
    }

    public static String centerMessage(String message) {
        String sanitizedString = ChatColor.translateAlternateColorCodes('&', message);

        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (char c : sanitizedString.toCharArray()) {
            if (c == '§') {
                previousCode = true;
            } else if (previousCode) {
                previousCode = false;
                isBold = c == 'l';
            } else {
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize = isBold ? messagePxSize + dFI.getBoldLength() : messagePxSize + dFI.getLength();
                messagePxSize++;
            }
        }

        int centerPixels = config.getConfigValue("Center-Pixels");
        int toCompensate = centerPixels - messagePxSize / 2;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;

        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }

        return sb + message;
    }

    public static void sendCenteredMessages(Player player, String[] lines) {
        for (String line : lines) {
            sendCenteredMessage(player, line);
        }
    }

    public static void sendCenteredMessage(Player player, String message) {
        player.sendMessage(centerMessage(message));
    }

}
