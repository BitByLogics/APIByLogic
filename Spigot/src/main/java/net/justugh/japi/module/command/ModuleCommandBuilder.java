package net.justugh.japi.module.command;

import lombok.Getter;
import org.bukkit.command.CommandSender;

@Getter
public class ModuleCommandBuilder {

    private String name;
    private String[] aliases = new String[] {};
    private String permission;
    private String prefix = "RealmManager";
    private boolean playerOnly;
    private ModuleCommandExecutor executor;

    public ModuleCommandBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ModuleCommandBuilder aliases(String[] aliases) {
        this.aliases = aliases;
        return this;
    }

    public ModuleCommandBuilder permission(String permission) {
        this.permission = permission;
        return this;
    }

    public ModuleCommandBuilder prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public ModuleCommandBuilder playerOnly(boolean playerOnly) {
        this.playerOnly = playerOnly;
        return this;
    }

    public ModuleCommandBuilder executor(ModuleCommandExecutor executor) {
        this.executor = executor;
        return this;
    }

    public ModuleCommand build() {
        ModuleCommand command = new ModuleCommand(name, aliases, permission) {
            @Override
            public void execute(CommandSender sender, String[] args) {
                executor.execute(sender, args);
            }
        };

        command.setPrefix(prefix);
        command.setPlayerOnly(playerOnly);

        return command;
    }

}
