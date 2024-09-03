package net.bitbylogic.apibylogic.util;

import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class LivePlaceholder implements StringModifier {

    private final String placeholder;
    private final Supplier<String> valueSupplier;

    @Override
    public String modify(String string) {
        return string.replace(placeholder, valueSupplier.get());
    }

}
