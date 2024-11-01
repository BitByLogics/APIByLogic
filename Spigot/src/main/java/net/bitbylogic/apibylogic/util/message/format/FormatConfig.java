package net.bitbylogic.apibylogic.util.message.format;

import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.apibylogic.util.LivePlaceholder;
import net.bitbylogic.apibylogic.util.config.configurable.Configurable;

import java.io.File;
import java.util.regex.Pattern;

@Getter
public class FormatConfig extends Configurable {

    public FormatConfig(@NonNull File configFile) {
        super(configFile, "Formatting.",
                pair("Center-Pixels", 154),
                pair("Symbols.Right-Arrow", "»"),
                pair("Symbols.Dot", "•"),
                pair("Patterns.Placeholder", "%.+?%"),
                pair("Patterns.Format", "<([a-zA-Z0-9 _]+)>(.*?)</\\1>|<([a-zA-Z0-9 _]+)#(.*?)>(.*?)</\\3>"),
                pair("Patterns.Hex", "#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})"),
                pair("Patterns.Spigot-Hex", "§x(§[a-fA-F0-9]){6}§r"),
                pair("Command", " <c#primary>/%command%</c> &8─ <c#secondary>%description%</c>"),
                pair("Rich-Command.Text", " <c#primary>/%command%</c>"),
                pair("Rich-Command.Hover", "<c#secondary>%description%</c>"),
                pair("Main", "<c#primary>&l%prefix%</c> &8%dot% <c#secondary>%message%</c>"),
                pair("Error", "<c#error_primary>&l%prefix%</c> &8%dot% <c#error_secondary>%message%</c>"),
                pair("Success", "<c#success_primary>&l%prefix%</c> &8%dot% <c#success_secondary>%message%</c>"),
                pair("List.Header", "<c#primary>&l%prefix%</c> &8%dot% <c#secondary>%info%</c>"),
                pair("List.Item", "&8| &8» <c#success_primary>%prefix%</c> &8%dot% <c#success_secondary>%message%</c>"),
                pair("Dotted-Message", "<c#success_primary>%prefix%</c> &8%dot% <c#success_secondary>%message%</c>"),
                pair("Paged.Invalid-Page", "Invalid page!"),
                pair("Paged.Item", "<c#separator>| » </c><c#highlight>%text%</c>"),
                pair("Paged.Footer", "⤷ &7(Page: %current-page%/%pages%)")
        );

        Formatter.registerGlobalModifier(
                new LivePlaceholder("%right-arrow%", () -> getConfigValue("Symbols.Right-Arrow")),
                new LivePlaceholder("%dot%", () -> getConfigValue("Symbols.Dot"))
        );
    }

    public Pattern getPlaceholderPattern() {
        return Pattern.compile(getConfigValue("Patterns.Placeholder"));
    }

    public Pattern getFormatPattern() {
        return Pattern.compile(getConfigValue("Patterns.Format"));
    }

    public Pattern getHexPattern() {
        return Pattern.compile(getConfigValue("Patterns.Hex"));
    }

    public Pattern getSpigotHexPattern() {
        return Pattern.compile(getConfigValue("Patterns.Spigot-Hex"));
    }

}
