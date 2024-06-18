package net.bitbylogic.logicutils;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import net.bitbylogic.logicutils.commands.*;
import net.bitbylogic.logicutils.config.Messages;
import net.bitbylogic.logicutils.menu.RuntimeMenusManager;
import net.bitbylogic.logicutils.menu.command.RuntimeMenusCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;

@Getter
public class LogicUtils extends JavaPlugin {

    @Getter
    private static LogicUtils instance;
    private Messages messages;

    private PaperCommandManager commandManager;
    private RuntimeMenusManager runtimeMenusManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        messages = new Messages(new File(getDataFolder(), "config.yml"));
        commandManager = new PaperCommandManager(this);
        runtimeMenusManager = new RuntimeMenusManager(this);

        commandManager.registerDependency(RuntimeMenusManager.class, runtimeMenusManager);

        registerCommand(
                new ItemDebugComand(),
                new ItemModifyCommand(),
                new RawMessageCommand(),
                new LocationCommand(),
                new RuntimeMenusCommand(),
                new SilentSoundCommand()
        );
    }

    @Override
    public void onDisable() {

    }

    private void registerCommand(BaseCommand... commands) {
        Arrays.stream(commands).forEach(command -> commandManager.registerCommand(command));
    }

}
