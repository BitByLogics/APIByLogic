package net.bitbylogic.apibylogic.util.config.wrapper;

import lombok.NonNull;
import org.bukkit.configuration.file.FileConfiguration;

public interface LogicConfigWrapper<T> {

    void wrap(@NonNull T object, @NonNull String path, @NonNull FileConfiguration config);

    <W> T unwrap(@NonNull W wrappedObject, @NonNull Class<?> requestedClass);

}
