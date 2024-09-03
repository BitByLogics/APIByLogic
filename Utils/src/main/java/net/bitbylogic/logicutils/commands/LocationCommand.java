package net.bitbylogic.logicutils.commands;

import net.bitbylogic.apibylogic.acf.BaseCommand;
import net.bitbylogic.apibylogic.acf.annotation.CommandAlias;
import net.bitbylogic.apibylogic.acf.annotation.CommandPermission;
import net.bitbylogic.apibylogic.acf.annotation.Default;
import net.bitbylogic.apibylogic.acf.annotation.Subcommand;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
import net.bitbylogic.logicutils.player.PlayerUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("ldbg|locdbg")
@CommandPermission("logicutils.command.locdbg")
public class LocationCommand extends BaseCommand {

    @Default
    public void onDefault(CommandSender sender) {
        PlayerUtil.sendRichMessages(sender,
                Formatter.richFormat(Formatter.listHeader("Location Debug", "Valid Commands")),
                Formatter.richCommand("ldbg listchunkperisstance", "View chunk persistence data.")
        );
    }

    @Subcommand("listchunkperisstance")
    public void onListChunkPersistence(Player player) {
        player.getLocation().getChunk().getPersistentDataContainer().getKeys().forEach(key -> {
            if (!key.getNamespace().equalsIgnoreCase("abl")) {
                return;
            }

            player.sendMessage(key.getKey());
        });
    }

}
