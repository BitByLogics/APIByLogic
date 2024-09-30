package net.bitbylogic.apibylogic.module;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import net.bitbylogic.apibylogic.dependency.DependencyManager;
import net.bitbylogic.apibylogic.module.command.ModulesCommand;
import net.bitbylogic.apibylogic.module.task.ModulePendingTask;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
public class ModuleManager {

    private final JavaPlugin plugin;
    private final DependencyManager dependencyManager;
    private final PaperCommandManager commandManager;

    private final HashMap<String, Module> modules;
    private final List<ModulePendingTask<? extends Module>> pendingModuleTasks;

    public ModuleManager(JavaPlugin plugin, PaperCommandManager commandManager, DependencyManager dependencyManager) {
        this.plugin = plugin;
        this.dependencyManager = dependencyManager;
        this.commandManager = commandManager;
        this.modules = new HashMap<>();
        this.pendingModuleTasks = new ArrayList<>();

        dependencyManager.registerDependency(getClass(), this);
        dependencyManager.setCommandManager(commandManager);

        commandManager.getCommandCompletions().registerCompletion("moduleIds", context -> modules.values().stream()
                .map(ModuleInterface::getModuleData).map(ModuleData::getId).collect(Collectors.toSet()));

        ModulesCommand modulesCommand = new ModulesCommand();
        dependencyManager.injectDependencies(modulesCommand, false);
        commandManager.registerCommand(modulesCommand);

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Module module : modules.values()) {
                if(module.getTasks().isEmpty()) {
                    continue;
                }

                Iterator<ModuleTask> moduleTaskIterator = module.getTasks().iterator();

                while(moduleTaskIterator.hasNext()) {
                    ModuleTask task = moduleTaskIterator.next();

                    if(task.getTaskId() == -1
                            || (Bukkit.getScheduler().isCurrentlyRunning(task.getTaskId())) || Bukkit.getScheduler().isQueued(task.getTaskId())) {
                        continue;
                    }

                    moduleTaskIterator.remove();
                }
            }
        }, 0, 1);
    }

    /**
     * Register a Module.
     *
     * @param classes The classes to register.
     */
    @SafeVarargs
    public final void registerModule(Class<? extends Module>... classes) {
        for (Class<? extends Module> moduleClass : classes) {
            if (modules.get(moduleClass.getSimpleName()) != null) {
                plugin.getLogger().log(Level.WARNING, "[Module Manager]: Couldn't register module '" + moduleClass.getSimpleName() + "', this module is already registered.");
                continue;
            }

            long startTime = System.nanoTime();

            Module module;

            try {
                module = moduleClass.getDeclaredConstructor(JavaPlugin.class, ModuleManager.class).newInstance(plugin, this);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                plugin.getLogger().log(Level.SEVERE, "[Module Manager]: Couldn't create new instance of module class '" + moduleClass.getName() + "'");
                e.printStackTrace();
                continue;
            }

            dependencyManager.registerDependency(moduleClass, module);
            dependencyManager.injectDependencies(module, false);

            module.onRegister();
            module.getCommands().forEach(command -> dependencyManager.injectDependencies(command, false));
            modules.put(moduleClass.getSimpleName(), module);

            if (!plugin.getConfig().getStringList("disabled-modules").contains(module.getModuleData().getId())) {
                module.setEnabled(true);
                module.onEnable();
                module.getCommands().forEach(commandManager::registerCommand);
                Bukkit.getPluginManager().registerEvents(module, plugin);
            }

            getPendingTasks(moduleClass).forEach(task -> task.accept(module));
            pendingModuleTasks.removeIf(task -> task.getClazz().equals(moduleClass));

            long endTime = System.nanoTime();
            plugin.getLogger().log(Level.INFO, "[Module Manager]: Registration time for module (" + module.getModuleData().getName() + "): " + (endTime - startTime) / 1000000d + "ms");
        }
    }

    /**
     * Get a Module instance by it class.
     *
     * @param clazz The Module's class.
     * @return An instance of the Module.
     */
    public <T extends Module> T getModuleInstance(Class<T> clazz) {
        for (Module module : modules.values()) {
            if (module.getClass() == clazz) {
                return (T) module;
            }
        }

        return null;
    }

    /**
     * Enable a Module.
     *
     * @param moduleID The Module's ID.
     */
    public void enableModule(String moduleID) {
        Module module = getModuleByID(moduleID);

        if (module == null) {
            plugin.getLogger().log(Level.WARNING, "[Module Manager]: Invalid Module ID '" + moduleID + "'.");
            return;
        }

        if (!module.isEnabled()) {
            List<String> disabledModules = plugin.getConfig().getStringList("disabled-modules");
            disabledModules.remove(module.getModuleData().getId());
            plugin.getConfig().set("disabled-modules", disabledModules);
            plugin.saveConfig();
            module.setEnabled(true);
            module.reloadConfig();
            module.loadConfigPaths();
            module.onEnable();
            module.getCommands().forEach(commandManager::registerCommand);
            Bukkit.getPluginManager().registerEvents(module, plugin);
        }
    }

    /**
     * Disable a Module.
     *
     * @param moduleID The Module's ID.
     */
    public void disableModule(String moduleID) {
        Module module = getModuleByID(moduleID);

        if (module == null) {
            plugin.getLogger().log(Level.WARNING, "[Module Manager]: Invalid Module ID '" + moduleID + "'.");
            return;
        }

        if (module.isEnabled()) {
            List<String> disabledModules = plugin.getConfig().getStringList("disabled-modules");
            disabledModules.add(module.getModuleData().getId());
            plugin.getConfig().set("disabled-modules", disabledModules);
            plugin.saveConfig();
            module.setEnabled(false);
            module.onDisable();
            module.getTasks().forEach(ModuleTask::cancel);
            module.getListeners().forEach(HandlerList::unregisterAll);
            module.getCommands().forEach(commandManager::unregisterCommand);
            HandlerList.unregisterAll(module);
        }
    }

    /**
     * Get a Module instance by its ID.
     *
     * @param id The Module's ID.
     * @return The Module instance.
     */
    public Module getModuleByID(String id) {
        return modules.values().stream().filter(module -> module.getModuleData().getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    private <T extends Module> List<ModulePendingTask<Module>> getPendingTasks(Class<T> moduleClass) {
        List<ModulePendingTask<Module>> tasks = new ArrayList<>();
        for (ModulePendingTask<? extends Module> task : pendingModuleTasks) {
            if (moduleClass.isAssignableFrom(task.getClazz())) {
                @SuppressWarnings("unchecked")
                ModulePendingTask<Module> castedTask = (ModulePendingTask<Module>) task;
                tasks.add(castedTask);
            }
        }
        return tasks;
    }

}
