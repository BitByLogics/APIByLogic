package net.justugh.japi.action.listener;

import lombok.AllArgsConstructor;
import net.justugh.japi.action.Action;
import net.justugh.japi.action.ActionManager;
import net.justugh.japi.action.ChatInputAction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;
import java.util.function.Predicate;

@AllArgsConstructor
public class ActionListener implements Listener {

    private final ActionManager actionManager;

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || !event.getItem().hasItemMeta()) {
            return;
        }

        actionManager.getItemActionMap().stream().filter(action -> action.matches(event.getItem())).forEach(action -> {
            action.onTrigger(event);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        List<Action> actions = actionManager.getActionMap().get(event.getPlayer().getUniqueId());

        if (actions == null) {
            return;
        }

        Predicate<Action> chatPredicate = action -> action instanceof ChatInputAction;

        actions.stream().filter(chatPredicate).forEach(action -> {
            action.setCompleted(true);
            ((ChatInputAction) action).onTrigger(event.getPlayer(), event.getMessage());

            if (((ChatInputAction) action).isCancel()) {
                event.setCancelled(true);
            }
        });

        actions.removeIf(chatPredicate);
        actionManager.getActionMap().put(event.getPlayer().getUniqueId(), actions);
    }

}
