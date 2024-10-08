package net.bitbylogic.logicutils;

import lombok.Getter;
import net.bitbylogic.apibylogic.acf.BaseCommand;
import net.bitbylogic.apibylogic.acf.PaperCommandManager;
import net.bitbylogic.logicutils.commands.*;
import net.bitbylogic.logicutils.menu.RuntimeMenusManager;
import net.bitbylogic.logicutils.menu.command.RuntimeMenusCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

@Getter
public class LogicUtils extends JavaPlugin {

    @Getter
    private static LogicUtils instance;

    private PaperCommandManager commandManager;
    private RuntimeMenusManager runtimeMenusManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

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
