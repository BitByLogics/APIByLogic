package net.justugh.ju;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import net.justugh.ju.commands.*;
import net.justugh.ju.menu.RuntimeMenusManager;
import net.justugh.ju.menu.command.RuntimeMenusCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

@Getter
public class JustUtils extends JavaPlugin {

    @Getter
    private static JustUtils instance;
    private PaperCommandManager commandManager;
    private RuntimeMenusManager runtimeMenusManager;

    @Override
    public void onEnable() {
        instance = this;
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
