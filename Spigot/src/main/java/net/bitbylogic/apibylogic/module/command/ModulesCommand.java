package net.bitbylogic.apibylogic.module.command;

import net.bitbylogic.apibylogic.util.NumberUtil;
import net.bitbylogic.apibylogic.module.Module;
import net.bitbylogic.apibylogic.module.ModuleManager;
import net.bitbylogic.apibylogic.util.Format;
import net.bitbylogic.apibylogic.util.message.Messages;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ModulesCommand extends ModuleCommand {

    private final ModuleManager moduleManager;

    public ModulesCommand(ModuleManager moduleManager) {
        super("modules", new String[]{"mdls"}, "apibylogic.admin");

        this.moduleManager = moduleManager;
        setEnabled(true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Messages.listHeader("Module Commands", ""));
            sender.sendMessage(Messages.command("module list <page>", "List all modules."));
            sender.sendMessage(Messages.command("module reload <id>", "Reload the specified module's config."));
            sender.sendMessage(Messages.command("module enable <id>", "Enable the specified module."));
            sender.sendMessage(Messages.command("module disable <id>", "Disable the specified module."));
            return;
        }

        if (args[0].equalsIgnoreCase("list")) {
            int page = 1;

            if (args.length >= 2) {
                if (!NumberUtil.isNumber(args[1])) {
                    sender.sendMessage(Messages.error("Modules", "Invalid page."));
                    return;
                }

                page = Integer.parseInt(args[1]);
            }

            displayPage(sender, page);
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (args.length < 2) {
                sender.sendMessage(Messages.error("Modules", "Invalid module ID."));
                return;
            }

            Module module = moduleManager.getModuleByID(args[1]);

            if (module == null) {
                sender.sendMessage(Messages.error("Modules", "Invalid module."));
                return;
            }

            if (!module.isEnabled()) {
                sender.sendMessage(Messages.error("Modules", "That module isn't enabled."));
            }

            sender.sendMessage(Messages.success("Modules", String.format("Reloading module! &7&o(Name: %s, ID: %s)", module.getModuleData().getName(), module.getModuleData().getId())));
            module.reloadConfig();
            module.onReload();
            return;
        }

        if (args[0].equalsIgnoreCase("enable")) {
            if (args.length < 2) {
                sender.sendMessage(Messages.error("Modules", "Invalid module ID."));
                return;
            }

            Module module = moduleManager.getModuleByID(args[1]);

            if (module == null) {
                sender.sendMessage(Messages.error("Modules", "Invalid module."));
                return;
            }

            if (module.isEnabled()) {
                sender.sendMessage(Messages.error("Modules", "That module isn't disabled."));
            }

            sender.sendMessage(Messages.success("Modules", String.format("Enabling module! &7&o(Name: %s, ID: %s)", module.getModuleData().getName(), module.getModuleData().getId())));
            moduleManager.enableModule(module.getModuleData().getId());
            return;
        }

        if (args[0].equalsIgnoreCase("disable")) {
            if (args.length < 2) {
                sender.sendMessage(Messages.error("Modules", "Invalid module ID."));
                return;
            }

            Module module = moduleManager.getModuleByID(args[1]);

            if (module == null) {
                sender.sendMessage(Messages.error("Modules", "Invalid module."));
                return;
            }

            if (!module.isEnabled()) {
                sender.sendMessage(Messages.error("Modules", "That module isn't enabled."));
            }

            sender.sendMessage(Messages.success("Modules", String.format("Disabling module! &7&o(Name: %s, ID: %s)", module.getModuleData().getName(), module.getModuleData().getId())));
            moduleManager.disableModule(module.getModuleData().getId());
        }
    }

    private void displayPage(CommandSender sender, int page) {
        List<Module> modules = new ArrayList<>(moduleManager.getModules().values());
        int pages = modules.size() / 10.0d % 1 == 0 ? modules.size() / 10 : modules.size() / 10 + 1;
        int lastPossibleModule = modules.size();

        if (page == 0 || page > pages) {
            sender.sendMessage(Messages.error("Modules", "Invalid page&8: &f%s", page));
            return;
        }

        int startingModule = (page * 10) - 10;
        int lastModule = Math.min(startingModule + 10, lastPossibleModule);

        sender.sendMessage(Messages.format("&8&m-----&r&8( &r&e&lMODULE LIST &8)&m-----"));

        for (int i = startingModule; i < lastModule; i++) {
            Module module = modules.get(i);

            sender.sendMessage(
                    Messages.richFormat(
                                    "&8- &e%s &7&o(ID: %s, Status: %s&7&o)",
                                    module.getModuleData().getName(),
                                    module.getModuleData().getId(),
                                    Format.format(module.isEnabled() ? "&a&oEnabled" : "&c&oDisabled"))
                            .hoverEvent(
                                    HoverEvent.showText(
                                            Messages.richFormat("&f%s", module.getModuleData().getDescription())
                                    )
                            )
                            .clickEvent(
                                    ClickEvent.runCommand(
                                            module.isEnabled() ? "/module disable " + module.getModuleData().getId() : "/module enable " + module.getModuleData().getId()
                                    )
                            )
            );
        }

        sender.sendMessage(Messages.replace("&8&m--------&r&8( &r&ePage&8: &f%s &8)&m--------", page));
    }

}
