package net.bitbylogic.apibylogic.menu.task;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.MenuFlag;
import net.bitbylogic.apibylogic.util.PlaceholderProvider;
import net.bitbylogic.apibylogic.util.Placeholder;
import net.bitbylogic.apibylogic.util.StringModifier;
import net.bitbylogic.apibylogic.util.message.format.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class TitleUpdateTask {

    private final @NonNull Menu menu;
    private int taskId = -1;

    public void start() {
        if(menu.getData().hasFlag(MenuFlag.DISABLE_TITLE_UPDATE)) {
            return;
        }

        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(APIByLogic.getInstance(), this::run, 5, 1).getTaskId();
    }

    public void cancel() {
        if (taskId == -1) {
            return;
        }

        Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    private void run() {
        List<StringModifier> modifiers = new ArrayList<>();
        modifiers.addAll(menu.getData().getModifiers());
        modifiers.addAll(menu.getData().getPlaceholderProviders().stream().map(PlaceholderProvider::asPlaceholder).toList());

        Placeholder pagesPlaceholder = new Placeholder("%pages%", menu.getInventories().size() + "");
        modifiers.add(pagesPlaceholder);

        menu.getInventories().forEach(menuInventory -> {
            Inventory inventory = menuInventory.getInventory();

            List<StringModifier> finalModifiers = new ArrayList<>(modifiers);

            Placeholder pagePlaceholder = new Placeholder("%page%", (menu.getInventories().indexOf(menuInventory) + 1) + "");
            finalModifiers.add(pagePlaceholder);

            new ArrayList<>(inventory.getViewers()).forEach(viewer -> {
                String newTitle = Formatter.format(menuInventory.getTitle(), finalModifiers.toArray(new StringModifier[]{}));

                if (viewer.getOpenInventory().getTitle().equalsIgnoreCase(newTitle)) {
                    return;
                }

                viewer.getOpenInventory().setTitle(newTitle);
            });
        });
    }

    public boolean isActive() {
        return taskId != -1;
    }

}
