package net.bitbylogic.apibylogic.util.config.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.apibylogic.util.config.wrapper.LogicConfigWrapper;

import java.lang.reflect.Field;

@RequiredArgsConstructor
@Getter
public class ConfigFieldData {

    private final Field field;
    private final String fieldPath;
    private final LogicConfigWrapper wrapper;

}
