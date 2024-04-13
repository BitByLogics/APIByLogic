package net.justugh.japi.module.command;

import lombok.Getter;
import net.justugh.japi.util.message.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

@Getter
public class CommandManager implements CommandExecutor, TabCompleter {

    public CommandMap commandMap;
    public final HashSet<ModuleCommand> commands = new HashSet<>();

    public CommandManager() {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf('.') + 1);
            Field commandMapField = Class.forName("org.bukkit.craftbukkit." + version + ".CraftServer").getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception exception) {
            Bukkit.getLogger().log(Level.SEVERE, "(Command Manager): Unable to load CommandMap class, commands are unavailable.");
            exception.printStackTrace();
        }
    }

    /**
     * Register a command
     *
     * @param moduleCommand - Command being registered
     */
    public void registerCommand(ModuleCommand moduleCommand) {
        long startTime = System.nanoTime();
        commands.add(moduleCommand);
        commandMap.register(moduleCommand.getPrefix(),
                new Command(moduleCommand.getName(), "", "", Arrays.asList(moduleCommand.getAliases())) {

                    @Override
                    public boolean execute(CommandSender caller, String label, String[] args) {
                        onCommand(caller, this, label, args);
                        return true;
                    }

                    @Override
                    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
                        return super.tabComplete(sender, alias, args);
                    }
                });
        long endTime = System.nanoTime();
        Bukkit.getLogger().log(Level.INFO, "(Command Manager): Registration time for command (" + moduleCommand.getName() + "): " + (endTime - startTime) / 1000000d + "ms");
    }

    public void unregisterCommand(ModuleCommand moduleCommand) {
        if (commands.contains(moduleCommand)) {
            try {
                commandMap.getCommand(moduleCommand.getName()).unregister(commandMap);
            } catch (NullPointerException exception) {
                Bukkit.getLogger().log(Level.SEVERE, "[Command Manager] Error: Cannot unregister a command that is null.");
            }
        } else {
            Bukkit.getLogger().log(Level.WARNING, "[Command Manager] Error: Cannot unregister a command that isn't registered.");
        }
    }

    @Override
    public boolean onCommand(CommandSender caller, Command cmd, String label, String[] args) {
        for (ModuleCommand moduleCommand : commands) {
            boolean validCommand = false;

            for (String s : moduleCommand.getAliases()) {
                if (s.equalsIgnoreCase(label)) {
                    validCommand = true;
                }
            }

            if (moduleCommand.getName().equalsIgnoreCase(label) || validCommand) {
                if (!moduleCommand.isEnabled()) {
                    caller.sendMessage("Unknown command. Type \"/help\" for help.");
                    return true;
                }

                if (!(caller instanceof Player) && moduleCommand.isPlayerOnly()) {
                    caller.sendMessage("§cSorry, turns out this command is for players only.");
                    return true;
                }

                if (moduleCommand.getPermission() == null) {
                    moduleCommand.execute(caller, args);
                    return true;
                }

                if (!caller.hasPermission(moduleCommand.getPermission())) {
                    caller.sendMessage(Messages.main("Permissions", "&cYou cannot run this command!"));
                    return true;
                }

                moduleCommand.execute(caller, args);
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        for (ModuleCommand moduleCommand : commands) {
            boolean validCommand = false;

            for (String s : moduleCommand.getAliases()) {
                if (s.equalsIgnoreCase(label)) {
                    validCommand = true;
                    break;
                }
            }

            if (moduleCommand.getName().equalsIgnoreCase(label) || validCommand) {
                if (!moduleCommand.isEnabled()) {
                    return null;
                }

                if (!(sender instanceof Player) && moduleCommand.isPlayerOnly()) {
                    return null;
                }

                if (moduleCommand.getPermission() == null) {
                    return moduleCommand.onTabComplete(sender, command, label, args);
                }

                if (!sender.hasPermission(moduleCommand.getPermission())) {
                    return null;
                }

                return moduleCommand.onTabComplete(sender, command, label, args);
            }
        }

        return null;
    }

}