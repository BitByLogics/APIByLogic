package net.justugh.japi.scoreboard;

import lombok.Data;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.scoreboard.Team;

@Data
public class JustBoardLine {

    private final String id;
    private final int line;
    private final Team team;
    private final ChatColor color;

}
