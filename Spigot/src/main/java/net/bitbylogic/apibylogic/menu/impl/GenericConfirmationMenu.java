package net.bitbylogic.apibylogic.menu.impl;

import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.MenuAction;
import net.bitbylogic.apibylogic.menu.MenuBuilder;
import net.bitbylogic.apibylogic.menu.MenuCloseAction;
import net.bitbylogic.apibylogic.util.Placeholder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

@Getter
public class GenericConfirmationMenu {

    public GenericConfirmationMenu(@NonNull Player player, @NonNull String question, @NonNull Consumer<Void> confirmConsumer,
                                   @NonNull Consumer<Void> cancelConsumer, @NonNull Consumer<Void> closeConsumer) {
        ConfigurationSection menuSection = APIByLogic.getInstance().getConfig().getConfigurationSection("Confirmation-Menu");

        if (menuSection == null) {
            return;
        }

        Placeholder questionPlaceholder = new Placeholder("%question%", question);

        Menu menu = new MenuBuilder().fromConfiguration(menuSection, questionPlaceholder).getMenu();

        MenuAction confirmAction = event -> {
            if(menu.getData().getMetaData().containsKey("completed")) {
                return;
            }

            confirmConsumer.accept(null);
            menu.getData().getMetaData().put("completed", "true");
            event.getWhoClicked().closeInventory();
        };

        MenuAction cancelAction = event -> {
            if(menu.getData().getMetaData().containsKey("completed")) {
                return;
            }

            cancelConsumer.accept(null);
            menu.getData().getMetaData().put("completed", "true");
            event.getWhoClicked().closeInventory();
        };

        MenuCloseAction closeAction = event -> {
            if(menu.getData().getMetaData().containsKey("completed")) {
                return;
            }

            closeConsumer.accept(null);
            menu.getData().getMetaData().put("completed", "true");
        };

        menu.getItem("Confirm").ifPresent(menuItem -> menuItem.addAction(confirmAction));
        menu.getItem("GreenPane").ifPresent(menuItem -> menuItem.addAction(confirmAction));
        menu.getItem("Cancel").ifPresent(menuItem -> menuItem.addAction(cancelAction));
        menu.getItem("RedPane").ifPresent(menuItem -> menuItem.addAction(cancelAction));

        menu.getData().setCloseAction(closeAction);

        player.openInventory(menu.getInventory());
    }

}
