package net.bitbylogic.apibylogic.menu.listener;

import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftPackage;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.menu.Menu;
import net.bitbylogic.apibylogic.menu.MenuFlag;
import net.bitbylogic.apibylogic.menu.placeholder.PlaceholderProvider;
import net.bitbylogic.apibylogic.util.Placeholder;
import net.bitbylogic.apibylogic.util.StringModifier;
import net.bitbylogic.apibylogic.util.inventory.InventoryUpdate;
import net.bitbylogic.apibylogic.util.inventory.InventoryUtil;
import net.bitbylogic.apibylogic.util.message.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MenuListener implements Listener {

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        Inventory topInventory = InventoryUtil.getViewInventory(event, "getTopInventory");
        Inventory bottomInventory = InventoryUtil.getViewInventory(event, "getBottomInventory");

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!(topInventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) topInventory.getHolder();

        if (event.getClick() == ClickType.NUMBER_KEY && event.getClickedInventory() == topInventory) {
            event.setCancelled(true);
            return;
        }

        if (event.isShiftClick() && event.getClickedInventory() != topInventory) {
            event.setCancelled(!menu.getData().hasFlag(MenuFlag.ALLOW_INPUT));
            return;
        }

        if (event.getClickedInventory() == bottomInventory) {
            if (menu.getData().getExternalClickAction() != null) {
                menu.getData().getExternalClickAction().onClick(event);
            }

            event.setCancelled(!menu.getData().hasFlag(MenuFlag.LOWER_INTERACTION));
            return;
        }

        if (event.getClickedInventory() != topInventory) {
            return;
        }

        if (!menu.getItem(topInventory, event.getSlot()).isPresent() && (event.getCursor() == null || event.getCursor().getType() == Material.AIR) && menu.getData().hasFlag(MenuFlag.ALLOW_REMOVAL)) {
            return;
        }

        event.setCancelled(menu.getItem(topInventory, event.getSlot()).isPresent() || !menu.getData().hasFlag(MenuFlag.ALLOW_INPUT));

        menu.getItems(topInventory, event.getSlot()).forEach(menuItem -> {
            if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(topInventory, menuItem, menu))) {
                return;
            }

            if (menuItem.getClickRequirements().stream().anyMatch(requirement -> !requirement.canClick((Player) event.getWhoClicked()))) {
                return;
            }

            menuItem.onClick(event);
        });
    }

    @EventHandler
    public void onTransfer(InventoryMoveItemEvent event) {
        Inventory inventory = event.getDestination();

        if (!(inventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) inventory.getHolder();

        event.setCancelled(!menu.getData().hasFlag(MenuFlag.ALLOW_INPUT));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        Inventory bottomInventory = InventoryUtil.getViewInventory(event, "getBottomInventory");

        if (!(inventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) inventory.getHolder();

        if (inventory == bottomInventory && menu.getData().hasFlag(MenuFlag.LOWER_INTERACTION)) {
            return;
        }

        event.setCancelled(!menu.getData().hasFlag(MenuFlag.ALLOW_INPUT));
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();

        if (!(inventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) inventory.getHolder();

        List<StringModifier> modifiers = new ArrayList<>();
        modifiers.addAll(menu.getData().getModifiers());
        modifiers.addAll(menu.getData().getPlaceholderProviders().stream().map(PlaceholderProvider::asPlaceholder).collect(Collectors.toList()));

        Placeholder pagesPlaceholder = new Placeholder("%pages%", menu.getInventories().size() + "");
        Placeholder pagePlaceholder = new Placeholder("%page%", (menu.getInventoryIndex(inventory) + 1) + "");
        modifiers.add(pagesPlaceholder);
        modifiers.add(pagePlaceholder);

        if (!menu.getData().hasFlag(MenuFlag.DISABLE_TITLE_UPDATE)) {
            Bukkit.getScheduler().runTaskLater(APIByLogic.getInstance(), () -> {
                if (XReflection.MINOR_NUMBER >= 20) {
                    try {
                        XReflection.ofMinecraft()
                                .inPackage(MinecraftPackage.BUKKIT, "inventory")
                                .named("InventoryView")
                                .method().named("setTitle")
                                .parameters(String.class).returns(void.class)
                                .reflect().invoke(event.getPlayer().getOpenInventory(), Formatter.format(menu.getTitle(),
                                        modifiers.toArray(new StringModifier[]{})));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return;
                }

                InventoryUpdate.updateInventory(APIByLogic.getInstance(), (Player) event.getPlayer(),
                        Formatter.format(menu.getMenuInventory(inventory).getTitle(), modifiers.toArray(new StringModifier[]{})));
            }, 1);
        }

        menu.getActivePlayers().add(event.getPlayer().getUniqueId());

        if (menu.getTitleUpdateTask() != null && !menu.getTitleUpdateTask().isActive()) {
            menu.getTitleUpdateTask().startTask();
        }

        if (menu.getUpdateTask() == null || menu.getUpdateTask().isActive()) {
            return;
        }

        menu.getUpdateTask().startTask();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (XReflection.MINOR_NUMBER < 20) {
            InventoryUpdate.getLastSentTitle().remove(event.getPlayer().getUniqueId());
        }

        Inventory inventory = event.getInventory();

        if (!(inventory.getHolder() instanceof Menu)) {
            return;
        }

        Menu menu = (Menu) inventory.getHolder();

        menu.getActivePlayers().remove(event.getPlayer().getUniqueId());

        if (event.getViewers().stream().filter(p -> !p.getUniqueId().equals(event.getPlayer().getUniqueId())).count() == 0) {
            menu.getUpdateTask().cancelTask();

            if (menu.getTitleUpdateTask() != null) {
                menu.getTitleUpdateTask().cancelTask();
            }
        }

        if (menu.getData().getCloseAction() == null) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(APIByLogic.getInstance(), () -> ((Player) event.getPlayer()).updateInventory(), 1);
        Bukkit.getScheduler().runTaskLater(APIByLogic.getInstance(), () -> menu.getData().getCloseAction().onClose(event), 2);
    }

}
