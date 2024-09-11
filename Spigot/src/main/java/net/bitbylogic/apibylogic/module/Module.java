package net.bitbylogic.apibylogic.module;

import co.aikar.commands.BaseCommand;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.apibylogic.module.task.ModulePendingTask;
import net.bitbylogic.apibylogic.util.config.Configurable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

@Getter
@Setter
public abstract class Module extends Configurable implements ModuleInterface, Listener {

    private final JavaPlugin plugin;
    private final ModuleManager moduleManager;

    private final File dataFolder;
    private final File configFile;

    private final List<BaseCommand> commands = new ArrayList<>();
    private final List<Integer> taskIds = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();
    private final List<Configurable> configurables = new ArrayList<>();

    private boolean enabled = true;
    private boolean debug = false;

    private YamlConfiguration config;

    public Module(JavaPlugin plugin, ModuleManager moduleManager) {
        this.plugin = plugin;
        this.moduleManager = moduleManager;

        ModuleData moduleData = getModuleData();
        String moduleDir = moduleData.getId().toLowerCase().replace(" ", "_");

        this.dataFolder = new File(plugin.getDataFolder() + File.separator + moduleDir);
        this.configFile = new File(getDataFolder() + File.separator + "config.yml");

        loadConfiguration();

        setConfigFile(configFile);
        loadConfigPaths();
    }

    private void loadConfiguration() {
        ModuleData moduleData = getModuleData();
        String moduleDir = moduleData.getId().toLowerCase().replace(" ", "_");

        if (!configFile.exists()) {
            InputStream configStream = plugin.getResource(moduleDir + "/config.yml");

            if (configStream != null) {
                plugin.saveResource(moduleDir + "/config.yml", false);
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            log(Level.SEVERE, "Unable to save configuration file.");
            e.printStackTrace();
        }
    }

    public <T> T getConfigValueOrDefault(@NonNull String path, @NonNull T defaultValue) {
        return getConfigValueOrDefault(path, defaultValue, true);
    }

    public <T> T getConfigValueOrDefault(@NonNull String path, @NonNull T defaultValue, boolean save) {
        Object actualValue = getConfig().get(path);

        if (actualValue == null && save) {
            config.set(path, defaultValue);
            saveConfig();
        }

        try {
            return actualValue == null ? defaultValue : (T) actualValue;
        } catch (ClassCastException e) {
            log(Level.SEVERE, "Unable to cast config value");
            e.printStackTrace();
        }

        return defaultValue;
    }

    protected void registerCommand(BaseCommand... commands) {
        for (BaseCommand command : commands) {
            if (this.commands.contains(command)) {
                continue;
            }

            this.commands.add(command);
        }
    }

    protected void registerModuleListener(Listener listener) {
        if (listeners.contains(listener)) {
            return;
        }

        moduleManager.getDependencyManager().injectDependencies(listener, false);
        listeners.add(listener);
        Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    protected void registerConfigurable(Configurable configurable) {
        if (configurables.contains(configurable)) {
            return;
        }

        configurables.add(configurable);
    }

    /**
     * Return a configuration file from the module's folder.
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

    public int runTask(Runnable runnable) {
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTask(plugin).getTaskId();

        taskIds.add(taskId);
        return taskId;
    }

    public int addTask(BukkitTask task) {
        taskIds.add(task.getTaskId());
        return task.getTaskId();
    }

    public int runTaskAsync(Runnable runnable) {
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskAsynchronously(plugin).getTaskId();

        taskIds.add(taskId);
        return taskId;
    }

    public int runTaskLater(Runnable runnable, long delay) {
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskLater(plugin, delay).getTaskId();

        taskIds.add(taskId);

        return taskId;
    }

    public int runTaskTimer(Runnable runnable, long delay, long repeat) {
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskTimer(plugin, delay, repeat).getTaskId();

        taskIds.add(taskId);

        return taskId;
    }

    public int runTaskLaterAsync(Runnable runnable, long delay) {
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskLaterAsynchronously(plugin, delay).getTaskId();

        taskIds.add(taskId);

        return taskId;
    }

    public int runTaskTimerAsync(Runnable runnable, long delay, long repeat) {
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskTimerAsynchronously(plugin, delay, repeat).getTaskId();

        taskIds.add(taskId);

        return taskId;
    }

    public void log(Level level, String message) {
        plugin.getLogger().log(level, "(" + getModuleData().getName() + ") " + message);
    }

    public void debug(Level level, String message) {
        if(!debug) {
            return;
        }

        plugin.getLogger().log(level, "(" + getModuleData().getName() + ") [DEBUG] " + message);
    }

    public void debugBroadcast(String message) {
        if(!debug) {
            return;
        }

        Bukkit.broadcast("(" + getModuleData().getName() + ") [DEBUG]: " + message, "apibylogic.module.debuglog");
    }

    public <T extends Module> void addDependencyTask(Class<T> dependency, Consumer<T> consumer) {
        if (moduleManager.getDependencyManager().getDependencies().containsKey(dependency, dependency.getName())) {
            consumer.accept((T) moduleManager.getDependencyManager().getDependencies().get(dependency, dependency.getName()));
            return;
        }

        moduleManager.getPendingModuleTasks().add(new ModulePendingTask<>(dependency) {
            @Override
            public void accept(T module) {
                consumer.accept(module);
            }
        });
    }

    @Override
    public void loadConfigPaths() {
        super.loadConfigPaths();

        if(configurables == null) {
            return;
        }

        configurables.forEach(Configurable::loadConfigPaths);
    }
}
