package net.bitbylogic.apibylogic.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class PlaceholderProvider {

    private final String identifier;
    
    public abstract String getValue();

    public Placeholder asPlaceholder() {
        return new Placeholder(identifier, getValue());
    }

}
