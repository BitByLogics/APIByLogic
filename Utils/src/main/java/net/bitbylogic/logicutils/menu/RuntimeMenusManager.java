package net.bitbylogic.logicutils.menu;

import lombok.Getter;
import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.MenuBuilder;
import net.bitbylogic.logicutils.LogicUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;

@Getter
public class RuntimeMenusManager {

    private final LogicUtils plugin;
    private final File menusFolder;

    private HashMap<String, Menu> loadedMenus;

    public RuntimeMenusManager(LogicUtils plugin) {
        this.plugin = plugin;
        menusFolder = new File(plugin.getDataFolder(), "menus");

        if (!menusFolder.exists()) {
            menusFolder.mkdir();
        }

        loadMenus();
    }

    public void loadMenus() {
        loadedMenus = new HashMap<>();

        File[] files = menusFolder.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory() || !file.getName().endsWith(".yml")) {
                continue;
            }

            YamlConfiguration loadedMenuFile = YamlConfiguration.loadConfiguration(file);

            if (loadedMenuFile.getKeys(false).isEmpty()) {
                plugin.getLogger().log(Level.WARNING, "Empty menu file, skipping. File: " + file.getName());
                continue;
            }

            loadedMenuFile.getKeys(false).forEach(key -> {
                if (!loadedMenuFile.isConfigurationSection(key)) {
                    return;
                }

                Menu menu = Menu.getFromConfig(loadedMenuFile.getConfigurationSection(key)).get();
                loadedMenus.put(key, menu);
            });
        }
    }

}
