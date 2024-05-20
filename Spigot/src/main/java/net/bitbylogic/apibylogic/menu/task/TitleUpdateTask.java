package net.bitbylogic.apibylogic.menu.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.placeholder.PlaceholderProvider;
import net.bitbylogic.apibylogic.util.InventoryUpdate;
import net.bitbylogic.apibylogic.util.Placeholder;
import net.bitbylogic.apibylogic.util.StringModifier;
import net.bitbylogic.apibylogic.util.message.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TitleUpdateTask {

    private final Menu menu;

    private int taskId;

    @Getter
    private boolean active;

    public void startTask() {
        active = true;

        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(APIByLogic.getInstance(), this::run, 5, 1).getTaskId();
    }

    public void cancelTask() {
        if (!active) {
            return;
        }

        Bukkit.getScheduler().cancelTask(taskId);
        active = false;
    }

    private void run() {
        List<StringModifier> modifiers = new ArrayList<>();
        modifiers.addAll(menu.getData().getModifiers());
        modifiers.addAll(menu.getData().getPlaceholderProviders().stream().map(PlaceholderProvider::asPlaceholder).collect(Collectors.toList()));

        Placeholder pagesPlaceholder = new Placeholder("%pages%", menu.getInventories().size() + "");
        modifiers.add(pagesPlaceholder);

        menu.getInventories().forEach(menuInventory -> {
            Inventory inventory = menuInventory.getInventory();

            List<StringModifier> finalModifiers = new ArrayList<>(modifiers);

            Placeholder pagePlaceholder = new Placeholder("%page%", (menu.getInventories().indexOf(menuInventory) + 1) + "");
            finalModifiers.add(pagePlaceholder);

            new ArrayList<>(inventory.getViewers()).forEach(viewer -> {
                InventoryUpdate.updateInventory(APIByLogic.getInstance(), (Player) viewer, Messages.format(menuInventory.getTitle(),
                        finalModifiers.toArray(new StringModifier[]{})));
            });
        });
    }

}
