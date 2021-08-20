package net.justugh.japi.menu.placeholder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class PlaceholderProvider {

    private final String identifier;

    public abstract String getValue();

}
