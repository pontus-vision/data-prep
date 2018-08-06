package org.talend.dataprep;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.action.ActionDefinition;
import org.talend.dataprep.transformation.ActionAdapter;
import org.talend.dataprep.transformation.WantedActionInterface;
import org.talend.dataprep.transformation.pipeline.ActionRegistry;

public class ClassPathActionRegistry implements ActionRegistry {

    private static final Object lock = new Object();

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathActionRegistry.class);

    private final Map<String, ActionDefinition> nameToAction = new HashMap<>();

    public ClassPathActionRegistry(String... actionPackages) {
        synchronized (lock) { // Reflections is not thread safe (see https://github.com/ronmamo/reflections/issues/81).
            for (String actionPackage : actionPackages) {
                LOGGER.debug("Scanning classpath @ '{}'", actionPackage);
                Reflections reflections = new Reflections(actionPackage);
                final Set<Class<? extends ActionDefinition>> allActions = reflections.getSubTypesOf(ActionDefinition.class);
                LOGGER.debug("Found {} possible action class(es) in '{}'", allActions.size(), actionPackage);
                instantiateAllClasses(allActions.stream())
                        .forEach(action -> nameToAction.put(action.getName(), action));

                final Set<Class<? extends WantedActionInterface>> allActionsV2 = reflections.getSubTypesOf(WantedActionInterface.class);
                LOGGER.debug("Found {} possible action class(es) in '{}'", allActionsV2.size(), actionPackage);
                instantiateAllClasses(allActionsV2.stream())
                        .map(ActionAdapter::new)
                        .forEach(action -> nameToAction.put(action.getName(), action));
            }
        }
        LOGGER.info("{} actions registered for usage.", nameToAction.size());
    }

    private static <T> Stream<T> instantiateAllClasses(Stream<Class<? extends T>> classes) {
        return classes
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .filter(c -> !Modifier.isInterface(c.getModifiers()))
                .flatMap(c -> Stream.of((Constructor<T>[]) c.getConstructors())) // Method documentation says it is safe
                .filter(constructor -> constructor.getParameters().length == 0)
                .map(c -> {
                    try {
                        return c.newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        LOGGER.error("Unable to register action '{}'", c.getDeclaringClass().getName(), e);
                        return null;
                    }
                }).filter(Objects::nonNull);
    }

    @Override
    public ActionDefinition get(String name) {
        final ActionDefinition aClass = nameToAction.get(name);
        if (aClass == null) {
            LOGGER.error("Action definition '{}' does not exist.", name);
            return null;
        } else {
            return aClass;
        }
    }

    @Override
    public Stream<ActionDefinition> findAll() {
        return nameToAction.values().stream();
    }
}
