package net.bitbylogic.apibylogic.module.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.apibylogic.module.Module;

@Getter
@RequiredArgsConstructor
public abstract class ModulePendingTask<T extends Module> {

    private final Class<T> clazz;

    public abstract void accept(T module);

}
