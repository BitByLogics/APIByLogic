package net.bitbylogic.apibylogic.menu.impl;

import lombok.Getter;
import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.MenuAction;
import net.bitbylogic.apibylogic.menu.MenuBuilder;
import net.bitbylogic.apibylogic.menu.MenuCloseAction;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.util.Placeholder;

@Getter
public class GenericConfirmationMenu {

    private final Menu menu;

    public GenericConfirmationMenu(String question, MenuAction confirmAction, MenuAction cancelAction, MenuCloseAction closeAction) {
        Placeholder questionPlaceholder = new Placeholder("%question%", question);

        menu = new MenuBuilder().fromConfiguration(APIByLogic.getInstance().getConfig().getConfigurationSection("Confirmation-Menu"), questionPlaceholder).getMenu();
        menu.getItem("Confirm").get().addAction(confirmAction);
        menu.getItem("GreenPane").get().addAction(confirmAction);
        menu.getItem("Cancel").get().addAction(cancelAction);
        menu.getItem("RedPane").get().addAction(cancelAction);

        menu.getData().setCloseAction(closeAction);
    }

}
