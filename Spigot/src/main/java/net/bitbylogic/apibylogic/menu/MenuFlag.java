package net.bitbylogic.apibylogic.menu;

public enum MenuFlag {

    /**
     * Allows the player to add items to the inventory
     */
    ALLOW_INPUT,
    /**
     * Allows the player to remove items from the inventory
     */
    ALLOW_REMOVAL,
    /**
     * Prevent the menus inventories from having their titles updated
     */
    DISABLE_TITLE_UPDATE,
    /**
     * Forces the inventory to always display its navigation items
     */
    ALWAYS_DISPLAY_NAV,
    /**
     * Allows the player to interact with their inventory while the menu is open
     */
    LOWER_INTERACTION,
    /**
     * Enables debug mode for the inventory, prints debug information to console
     */
    DEBUG;

}
