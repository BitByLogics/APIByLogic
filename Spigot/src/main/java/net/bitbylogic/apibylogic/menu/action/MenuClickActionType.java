package net.bitbylogic.apibylogic.menu.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.bitbylogic.apibylogic.util.Placeholder;
import net.bitbylogic.apibylogic.util.RichTextUtil;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Getter
@AllArgsConstructor
public enum MenuClickActionType {

    RUN_CONSOLE_COMMAND((event, args) -> {
        for (String command : RichTextUtil.getRichText(args, 0)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Formatter.format(command, new Placeholder("%player%", event.getWhoClicked().getName())));
        }
    }),
    RUN_PLAYER_COMMAND((event, args) -> {
        for (String command : RichTextUtil.getRichText(args, 0)) {
            ((Player) event.getWhoClicked()).performCommand(Formatter.format(command, new Placeholder("%player%", event.getWhoClicked().getName())));
        }
    }),
    SEND_MESSAGE((event, args) -> {
        for (String message : RichTextUtil.getRichText(args, 0)) {
            event.getWhoClicked().sendMessage(Formatter.format(message, new Placeholder("%player%", event.getWhoClicked().getName())));
        }
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
