package net.justugh.japi.action;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import net.justugh.japi.action.listener.ActionListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Getter
public class ActionManager implements Listener {

    private final JavaPlugin plugin;

    private final List<ItemInteractAction> itemActionMap;
    private final HashMap<UUID, List<Action>> actionMap;

    public ActionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        itemActionMap = Lists.newArrayList();
        actionMap = Maps.newHashMap();

        Bukkit.getServer().getPluginManager().registerEvents(new ActionListener(this), plugin);
    }

    public void addItemAction(ItemInteractAction action, String itemIdentifier) {
        action.setItemIdentifier(itemIdentifier);
        itemActionMap.add(action);
    }

    public void trackAction(Player player, Action action) {
        List<Action> actions = actionMap.getOrDefault(player.getUniqueId(), Lists.newArrayList());
        actions.add(action);
        actionMap.put(player.getUniqueId(), actions);

        if (action.getExpireTime() != -1) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                List<Action> actions1 = actionMap.getOrDefault(player.getUniqueId(), Lists.newArrayList());
                actions1.remove(action);
                actionMap.put(player.getUniqueId(), actions1);

                if (!action.isCompleted()) {
                    action.onExpire(player);
                }
            }, action.getExpireTime());
        }
    }

}
