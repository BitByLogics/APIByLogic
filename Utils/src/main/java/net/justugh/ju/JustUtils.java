package net.justugh.ju;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import net.justugh.ju.commands.ItemDebugComand;
import net.justugh.ju.commands.ItemModifyCommand;
import net.justugh.ju.commands.LocationCommand;
import net.justugh.ju.commands.RawMessageCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

@Getter
public class JustUtils extends JavaPlugin {

    @Getter
    private static JustUtils instance;
    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;
        commandManager = new PaperCommandManager(this);
        registerCommand(
                new ItemDebugComand(),
                new ItemModifyCommand(),
                new RawMessageCommand(),
                new LocationCommand()
        );
    }

    @Override
    public void onDisable() {

    }

    private void registerCommand(BaseCommand... commands) {
        Arrays.stream(commands).forEach(command -> commandManager.registerCommand(command));
    }

}
