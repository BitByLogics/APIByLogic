package net.bitbylogic.logicutils.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum LogicColors {

    PRIMARY(ChatColor.of("#F77E21")),
    SECONDARY(ChatColor.of("#D1D1D1")),
    HIGHLIGHT(ChatColor.of("#548CFF")),
    ERROR_PRIMARY(ChatColor.of("#9B0000")),
    ERROR_SECONDARY(ChatColor.of("#FF7272")),
    ERROR_HIGHLIGHT(ChatColor.of("#B22727")),
    SUCCESS_PRIMARY(ChatColor.of("#52cd00")),
    SUCCESS_SECONDARY(ChatColor.of("#B8F1B0")),
    SUCCESS_HIGHLIGHT(ChatColor.of("#4E944F"));

    private final ChatColor color;

    public static LogicColors match(String string) {
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(string))
                .findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return color.toString();
    }

}
