package net.justugh.ju.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.justugh.japi.util.Format;
import net.justugh.ju.JustUtils;
import net.justugh.ju.message.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("rawmsg")
@CommandPermission("justutils.command.rawmessage")
public class RawMessageCommand extends BaseCommand {

    @Dependency
    private JustUtils plugin;

    @Default
    @Syntax("<target> <message>")
    public void onCommand(CommandSender sender, String target, String message) {
        if (target.equals("*")) {
            sender.sendMessage(Format.format("&e&lMessaging &8• &aSending raw message to everyone."));
            Bukkit.getOnlinePlayers().forEach(player -> Messages.sendRawMessage(player, message));
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(target);

        if (targetPlayer == null) {
            sender.sendMessage(Format.format("&e&lMessaging &8• &cThat player isn't online!"));
            return;
        }

        List<String> messages = new ArrayList<>();
        for (String s : message.split("/\\+")) {
            messages.add(Format.format(s));
        }

        messages.forEach(targetPlayer::sendMessage);
    }

}
