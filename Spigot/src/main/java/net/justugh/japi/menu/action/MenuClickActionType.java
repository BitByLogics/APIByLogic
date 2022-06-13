package net.justugh.japi.menu.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.menu.Menu;
import net.justugh.japi.menu.MenuAction;
import net.justugh.japi.util.Format;
import net.justugh.japi.util.Placeholder;
import net.justugh.japi.util.RichTextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Getter
@AllArgsConstructor
public enum MenuClickActionType {

    RUN_CONSOLE_COMMAND((event, args) -> {
        for (String command : RichTextUtil.getRichText(args, 0)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Format.format(command, new Placeholder("%player%", event.getWhoClicked().getName())));
        }
    }),
    RUN_PLAYER_COMMAND((event, args) -> {
        for (String command : RichTextUtil.getRichText(args, 0)) {
            ((Player) event.getWhoClicked()).performCommand(Format.format(command, new Placeholder("%player%", event.getWhoClicked().getName())));
        }
    }),
    SEND_MESSAGE((event, args) -> {
        for (String message : RichTextUtil.getRichText(args, 0)) {
            event.getWhoClicked().sendMessage(Format.format(message, new Placeholder("%player%", event.getWhoClicked().getName())));
        }
    }),
    INTERNAL_ACTION((event, args) -> {
        MenuAction internalAction = JustAPIPlugin.getInstance().getMenuManager().getGlobalAction(args);

        if (internalAction == null) {
            return;
        }

        internalAction.onClick(event);
    });

    private final ClickTypeAction action;

    public static MenuClickActionType parseType(String name) {
        for (MenuClickActionType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return null;
    }

}
