package net.bitbylogic.logicutils.commands;

import net.bitbylogic.apibylogic.acf.BaseCommand;
import net.bitbylogic.apibylogic.acf.annotation.CommandAlias;
import net.bitbylogic.apibylogic.acf.annotation.CommandPermission;
import net.bitbylogic.apibylogic.acf.annotation.Default;
import net.bitbylogic.apibylogic.acf.annotation.Syntax;
import net.bitbylogic.apibylogic.util.message.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("silentsound|ssound")
@CommandPermission("apibylogic.command.silentsound")
public class SilentSoundCommand extends BaseCommand {

    @Default
    @Syntax("<player> <sound> [volume] [pitch]")
    public void playSound(CommandSender sender, String target, Sound sound, @Default("1") float volume, @Default("1") float pitch) {
        Player targetPlayer = Bukkit.getPlayer(target);

        if (targetPlayer == null) {
            sender.sendMessage(Formatter.format("&e&lMessaging &8• &cThat player isn't online!"));
            return;
        }

        targetPlayer.playSound(targetPlayer.getLocation(), sound, volume, pitch);
    }

}
