package net.bitbylogic.apibylogic.dependency;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import lombok.Setter;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.dependency.annotation.Dependency;
import net.bitbylogic.apibylogic.util.Table;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

@Getter
public class DependencyManager {

    private final Table<Class<?>, String, Object> dependencies;
    private final HashMap<Class<?>, List<Object>> missingDependencies;

    @Setter
    private PaperCommandManager commandManager;

    public DependencyManager() {
        this(null);
    }

    public DependencyManager(@Nullable PaperCommandManager commandManager) {
        this.commandManager = commandManager;
        this.dependencies = new Table<>();
        this.missingDependencies = new HashMap<>();

        registerDependency(APIByLogic.class, APIByLogic.getInstance());
        registerDependency(this.getClass(), this);
    }

    public <T> void registerDependency(Class<? extends T> clazz, T instance) {
        if (dependencies.containsKey(clazz, clazz.getName())) {
            throw new IllegalStateException("There is already an instance of " + clazz.getName() + " registered!");
        }

        dependencies.put(clazz, clazz.getName(), instance);

        if (missingDependencies.containsKey(clazz)) {
            APIByLogic.getInstance().getLogger().log(Level.INFO, "Injecting missing dependencies for %s", clazz.getName());
            missingDependencies.get(clazz).forEach(this::injectDependencies);
            missingDependencies.remove(clazz);
        }

        if (commandManager == null) {
            return;
        }

        commandManager.registerDependency(clazz, instance);
    }

    public void injectDependencies(Object obj) {
        Class<?> clazz = obj.getClass();

        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Dependency.class)) {
                    continue;
                }

                String fieldKey = field.getType().getName();
                Object object = dependencies.row(field.getType()).get(fieldKey);

                if (object == null) {
                    APIByLogic.getInstance().getLogger().log(Level.SEVERE,
                            String.format("Couldn't find dependency for field %s in class %s!",
                                    field.getName(), obj.getClass().getName()));

                    List<Object> pendingObjects = missingDependencies.getOrDefault(field.getType(), new ArrayList<>());
                    pendingObjects.add(obj);
                    missingDependencies.put(field.getType(), pendingObjects);

                    continue;
                }

                boolean accessible = field.isAccessible();

                if (!accessible) {
                    field.setAccessible(true);
                }

                try {
                    field.set(obj, object);
                    field.setAccessible(accessible);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);
    }

}
