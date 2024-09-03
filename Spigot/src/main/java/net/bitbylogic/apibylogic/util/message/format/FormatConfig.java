package net.bitbylogic.apibylogic.util.message.format;

import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.apibylogic.util.LivePlaceholder;
import net.bitbylogic.apibylogic.util.config.Configurable;
import net.bitbylogic.apibylogic.util.config.annotation.ConfigPath;

import java.io.File;
import java.util.regex.Pattern;

@Getter
public class FormatConfig extends Configurable {

    @ConfigPath(path = "Center-Pixels")
    private int centerPixels = 154;

    @ConfigPath(path = "Symbols.Right-Arrow")
    private String rightArrow = "»";

    @ConfigPath(path = "Symbols.Dot")
    private String dot = "•";

    @ConfigPath(path = "Patterns.Placeholder")
    private String patternPlaceholder = "%.+?%";

    @ConfigPath(path = "Patterns.Format")
    private String patternFormat = "<([a-zA-Z0-9 _]+)>(.*?)</\\1>|<([a-zA-Z0-9 _]+)#(.*?)>(.*?)</\\3>";

    @ConfigPath(path = "Patterns.Hex")
    private String patternHex = "#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})";

    @ConfigPath(path = "Command")
    private String command = " <c#primary>/%command%</c> &8─ <c#secondary>%description%</c>";

    @ConfigPath(path = "Rich-Command.Text")
    private String richCommandText = " <c#primary>/%command%</c>";

    @ConfigPath(path = "Rich-Command.Hover")
    private String richCommandHover = "<c#secondary>%description%</c>";

    @ConfigPath(path = "Main")
    private String mainFormat = "<c#primary>&l%prefix%</c> &8%dot% <c#secondary>%message%</c>";

    @ConfigPath(path = "Error")
    private String errorFormat = "<c#error_primary>&l%prefix%</c> &8%dot% <c#error_secondary>%message%</c>";

    @ConfigPath(path = "Success")
    private String successFormat = "<c#success_primary>&l%prefix%</c> &8%dot% <c#success_secondary>%message%</c>";

    @ConfigPath(path = "List.Header")
    private String listHeader = "<c#primary>&l%prefix%</c> &8%dot% <c#secondary>%info%</c>";

    @ConfigPath(path = "List.Item")
    private String listItem = "&8| &8» <c#success_primary>%prefix%</c> &8%dot% <c#success_secondary>%message%</c>";

    @ConfigPath(path = "Dotted-Message")
    private String dottedMessage = "<c#success_primary>%prefix%</c> &8%dot% <c#success_secondary>%message%</c>";

    @ConfigPath(path = "Paged.Invalid-Page")
    private String invalidPage = "Invalid page!";

    @ConfigPath(path = "Paged.Item")
    private String pagedItem = "<c#separator>| » </c><c#highlight>%text%</c>";

    @ConfigPath(path = "Paged.Footer")
    private String pageFooter = "⤷ &7(Page: %current-page%/%pages%)";

    public FormatConfig(@NonNull File configFile) {
        super(configFile, "Formatting.");

        Formatter.registerGlobalModifier(
                new LivePlaceholder("%right-arrow%", () -> rightArrow),
                new LivePlaceholder("%dot%", () -> dot)
        );
    }

    public Pattern getPlaceholderPattern() {
        return Pattern.compile(patternPlaceholder);
    }

    public Pattern getFormatPattern() {
        return Pattern.compile(patternFormat);
    }

    public Pattern getHexPattern() {
        return Pattern.compile(patternHex);
    }

}
