package net.bitbylogic.apibylogic.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.bitbylogic.apibylogic.util.Placeholder;
import net.bitbylogic.apibylogic.util.StringUtil;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "APIByLogic";
    }

    @Override
    public @NotNull String getAuthor() {
        return "BitByLogic";
    }

    @Override
    public @NotNull String getVersion() {
        return "2024.10";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String placeholder) {
        if (placeholder.split("_")[0].equalsIgnoreCase("gradient")) {
            String[] placeholderData = placeholder.split("_");
            List<String> colors = new ArrayList<>(Arrays.asList(placeholderData[1].split(",")));

            return Formatter.color(Formatter.applyGradientToText(placeholderData[2], colors.toArray(new String[]{})));
        }

        if(placeholder.split("_")[0].equalsIgnoreCase("center")) {
            String[] placeholderData = placeholder.split("_");
            return Formatter.centerMessage(StringUtil.join(1, placeholderData, " "));
        }

        for (Placeholder modifier : Formatter.getGlobalModifiers().stream().filter(modifier -> modifier instanceof Placeholder)
                .map(modifier -> ((Placeholder) modifier)).toList()) {

            for (Map.Entry<String, String> entry : modifier.getPlaceholderMap().entrySet()) {
                if (!placeholder.equalsIgnoreCase(entry.getKey().replace("%", ""))) {
                    continue;
                }

                return entry.getValue();
            }

        }

        return Formatter.format(placeholder);
    }
}
