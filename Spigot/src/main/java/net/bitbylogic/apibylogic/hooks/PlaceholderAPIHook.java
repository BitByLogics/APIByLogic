package net.bitbylogic.apibylogic.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.bitbylogic.apibylogic.util.Placeholder;
import net.bitbylogic.apibylogic.util.message.Formatter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Collectors;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "abl";
    }

    @Override
    public @NotNull String getAuthor() {
        return "BitByLogic";
    }

    @Override
    public @NotNull String getVersion() {
        return "2024.4";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String placeholder) {
        for (Placeholder modifier : Formatter.getGlobalModifiers().stream().filter(modifier -> modifier instanceof Placeholder)
                .map(modifier -> ((Placeholder) modifier)).collect(Collectors.toList())) {
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
