package net.bitbylogic.apibylogic.menu;

import lombok.*;
import net.bitbylogic.apibylogic.menu.item.MenuItem;
import net.bitbylogic.apibylogic.util.PlaceholderProvider;
import net.bitbylogic.apibylogic.util.GenericHashMap;
import net.bitbylogic.apibylogic.util.StringModifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class MenuData implements Cloneable {

    private @Nullable MenuCloseAction closeAction;
    private @Nullable MenuAction externalClickAction;

    private int minInventories = 1;
    private int maxInventories = -1;

    private final @NonNull List<MenuItem> itemStorage;
    private final @NonNull List<MenuFlag> flags;
    private final @NonNull List<Integer> validSlots;

    private final @NonNull List<PlaceholderProvider> placeholderProviders;
    private final @NonNull List<StringModifier> modifiers;

    private final @NonNull GenericHashMap<String, Object> metadata;

    public MenuData() {
        this.itemStorage = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.validSlots = new ArrayList<>();

        this.placeholderProviders = new ArrayList<>();
        this.modifiers = new ArrayList<>();

        this.metadata = new GenericHashMap<>();
    }

    public MenuData withCloseAction(@NonNull MenuCloseAction closeAction) {
        this.closeAction = closeAction;
        return this;
    }

    public MenuData withExternalClickAction(@NonNull MenuAction externalClickAction) {
        this.externalClickAction = externalClickAction;
        return this;
    }

    public MenuData withMinInventories(int minInventories) {
        this.minInventories = minInventories;
        return this;
    }

    public MenuData withMaxInventories(int maxInventories) {
        this.maxInventories = maxInventories;
        return this;
    }

    public MenuData withStoredItem(@NonNull MenuItem menuItem) {
        this.itemStorage.add(menuItem);
        return this;
    }

    public MenuData withStoredItems(@NonNull List<MenuItem> storedItems) {
        this.itemStorage.addAll(storedItems);
        return this;
    }

    public MenuData withFlag(@NonNull MenuFlag flag) {
        flags.add(flag);
        return this;
    }

    public MenuData withFlags(@NonNull List<MenuFlag> flags) {
        this.flags.addAll(flags);
        return this;
    }

    public MenuData withValidSlots(@NonNull List<Integer> validSlots) {
        this.validSlots.addAll(validSlots);
        return this;
    }

    public MenuData withPlaceholderProvider(@NonNull PlaceholderProvider placeholderProvider) {
        this.placeholderProviders.add(placeholderProvider);
        return this;
    }

    public MenuData withPlaceholderProviders(@NonNull List<PlaceholderProvider> placeholderProviders) {
        this.placeholderProviders.addAll(placeholderProviders);
        return this;
    }

    public MenuData withModifier(@NonNull StringModifier modifier) {
        this.modifiers.add(modifier);
        return this;
    }

    public MenuData withModifiers(@NonNull List<StringModifier> modifiers) {
        this.modifiers.addAll(modifiers);
        return this;
    }

    public MenuData withMetadata(@NonNull String key, @NonNull Object value) {
        metadata.put(key, value);
        return this;
    }

    public MenuData withMetadata(@NonNull GenericHashMap<String, Object> metadata) {
        this.metadata.putAll(metadata);
        return this;
    }

    public boolean hasFlag(@NonNull MenuFlag flag) {
        return flags.contains(flag);
    }

    public void addModifier(@NonNull StringModifier modifier) {
        modifiers.add(modifier);
    }

    public Optional<MenuItem> getStoredItem(String id) {
        return itemStorage.stream().filter(item -> item.getId().equalsIgnoreCase(id)).findFirst();
    }

    public MenuItem getStoredItemOrCreate(String id) {
        Optional<MenuItem> optionalItem = itemStorage.stream().filter(item -> item.getId().equalsIgnoreCase(id)).findFirst();

        if(optionalItem.isPresent()) {
            return optionalItem.get();
        }

        MenuItem fallbackItem = new MenuItem(id);

        itemStorage.add(fallbackItem);
        return fallbackItem;
    }

    public Optional<MenuItem> getFillerItem() {
        return itemStorage.stream().filter(MenuItem::isFiller).findFirst();
    }

    @Override
    public MenuData clone() {
        List<MenuItem> itemStorage = new ArrayList<>();
        this.itemStorage.forEach(item -> itemStorage.add(item.clone()));

        GenericHashMap<String, Object> metadata = new GenericHashMap<>();
        metadata.putAll(this.metadata);

        return new MenuData(closeAction, externalClickAction, minInventories,
                maxInventories, itemStorage, new ArrayList<>(flags), new ArrayList<>(validSlots),
                new ArrayList<>(placeholderProviders), new ArrayList<>(modifiers), metadata);
    }
}
