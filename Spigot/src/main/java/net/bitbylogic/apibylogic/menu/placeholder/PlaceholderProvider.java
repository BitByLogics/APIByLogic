package net.bitbylogic.apibylogic.menu.placeholder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.bitbylogic.apibylogic.util.Placeholder;

@Getter
@AllArgsConstructor
public abstract class PlaceholderProvider {

    private final String identifier;
    
    public abstract String getValue();

    public Placeholder asPlaceholder() {
        return new Placeholder(identifier, getValue());
    }

}
