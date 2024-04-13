package net.justugh.japi.scoreboard;

import lombok.Getter;
import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.message.MessageProvider;
import net.justugh.japi.util.Format;
import net.justugh.japi.util.StringModifier;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
public class JustBoard {

    private final String id;

    private final Scoreboard scoreboard;
    private final Objective objective;
    private int currentLine = 15;
    private MessageProvider messageProvider;
    private List<StringModifier> modifiers = new ArrayList<>();

    private List<JustBoardLine> lines = new ArrayList<>();

    private BukkitTask updateTask;

    public JustBoard(String id, String title) {
        this.id = id;

        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective(UUID.randomUUID().toString(), "dummy", Format.format(title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        JustAPIPlugin.getInstance().getActiveBoards().add(this);
    }

    public JustBoard addLine(String id, String text) {
        return addLine(id, text, false);
    }

    public JustBoard addLine(String id, String text, boolean update) {
        Team newLabel = scoreboard.registerNewTeam(id);
        ChatColor color = getAvailableColor();
        newLabel.addEntry(color.toString());
        newLabel.setPrefix(Format.format(text));
        int line = currentLine--;
        objective.getScore(color.toString()).setScore(line);
        lines.add(new JustBoardLine(id, line, scoreboard, newLabel, color, text, update));
        return this;
    }

    public JustBoard addBlankLine() {
        addLine(UUID.randomUUID().toString(), "");
        return this;
    }

    public JustBoardLine getLine(int line) {
        return lines.stream().filter(l -> l.getPosition() == line).findFirst().orElse(null);
    }

    public JustBoardLine getLine(String id) {
        return lines.stream().filter(l -> l.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public JustBoard removeLine(int line) {
        JustBoardLine justBoardLine = getLine(line);
        justBoardLine.getTeam().unregister();
        scoreboard.resetScores(justBoardLine.getColor().toString());
        currentLine++;
        return this;
    }

    public JustBoard startUpdateTask(JavaPlugin plugin, long updateTime) {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            lines.forEach(line -> {
                if (!line.isUpdate()) {
                    return;
                }

                String text = Format.format(line.getOriginalText());

                for (StringModifier modifier : modifiers) {
                    text = modifier.modify(text);
                }

                line.getTeam().setPrefix(messageProvider == null ? text : messageProvider.applyPlaceholders(text));
            });
        }, 0, updateTime);
        return this;
    }

    public JustBoard withMessageProvider(MessageProvider messageProvider) {
        this.messageProvider = messageProvider;
        return this;
    }

    public JustBoard withModifier(StringModifier modifier) {
        this.modifiers.add(modifier);
        return this;
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

    public static Optional<JustBoard> fromConfiguration(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }

        String id = section.getName();
        String title = section.getString("Title");

        JustBoard justBoard = new JustBoard(id, title);
        section.getStringList("Lines").forEach(line -> justBoard.addLine(UUID.randomUUID().toString(), line));

        return Optional.of(justBoard);
    }

}
