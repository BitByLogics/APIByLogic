package net.bitbylogic.apibylogic.module;

import co.aikar.commands.BaseCommand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.apibylogic.module.task.ModulePendingTask;
import net.bitbylogic.apibylogic.util.config.configurable.Configurable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
@Setter
public abstract class Module extends Configurable implements ModuleInterface, Listener {

    private final JavaPlugin plugin;
    private final ModuleManager moduleManager;

    private final File dataFolder;
    private final File configFile;

    private final List<BaseCommand> commands = new ArrayList<>();
    private final List<ModuleTask> tasks = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();
    private final List<Configurable> configurables = new ArrayList<>();

    private boolean enabled = true;

    @Setter(AccessLevel.NONE)
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

    protected void registerModuleListener(Listener... listeners) {
        for (Listener listener : listeners) {
            if (this.listeners.contains(listener)) {
                return;
            }

            moduleManager.getDependencyManager().injectDependencies(listener, true);
            this.listeners.add(listener);

            Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
            debug(Level.INFO, String.format("Successfully registered listener: %s", listener.getClass().getSimpleName()));
        }
    }

    protected void registerConfigurable(Configurable configurable) {
        if (configurables.contains(configurable)) {
            return;
        }

        configurables.add(configurable);
    }

    public File getModuleFile(@NonNull String name) {
        if(!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File file = new File(getDataFolder() + File.separator + name);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log(Level.WARNING, String.format("Unable to create module file '%s' for module '%s'!", name, getModuleData().getId()));
            }
        }

