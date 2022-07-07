package net.justugh.japi.menu.placeholder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.justugh.japi.menu.MenuItem;
import org.bukkit.OfflinePlayer;

@Getter
@AllArgsConstructor
public abstract class UserPlaceholderProvider {

    private final String identifier;

    public abstract String getValue(MenuItem menuItem, OfflinePlayer player);
}
