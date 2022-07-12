package net.justugh.japi.scoreboard;

import lombok.Getter;
import net.justugh.japi.util.Format;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class JustBoard {

    private final Scoreboard scoreboard;
    private final Objective objective;
    private int currentLine = 15;

    private List<JustBoardLine> lines = new ArrayList<>();

    public JustBoard(String name) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective(UUID.randomUUID().toString(), "dummy", Format.format(name));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public JustBoard addLine(String id, String message) {
        Team newLabel = scoreboard.registerNewTeam(id);
        ChatColor color = getAvailableColor();
        newLabel.addEntry(color.toString());
        newLabel.setPrefix(Format.format(message));
        int line = currentLine--;
        objective.getScore(color.toString()).setScore(line);
        lines.add(new JustBoardLine(id, line, newLabel, color));
        return this;
    }

    public JustBoard addBlankLine() {
        addLine(UUID.randomUUID().toString(), "");
        return this;
    }

    public JustBoardLine getLine(int line) {
        return lines.stream().filter(l -> l.getLine() == line).findFirst().orElse(null);
    }

    public JustBoardLine getLine(String id) {
        return lines.stream().filter(l -> l.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public void removeLine(int line) {
        JustBoardLine justBoardLine = getLine(line);
        justBoardLine.getTeam().unregister();
        scoreboard.resetScores(justBoardLine.getColor().toString());
        currentLine++;
    }

    private ChatColor getAvailableColor() {
        for (ChatColor color : ChatColor.values()) {
            if (lines.stream().anyMatch(line -> line.getColor() == color)) {
                continue;
            }

            return color;
        }

        int R = (int) (Math.random() * 256);
        int G = (int) (Math.random() * 256);
        int B = (int) (Math.random() * 256);

        return ChatColor.of(new Color(R, G, B));
    }

}
