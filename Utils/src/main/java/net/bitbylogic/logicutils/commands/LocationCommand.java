package net.bitbylogic.logicutils.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import net.bitbylogic.logicutils.message.Messages;
import net.bitbylogic.logicutils.player.PlayerUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("ldbg|locdbg")
@CommandPermission("logicutils.command.locdbg")
public class LocationCommand extends BaseCommand {

    @Default
    public void onDefault(CommandSender sender) {
        PlayerUtil.sendRichMessages(sender,
                Messages.richFormat(Messages.listHeader("Location Debug", "Valid Commands")),
                Messages.richCommand("ldbg listchunkperisstance", "View chunk persistence data.")
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
