package net.bitbylogic.apibylogic.module;

import lombok.Getter;
import net.bitbylogic.apibylogic.dependency.DependencyManager;
import net.bitbylogic.apibylogic.module.command.ModulesCommand;
import net.bitbylogic.apibylogic.module.command.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

@Getter
public class ModuleManager {

    private final JavaPlugin plugin;
    private final DependencyManager dependencyManager;
    private final CommandManager commandManager;

    private final HashMap<String, Module> modules;

    public ModuleManager(JavaPlugin plugin, DependencyManager dependencyManager) {
        this.plugin = plugin;
        this.dependencyManager = dependencyManager;
        commandManager = new CommandManager();
        modules = new HashMap<>();

        commandManager.registerCommand(new ModulesCommand(this));
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
                Bukkit.getLogger().log(Level.WARNING, "[Module Manager]: Couldn't register module '" + moduleClass.getSimpleName() + "', this module is already registered.");
                continue;
            }

            long startTime = System.nanoTime();

            Module module;

            try {
                module = moduleClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                Bukkit.getLogger().log(Level.SEVERE, "[Module Manager]: Couldn't create new instance of module class '" + moduleClass.getName() + "'");
                e.printStackTrace();
                continue;
            }

            dependencyManager.registerDependency(module.getClass(), module);
            dependencyManager.injectDependencies(module);

            module.onRegister();
            module.getCommands().forEach(dependencyManager::injectDependencies);
            modules.put(moduleClass.getSimpleName(), module);

            if (!plugin.getConfig().getStringList("disabled-modules").contains(module.getModuleData().getId() + "")) {
                module.setEnabled(true);
                module.onEnable();
                module.getCommands().forEach(command -> command.setEnabled(true));
                Bukkit.getPluginManager().registerEvents(module, plugin);
            }

            long endTime = System.nanoTime();
            Bukkit.getLogger().log(Level.INFO, "[Module Manager]: Registration time for module (" + module.getModuleData().getName() + "): " + (endTime - startTime) / 1000000d + "ms");
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
            Bukkit.getLogger().log(Level.WARNING, "[Module Manager]: Invalid Module ID '" + moduleID + "'.");
            return;
        }

        if (!module.isEnabled()) {
            List<String> disabledModules = plugin.getConfig().getStringList("disabled-modules");
            disabledModules.remove(module.getModuleData().getId() + "");
            plugin.getConfig().set("disabled-modules", disabledModules);
            plugin.saveConfig();
            module.setEnabled(true);
            module.onEnable();
            module.reloadConfig();
            module.getCommands().forEach(command -> command.setEnabled(true));
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
            Bukkit.getLogger().log(Level.WARNING, "[Module Manager]: Invalid Module ID '" + moduleID + "'.");
            return;
        }

        if (module.isEnabled()) {
            List<String> disabledModules = plugin.getConfig().getStringList("disabled-modules");
            disabledModules.add(module.getModuleData().getId() + "");
            plugin.getConfig().set("disabled-modules", disabledModules);
            plugin.saveConfig();
            module.setEnabled(false);
            module.onDisable();
            module.getTasks().forEach(taskID -> Bukkit.getScheduler().cancelTask(taskID));
            module.getListeners().forEach(HandlerList::unregisterAll);
            module.getCommands().forEach(command -> command.setEnabled(false));
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

}
