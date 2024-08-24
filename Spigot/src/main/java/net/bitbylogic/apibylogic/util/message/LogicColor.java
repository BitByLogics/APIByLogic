package net.bitbylogic.apibylogic.util.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;

@Getter
@AllArgsConstructor
public class LogicColor {

    private final static HashMap<String, String> colors = new HashMap<>();

    public static void loadColors(FileConfiguration config) {
        colors.clear();

        for (String key : config.getConfigurationSection("Colors").getKeys(false)) {
            colors.put(key, ChatColor.of(config.getString("Colors." + key)).toString());
        }
    }

    public static String getColor(String name) {
        if(name == null || name.isEmpty()) {
            return null;
        }

        String colorId = colors.keySet().stream().filter(color -> color.equalsIgnoreCase(name) ||
                color.replace("-", "_").equalsIgnoreCase(name)).findFirst().orElse(null);
        return colorId == null ? null : colors.get(colorId);
    }

}
