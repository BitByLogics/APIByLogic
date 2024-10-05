package net.bitbylogic.apibylogic.dependency;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.apibylogic.APIByLogic;
import net.bitbylogic.apibylogic.dependency.annotation.Dependency;
import net.bitbylogic.apibylogic.util.Table;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
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
            throw new IllegalStateException("There is already an instance of " + clazz.getSimpleName() + " registered!");
        }

        dependencies.put(clazz, clazz.getName(), instance);

        if (missingDependencies.containsKey(clazz)) {
            APIByLogic.getInstance().getLogger().log(Level.INFO, "Injecting missing dependencies for '" + clazz.getSimpleName() + "'.");
            missingDependencies.get(clazz).forEach(obj -> injectDependencies(obj, false));
            missingDependencies.remove(clazz);
        }

        if (commandManager == null) {
            return;
        }

        commandManager.registerDependency(clazz, instance);
    }

    public void injectDependencies(@NonNull Object object, boolean deepInjection) {
        Class<?> clazz = object.getClass();

        do {
            injectFieldDependencies(object);

            if (missingDependencies.values().stream().noneMatch(objects -> objects.contains(object))) {
                getDependencyMethods(object).forEach(method -> {
                    try {
                        method.invoke(object);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        APIByLogic.getInstance().getLogger().log(Level.SEVERE, "Unable to invoke dependency method for class: " + object.getClass().getSimpleName());
                        e.printStackTrace();
                    }
                });
            }

            clazz = clazz.getSuperclass();
        } while (deepInjection && clazz != null);
    }

    private void injectFieldDependencies(@NonNull Object object) {
        getDependencyFields(object).forEach(field -> {
            resolveDependency(field).ifPresentOrElse(
                    dependency -> injectIntoField(object, field, dependency),
                    () -> handleMissingDependency(field, object)
            );
        });
    }

    private Optional<Object> resolveDependency(@NonNull Field field) {
        String fieldKey = field.getType().getName();
        return Optional.ofNullable(dependencies.row(field.getType()).get(fieldKey));
    }

    private void handleMissingDependency(@NonNull Field field, @NonNull Object object) {
        List<Object> pendingObjects = missingDependencies.getOrDefault(field.getType(), new ArrayList<>());

        if (pendingObjects.contains(object)) {
            return;
        }

        APIByLogic.getInstance().getLogger().log(Level.WARNING,
                String.format("Couldn't find dependency for field '%s' in class '%s'. It may load later.",
                        field.getName(), object.getClass().getSimpleName()));

        pendingObjects.add(object);
        missingDependencies.put(field.getType(), pendingObjects);
    }

    private void injectIntoField(@NonNull Object object, @NonNull Field field, @NonNull Object dependency) {
        boolean wasAccessible = field.canAccess(object);

        if (!wasAccessible && !field.trySetAccessible()) {
            return;
        }

        try {
            field.set(object, dependency);
        } catch (IllegalAccessException e) {
            APIByLogic.getInstance().getLogger().log(Level.SEVERE, "Unable to set field dependency for: " + field.getName());
            e.printStackTrace();
        } finally {
            if (!wasAccessible) {
                field.setAccessible(false);
            }
        }
    }

    private Set<Field> getDependencyFields(@NonNull Object object) {
        Class<?> clazz = object.getClass();
        Set<Field> dependencyFields = new HashSet<>();

        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Dependency.class)) {
                continue;
            }

            dependencyFields.add(field);
        }

        return Set.copyOf(dependencyFields);
    }

    private Set<Method> getDependencyMethods(@NonNull Object object) {
        Class<?> clazz = object.getClass();
        Set<Method> dependencyMethods = new HashSet<>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Dependency.class) || method.getParameterCount() > 0) {
                continue;
            }

            dependencyMethods.add(method);
        }

        return Set.copyOf(dependencyMethods);
    }

}