        return file;
    }

    /**
     * Return a configuration file from the module's folder.
     *
     * @param name The files name (do not include .yml)
     * @return The newly created configuration file instance.
     */
    public YamlConfiguration getModuleConfig(String name) {
        if(!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

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

    public int runTask(@NonNull String id, @NonNull ModuleRunnable runnable) {
        ModuleTask moduleTask = new ModuleTask(id, ModuleTask.ModuleTaskType.SINGLE, runnable) {
            @Override
            public void run() {
                runnable.run();
            }
        };

        moduleTask.setModuleInstance(this);
        synchronized (tasks) {
            tasks.add(moduleTask);
        }

        moduleTask.getBukkitRunnable().runTask(plugin);
        return moduleTask.getTaskId();
    }

    public int runTask(@NonNull String id, @NonNull Runnable runnable) {
        ModuleTask moduleTask = new ModuleTask(id, ModuleTask.ModuleTaskType.SINGLE) {
            @Override
            public void run() {
                runnable.run();
            }
        };

        moduleTask.setModuleInstance(this);
        synchronized (tasks) {
            tasks.add(moduleTask);
        }

        moduleTask.getBukkitRunnable().runTask(plugin);
        return moduleTask.getTaskId();
    }

    public int runTaskAsync(@NonNull String id, @NonNull ModuleRunnable runnable) {
        ModuleTask moduleTask = new ModuleTask(id, ModuleTask.ModuleTaskType.SINGLE_ASYNC, runnable) {
            @Override
            public void run() {
                runnable.run();
            }
        };

        moduleTask.setModuleInstance(this);
        synchronized (tasks) {
            tasks.add(moduleTask);
        }

        moduleTask.getBukkitRunnable().runTaskAsynchronously(plugin);
        return moduleTask.getTaskId();
    }

    public int runTaskAsync(@NonNull String id, @NonNull Runnable runnable) {
        ModuleTask moduleTask = new ModuleTask(id, ModuleTask.ModuleTaskType.SINGLE_ASYNC) {
            @Override
            public void run() {
                runnable.run();
            }
        };

        moduleTask.setModuleInstance(this);
        synchronized (tasks) {
            tasks.add(moduleTask);
        }

        moduleTask.getBukkitRunnable().runTaskAsynchronously(plugin);
        return moduleTask.getTaskId();
    }

    public int runTaskLater(@NonNull String id, long delay, @NonNull ModuleRunnable runnable) {
        ModuleTask moduleTask = new ModuleTask(id, ModuleTask.ModuleTaskType.DELAYED, runnable) {
            @Override
            public void run() {
                runnable.run();
            }
        };

        moduleTask.setModuleInstance(this);
        synchronized (tasks) {
            tasks.add(moduleTask);
        }

        moduleTask.getBukkitRunnable().runTaskLater(plugin, delay);
        return moduleTask.getTaskId();
    }

    public int runTaskLater(@NonNull String id, long delay, @NonNull Runnable runnable) {
        ModuleTask moduleTask = new ModuleTask(id, ModuleTask.ModuleTaskType.DELAYED) {
            @Override
            public void run() {
                runnable.run();
            }
        };

        moduleTask.setModuleInstance(this);
        synchronized (tasks) {
            tasks.add(moduleTask);
        }

        moduleTask.getBukkitRunnable().runTaskLater(plugin, delay);
        return moduleTask.getTaskId();
    }

    public int runTaskTimer(@NonNull String id, long delay, long repeat, @NonNull ModuleRunnable runnable) {
        ModuleTask moduleTask = new ModuleTask(id, ModuleTask.ModuleTaskType.TIMER, runnable) {
            @Override
            public void run() {
                runnable.run();
            }
        };

        moduleTask.setModuleInstance(this);
        synchronized (tasks) {
            tasks.add(moduleTask);
        }

        moduleTask.getBukkitRunnable().runTaskTimer(plugin, delay, repeat);
        return moduleTask.getTaskId();
    }

    public int runTaskTimer(@NonNull String id, long delay, long repeat, @NonNull Runnable runnable) {
        ModuleTask moduleTask = new ModuleTask(id, ModuleTask.ModuleTaskType.TIMER) {
            @Override
            public void run() {
                runnable.run();
            }
        };

        moduleTask.setModuleInstance(this);
        synchronized (tasks) {
            tasks.add(moduleTask);
        }

        moduleTask.getBukkitRunnable().runTaskTimer(plugin, delay, repeat);
        return moduleTask.getTaskId();
    }

    public int runTaskLaterAsync(@NonNull String id, long delay, @NonNull ModuleRunnable runnable) {
        ModuleTask moduleTask = new ModuleTask(id, ModuleTask.ModuleTaskType.DELAYED_ASYNC, runnable) {
            @Override
            public void run() {
                runnable.run();
            }
        };

        moduleTask.setModuleInstance(this);
        synchronized (tasks) {
            tasks.add(moduleTask);
        }

        moduleTask.getBukkitRunnable().runTaskLaterAsynchronously(plugin, delay);
        return moduleTask.getTaskId();
    }

    public int runTaskLaterAsync(@NonNull String id, long delay, @NonNull Runnable runnable) {
        ModuleTask moduleTask = new ModuleTask(id, ModuleTask.ModuleTaskType.DELAYED_ASYNC) {
            @Override
            public void run() {
                runnable.run();
            }
        };

        moduleTask.setModuleInstance(this);
        synchronized (tasks) {
            tasks.add(moduleTask);
        }

        moduleTask.getBukkitRunnable().runTaskLaterAsynchronously(plugin, delay);
        return moduleTask.getTaskId();
    }

    public int runTaskTimerAsync(@NonNull String id, long delay, long repeat, @NonNull ModuleRunnable runnable) {
        ModuleTask moduleTask = new ModuleTask(id, ModuleTask.ModuleTaskType.TIMER_ASYNC, runnable) {
            @Override
            public void run() {
                runnable.run();
            }

            @Override
            public void cancel() {
                super.cancel();
            }
        };

        moduleTask.setModuleInstance(this);
        synchronized (tasks) {
            tasks.add(moduleTask);
        }

        moduleTask.getBukkitRunnable().runTaskTimerAsynchronously(plugin, delay, repeat);
        return moduleTask.getTaskId();
    }

    public int runTaskTimerAsync(@NonNull String id, long delay, long repeat, @NonNull Runnable runnable) {
        ModuleTask moduleTask = new ModuleTask(id, ModuleTask.ModuleTaskType.TIMER_ASYNC) {
            @Override
            public void run() {
                runnable.run();
            }
        };

        moduleTask.setModuleInstance(this);
        synchronized (tasks) {
            tasks.add(moduleTask);
        }

        moduleTask.getBukkitRunnable().runTaskTimerAsynchronously(plugin, delay, repeat);
        return moduleTask.getTaskId();
    }

    public Set<ModuleTask> getTasksById(@NonNull String id) {
        synchronized (tasks) {
            return tasks.stream().filter(moduleTask -> moduleTask.getId().equalsIgnoreCase(id)).collect(Collectors.toUnmodifiableSet());
        }
    }

    public void cancelTask(@NonNull String id) {
        synchronized (tasks) {
            tasks.stream().filter(moduleTask -> moduleTask.getId().equalsIgnoreCase(id)).findFirst().ifPresent(task -> {
                if(task.getRunnable() != null) {
                    task.getRunnable().cancel();
                    return;
                }

                task.cancel();
            });
        }
    }

    public void log(Level level, String message) {
        plugin.getLogger().log(level, "(" + getModuleData().getName() + ") " + message);
    }

    public void debug(Level level, String message) {
        if (!debug) {
            return;
        }

        plugin.getLogger().log(level, "(" + getModuleData().getName() + ") [DEBUG] " + message);
    }

    public void debugBroadcast(String message) {
        if (!debug) {
            return;
        }

        Bukkit.broadcast("(" + getModuleData().getName() + ") [DEBUG]: " + message, "apibylogic.module.debuglog");
    }

    public <T extends Module> void addDependencyTask(Class<T> dependency, Consumer<T> consumer) {
        if (moduleManager.getDependencyManager().getDependencies().containsKey(dependency)) {
            consumer.accept((T) moduleManager.getDependencyManager().getDependencies().get(dependency));
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

        if (configurables == null) {
            return;
        }

        configurables.forEach(Configurable::loadConfigPaths);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;

        List<String> debugModules = plugin.getConfig().getStringList("Debug-Modules");

        if(!debug) {
            debugModules.remove(getModuleData().getId());
        } else {
            debugModules.add(getModuleData().getId());
        }

        plugin.getConfig().set("Debug-Modules", debugModules);
        plugin.saveConfig();
    }
}
