package net.bitbylogic.apibylogic.menu.impl;

import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.MenuAction;
import net.bitbylogic.apibylogic.menu.MenuData;
import net.bitbylogic.apibylogic.util.Placeholder;
import net.bitbylogic.apibylogic.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
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

        Menu menu = Menu.getFromConfig(menuSection)
                .orElse(new Menu(
                        "Confirmation-Menu",
                        "%question%",
                        27,
                        new MenuData()
                                .withModifier(questionPlaceholder)
                ));

        MenuAction confirmAction = event -> {
            if (menu.getData().getMetadata().containsKey("completed")) {
                return;
            }

            confirmConsumer.accept(null);
            menu.getData().getMetadata().put("completed", "true");
            event.getWhoClicked().closeInventory();
        };

        MenuAction cancelAction = event -> {
            if (menu.getData().getMetadata().containsKey("completed")) {
                return;
            }

            cancelConsumer.accept(null);
            menu.getData().getMetadata().put("completed", "true");
            event.getWhoClicked().closeInventory();
        };

        menu.getData().setCloseAction(event -> {
            if (menu.getData().getMetadata().containsKey("completed")) {
                return;
            }

            closeConsumer.accept(null);
            menu.getData().getMetadata().put("completed", "true");
        });

        menu.getItemOrCreate("Confirm")
                .withSlot(10)
                .item(ItemBuilder.of(Material.LIME_STAINED_GLASS_PANE).name("&a&lConfirm").build())
                .withAction(confirmAction);

        menu.getItemOrCreate("GreenPane")
                .withSlots(new ArrayList<>(List.of(0, 1, 2, 9, 11, 18, 19, 20)))
                .item(ItemBuilder.of(Material.LIME_STAINED_GLASS_PANE).name("&aClick To Confirm").build())
                .withAction(confirmAction);

        menu.getItemOrCreate("Cancel")
                .withSlot(16)
                .item(ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).name("&c&lCancel").build())
                .withAction(cancelAction);

        menu.getItemOrCreate("RedPane")
                .withSlots(new ArrayList<>(List.of(6, 7, 8, 15, 17, 23, 25, 26)))
                .item(ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).name("&c&lClick To Cancel").build())
                .withAction(cancelAction);

        menu.getItemOrCreate("Info-Item")
                .withSlot(13)
                .item(ItemBuilder.of(Material.PLAYER_HEAD).name("&a%question%").skullName("MHF_Question").build());
        
        menu.saveToConfig(APIByLogic.getInstance().getConfig());

        player.openInventory(menu.getInventory());
    }

}
