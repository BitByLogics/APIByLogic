package net.bitbylogic.apibylogic.module;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import net.bitbylogic.apibylogic.module.command.ModuleCommand;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Getter
@Setter
public abstract class Module implements ModuleInterface, Listener {

    private final JavaPlugin plugin;
    private final ModuleManager moduleManager;

    private boolean enabled;

    private File dataFolder;
    private File configFile;
    private YamlConfiguration config;
    private List<ModuleCommand> commands;
    private List<Integer> tasks = Lists.newArrayList();
    private List<Listener> listeners = Lists.newArrayList();

    public Module(JavaPlugin plugin, ModuleManager moduleManager) {
        this.plugin = plugin;
        this.moduleManager = moduleManager;
        this.commands = new ArrayList<>();

        loadConfiguration();
    }

    private void loadConfiguration() {
        ModuleData moduleData = getModuleData();
        String pathPrefix = moduleData.getId().toLowerCase().replace(" ", "_");

        dataFolder = new File(plugin.getDataFolder() + File.separator + pathPrefix);
        configFile = new File(getDataFolder() + File.separator + "config.yml");

        InputStream configStream = plugin.getResource(pathPrefix + "/config.yml");

        if (!configFile.exists() && configStream != null) {
            plugin.saveResource(pathPrefix + "/config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        if (configStream != null) {
            YamlConfiguration jarConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(configStream));

//            for (String key : jarConfig.getKeys(true)) {
//                if(config.get(key) == null) {
//                    config.set(key, jarConfig.get(key));
//                }
//            }

            saveConfig();
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        onReload();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void registerCommand(ModuleCommand... commands) {
        for (ModuleCommand command : commands) {
            if (!this.commands.contains(command) && this.commands.stream().noneMatch(lCommand -> lCommand.getName().equalsIgnoreCase(command.getName()))) {
                this.commands.add(command);
                moduleManager.getCommandManager().registerCommand(command);
            }
        }
    }

    protected void registerModuleListener(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    /**
     * Return a configuration file from the modules folder.
     *
     * @param name The files name (do not include .yml)
     * @return The newly created configuration file instance.
     */
    public YamlConfiguration getModuleConfig(String name) {
        File tempConfigFile = new File(getDataFolder() + File.separator + name + ".yml");

        if (!tempConfigFile.exists()) {
            try {
                tempConfigFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return YamlConfiguration.loadConfiguration(tempConfigFile);
    }

    public void saveModuleConfig(YamlConfiguration config, String fileName) {
        File tempConfigFile = new File(getDataFolder() + File.separator + fileName + ".yml");

        try {
            config.save(tempConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int runModuleTask(Runnable runnable) {
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTask(plugin).getTaskId();

        tasks.add(taskId);

        return taskId;
    }

    public int runTaskLater(Runnable runnable, long delay) {
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskLater(plugin, delay).getTaskId();

        tasks.add(taskId);

        return taskId;
    }

    public int runTaskTimer(Runnable runnable, long delay, long repeat) {
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskTimer(plugin, delay, repeat).getTaskId();

        tasks.add(taskId);

        return taskId;
    }

    public int runModuleTask(BukkitTask task) {
        tasks.add(task.getTaskId());
        return task.getTaskId();
    }

    public void log(Level level, String message) {
        plugin.getLogger().log(level, "(" + getModuleData().getName() + ") " + message);
    }
}
