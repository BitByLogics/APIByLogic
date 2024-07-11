package net.bitbylogic.apibylogic.module.command;

import net.bitbylogic.apibylogic.module.Module;
import net.bitbylogic.apibylogic.module.ModuleData;
import net.bitbylogic.apibylogic.module.ModuleInterface;
import net.bitbylogic.apibylogic.module.ModuleManager;
import net.bitbylogic.apibylogic.util.NumberUtil;
import net.bitbylogic.apibylogic.util.message.Formatter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ModulesCommand extends ModuleCommand {

    private final ModuleManager moduleManager;

    public ModulesCommand(ModuleManager moduleManager) {
        super("module", new String[]{"mdl", "modules", "mdls"}, "apibylogic.admin");

        this.moduleManager = moduleManager;
        setEnabled(true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Formatter.listHeader("Module Commands", ""));
            sender.sendMessage(Formatter.command("module list <page>", "List all modules."));
            sender.sendMessage(Formatter.command("module reload <id>", "Reload the specified module's config."));
            sender.sendMessage(Formatter.command("module enable <id>", "Enable the specified module."));
            sender.sendMessage(Formatter.command("module disable <id>", "Disable the specified module."));
            sender.sendMessage(Formatter.command("module toggle <id>", "Toggles the specified module."));
            return;
        }

        if (args[0].equalsIgnoreCase("list")) {
            int page = 1;

            if (args.length >= 2) {
                if (!NumberUtil.isNumber(args[1])) {
                    sender.sendMessage(Formatter.error("Modules", "Invalid page."));
                    return;
                }

                page = Integer.parseInt(args[1]);
            }

            displayPage(sender, page);
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (args.length < 2) {
                sender.sendMessage(Formatter.error("Modules", "Invalid module ID."));
                return;
            }

            Module module = moduleManager.getModuleByID(args[1]);

            if (module == null) {
                sender.sendMessage(Formatter.error("Modules", "Invalid module."));
                return;
            }

            if (!module.isEnabled()) {
                sender.sendMessage(Formatter.error("Modules", "That module isn't enabled."));
            }

            sender.sendMessage(Formatter.success("Modules", String.format("Reloading module! <c#separator>(<c#success_secondary>Name<c#separator>: %s<c#separator>, <c#success_secondary>ID<c#separator>: %s<c#separator>)", module.getModuleData().getName(), module.getModuleData().getId())));
            module.reloadConfig();
            module.onReload();
            return;
        }

        if (args[0].equalsIgnoreCase("enable")) {
            if (args.length < 2) {
                sender.sendMessage(Formatter.error("Modules", "Invalid module ID."));
                return;
            }

            Module module = moduleManager.getModuleByID(args[1]);

            if (module == null) {
                sender.sendMessage(Formatter.error("Modules", "Invalid module."));
                return;
            }

            if (module.isEnabled()) {
                sender.sendMessage(Formatter.error("Modules", "That module isn't disabled."));
            }

            sender.sendMessage(Formatter.success("Modules", "Enabling module! <c#separator>(<c#success_secondary>Name<c#separator>: %s<c#separator>, <c#success_secondary>ID<c#separator>: %s<c#separator>)", module.getModuleData().getName(), module.getModuleData().getId()));
            moduleManager.enableModule(module.getModuleData().getId());
            return;
        }

        if (args[0].equalsIgnoreCase("disable")) {
            if (args.length < 2) {
                sender.sendMessage(Formatter.error("Modules", "Invalid module ID."));
                return;
            }

            Module module = moduleManager.getModuleByID(args[1]);

            if (module == null) {
                sender.sendMessage(Formatter.error("Modules", "Invalid module."));
                return;
            }

            if (!module.isEnabled()) {
                sender.sendMessage(Formatter.error("Modules", "That module isn't enabled."));
            }

            sender.sendMessage(Formatter.success("Modules", "Disabling module! <c#separator>(<c#success_secondary>Name<c#separator>: %s<c#separator>, <c#success_secondary>ID<c#separator>: %s<c#separator>)", module.getModuleData().getName(), module.getModuleData().getId()));
            moduleManager.disableModule(module.getModuleData().getId());
            return;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            if (args.length < 2) {
                sender.sendMessage(Formatter.error("Modules", "Invalid module ID."));
                return;
            }

            Module module = moduleManager.getModuleByID(args[1]);

            if (module == null) {
                sender.sendMessage(Formatter.error("Modules", "Invalid module."));
                return;
            }

            if (module.isEnabled()) {
                module.setEnabled(false);
                sender.sendMessage(Formatter.success("Modules", "Disabling module! <c#separator>(<c#success_secondary>Name<c#separator>: %s<c#separator>, <c#success_secondary>ID<c#separator>: %s<c#separator>)", module.getModuleData().getName(), module.getModuleData().getId()));
                moduleManager.disableModule(module.getModuleData().getId());
                return;
            }

            module.setEnabled(true);
            sender.sendMessage(Formatter.success("Modules", "Enabling module! <c#separator>(<c#success_secondary>Name<c#separator>: %s<c#separator>, <c#success_secondary>ID<c#separator>: %s<c#separator>)", module.getModuleData().getName(), module.getModuleData().getId()));
            moduleManager.enableModule(module.getModuleData().getId());
        }
    }

    private void displayPage(CommandSender sender, int page) {
        List<Module> modules = new ArrayList<>(moduleManager.getModules().values());
        int pages = modules.size() / 10.0d % 1 == 0 ? modules.size() / 10 : modules.size() / 10 + 1;
        int lastPossibleModule = modules.size();

        if (page == 0 || page > pages) {
            sender.sendMessage(Formatter.error("Modules", "Invalid page&8: &f%s", page));
            return;
        }

        int startingModule = (page * 10) - 10;
        int lastModule = Math.min(startingModule + 10, lastPossibleModule);

        sender.sendMessage(Formatter.format("<c#separator>&m     &r <c#separator>( <c#primary>&lMODULE LIST <c#separator>)&m     "));

        for (int i = startingModule; i < lastModule; i++) {
            Module module = modules.get(i);

            BaseComponent moduleComponent = Formatter.richFormat(
                    "<c#separator>- <c#primary>%s <c#separator>(<c#secondary>ID<c#separator>: <c#highlight>%s<c#separator>, <c#secondary>Status<c#separator>: <c#highlight>%s<c#separator>)",
                    module.getModuleData().getName(),
                    module.getModuleData().getId(),
                    Formatter.format(module.isEnabled() ? "<c#success_highlight>Enabled" : "<c#error_highlight>Disabled"));

            moduleComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.richFormat("<c#highlight>%s", module.getModuleData().getDescription()))));
            moduleComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/module toggle " + module.getModuleData().getId()));

            sender.spigot().sendMessage(moduleComponent);
        }

        sender.sendMessage(Formatter.replace("<c#separator>&m        &r <c#separator>( <c#secondary>Page<c#separator>: <c#highlight>%s <c#separator>)&m        ", page));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("list", "reload", "enable", "disable"), new ArrayList<>());
        }

        if (args.length != 2) {
            return new ArrayList<>();
        }

        if (args[0].equalsIgnoreCase("list")) {
            return StringUtil.copyPartialMatches(args[1], Collections.singleton("1"), new ArrayList<>());
        }

        return StringUtil.copyPartialMatches(args[1], moduleManager.getModules().values().stream()
                .map(ModuleInterface::getModuleData).map(ModuleData::getId).collect(Collectors.toSet()), new ArrayList<>());
    }
}
