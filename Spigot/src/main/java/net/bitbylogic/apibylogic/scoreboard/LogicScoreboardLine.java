package net.bitbylogic.apibylogic.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

@Data
@AllArgsConstructor
public class LogicScoreboardLine {

    private final String id;
    private final int position;
    private final Scoreboard board;
    private final Team team;
    private final ChatColor color;
    private final String originalText;

    @Setter
    private boolean update;

    public String getCurrentText() {
        return team.getPrefix();
    }

}
