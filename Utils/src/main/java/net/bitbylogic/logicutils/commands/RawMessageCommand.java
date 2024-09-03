package net.bitbylogic.logicutils.commands;

import net.bitbylogic.apibylogic.acf.BaseCommand;
import net.bitbylogic.apibylogic.acf.annotation.*;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
import net.bitbylogic.logicutils.LogicUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("rawmsg")
@CommandPermission("logicutils.command.rawmessage")
public class RawMessageCommand extends BaseCommand {

    @Dependency
    private LogicUtils plugin;

    @Default
    @Syntax("<target> <message>")
    @CommandCompletion("@players *")
    public void onCommand(CommandSender sender, String target, String message) {
        if (target.equals("*")) {
            sender.sendMessage(Formatter.format("&e&lMessaging &8• &aSending raw message to everyone."));
            Bukkit.getOnlinePlayers().forEach(player -> Formatter.sendRawMessage(player, message));
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(target);

        if (targetPlayer == null) {
            sender.sendMessage(Formatter.format("&e&lMessaging &8• &cThat player isn't online!"));
            return;
        }

        List<String> messages = new ArrayList<>();
        for (String s : message.split("/\\+")) {
            messages.add(Formatter.format(s));
        }

        messages.forEach(targetPlayer::sendMessage);
    }

}
