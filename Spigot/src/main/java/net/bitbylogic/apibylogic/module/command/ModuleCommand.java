package net.bitbylogic.apibylogic.module.command;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class ModuleCommand implements CommandInterface, TabCompleter {

    private final String name;
    private final String[] aliases;
    private final String permission;

    private String prefix = "APIByLogic";
    private boolean enabled = false;
    private boolean playerOnly;

    public ModuleCommand(String name, String[] aliases, String permission) {
        this.name = name;
        this.aliases = aliases;
        this.permission = permission;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>();
    }

}
