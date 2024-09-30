package net.bitbylogic.apibylogic.module;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

@Getter
public abstract class ModuleTask extends ModuleRunnable {

    private final String id;
    private final ModuleTaskType type;

    @Getter(AccessLevel.PROTECTED)
    private final BukkitRunnable bukkitRunnable;

    @Setter(AccessLevel.PROTECTED)
    private Module moduleInstance;

    @Setter
    private int taskId = -1;

    public ModuleTask(@NonNull String id, @NonNull ModuleTaskType type) {
        this.id = id;
        this.type = type;

        this.bukkitRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    taskId = getTaskId();

                    if (moduleInstance == null) {
                        ModuleTask.this.cancel();
                        cancel();
                        return;
                    }

                    ModuleTask.this.run();
                } catch(Exception e) {
                    e.printStackTrace();
                    ModuleTask.this.cancel();
                    cancel();
                }
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                super.cancel();
                ModuleTask.this.cancel();
            }
        };
    }

    public ModuleTask(@NonNull String id, @NonNull ModuleTaskType type, @NonNull ModuleRunnable runnable) {
        this.id = id;
        this.type = type;
        runnable.setTask(this);

        this.bukkitRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    taskId = getTaskId();

                    if (moduleInstance == null) {
                        runnable.cancel();
                        cancel();
                        return;
                    }

                    runnable.run();
                } catch(Exception e) {
                    e.printStackTrace();
                    runnable.cancel();
                    cancel();
                }
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                super.cancel();
                runnable.cancel();
            }
        };
    }

    @Override
    public void cancel() {
        if (Bukkit.getScheduler().isCurrentlyRunning(taskId)) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        if (moduleInstance == null) {
            return;
        }

        moduleInstance.getTasks().remove(this);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ModuleTask that = (ModuleTask) object;
        return taskId == that.taskId && Objects.equals(id, that.id) && type == that.type && Objects.equals(bukkitRunnable, that.bukkitRunnable) && Objects.equals(moduleInstance, that.moduleInstance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, bukkitRunnable, moduleInstance, taskId);
    }

    public enum ModuleTaskType {

        SINGLE,
        DELAYED,
        TIMER,
        SINGLE_ASYNC,
        DELAYED_ASYNC,
        TIMER_ASYNC;

    }

}
