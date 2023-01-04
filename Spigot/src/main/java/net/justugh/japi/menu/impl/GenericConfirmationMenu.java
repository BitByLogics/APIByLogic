package net.justugh.japi.menu.impl;

import lombok.Getter;
import net.justugh.japi.JustAPIPlugin;
import net.justugh.japi.menu.Menu;
import net.justugh.japi.menu.MenuAction;
import net.justugh.japi.menu.MenuBuilder;
import net.justugh.japi.menu.MenuCloseAction;
import net.justugh.japi.util.Placeholder;

@Getter
public class GenericConfirmationMenu {

    private final Menu menu;

    public GenericConfirmationMenu(String question, MenuAction confirmAction, MenuAction cancelAction, MenuCloseAction closeAction) {
        Placeholder questionPlaceholder = new Placeholder("%question%", question);

        menu = new MenuBuilder().fromConfiguration(JustAPIPlugin.getInstance().getConfig().getConfigurationSection("Confirmation-Menu"), questionPlaceholder).getMenu();
        menu.getItem("Confirm").get().addAction(confirmAction);
        menu.getItem("GreenPane").get().addAction(confirmAction);
        menu.getItem("Cancel").get().addAction(cancelAction);
        menu.getItem("RedPane").get().addAction(cancelAction);

        menu.getData().setCloseAction(closeAction);
    }

}
