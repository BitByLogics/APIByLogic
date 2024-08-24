package net.bitbylogic.apibylogic.util.message;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class FormatData {

    private final FormatCode code;
    private final String codeData;

    private String entireMatch;
    private String contents;
    private FormatData parentData;

    public FormatData(String entireMatch, FormatCode code, String codeData, String contents) {
        this.entireMatch = entireMatch;
        this.code = code;
        this.codeData = codeData;
        this.contents = contents;
    }

    public String format(@NonNull String string) {
        switch (code) {
            case COLOR:
                String color = LogicColor.getColor(codeData);

                if (color == null) {
                    break;
                }

                contents = entireMatch.replace(entireMatch, color + contents);
                break;
            case HEX:
                if (codeData == null) {
                    break;
                }

                contents = entireMatch.replace(entireMatch, ChatColor.of(codeData).toString() + contents);
                break;
            case GRADIENT:
                if (codeData == null) {
                    break;
                }

                List<String> colors = new ArrayList<>(Arrays.asList(codeData.split(",")));
                contents = entireMatch.replace(entireMatch, Formatter.color(Formatter.applyGradientToText(contents, colors.toArray(new String[]{}))));
                break;
            case CENTER:
                contents = entireMatch.replace(entireMatch, Formatter.centerMessage(contents));
            default:
            case CUSTOM_DATA:
        }

        if (parentData != null) {
            parentData.setEntireMatch(parentData.getEntireMatch().replace(entireMatch, contents));
            parentData.setContents(parentData.getContents().replace(entireMatch, contents));
        }

        return string.replace(entireMatch, contents);
    }

}
