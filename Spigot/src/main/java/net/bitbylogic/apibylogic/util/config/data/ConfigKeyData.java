package net.bitbylogic.apibylogic.util.config.data;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ConfigKeyData {

    private final String path;
    private final String wrapperId;

    public static ConfigKeyData of(@NonNull String path) {
        return new ConfigKeyData(path, "");
    }

    public static ConfigKeyData of(@NonNull String path, @NonNull String wrapperId) {
        return new ConfigKeyData(path, wrapperId);
    }

}
