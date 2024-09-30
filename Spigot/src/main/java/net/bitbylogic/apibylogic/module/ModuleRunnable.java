package net.bitbylogic.apibylogic.module;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class ModuleRunnable {

    @Setter(AccessLevel.PROTECTED)
    private ModuleTask task;

    public void run() {
        if (task == null) {
            return;
        }

        task.run();
    }

    public void cancel() {
        if (task == null) {
            return;
        }

        task.cancel();
    }

}
