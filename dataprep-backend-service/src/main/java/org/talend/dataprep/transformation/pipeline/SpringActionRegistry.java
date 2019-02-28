package org.talend.dataprep.transformation.pipeline;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.action.ActionDefinition;

@Component
public class SpringActionRegistry implements ActionRegistry { // NOSONAR

    private List<String> actionsThatHaveUpgrade;

    private List<ActionDefinition> actions;

    public SpringActionRegistry(@Autowired(required = false) List<ActionDefinition> actions) {
        this.actions = actions;
        if (actions != null) {
            actionsThatHaveUpgrade =
                    actions.stream().filter(ActionDefinition::haveUpgrade).map(ActionDefinition::getName).collect(
                            Collectors.toList());
        } else {
            actionsThatHaveUpgrade = Collections.EMPTY_LIST;
        }
    }

    @Override
    public ActionDefinition get(String name) {
        if (actions != null) {
            for (ActionDefinition action : actions) {
                if (action.getName().equals(name)) {
                    return action;
                }
            }
        }
        return null;
    }

    @Override
    public Stream<Class<? extends ActionDefinition>> getAllClasses() {
        return actions.stream().map(ActionDefinition::getClass);
    }

    @Override
    public Stream<ActionDefinition> getAllInstances() {
        return actions.stream();
    }

    @Override
    public boolean haveUpgrade(String name) {
        return actionsThatHaveUpgrade.contains(name);
    }
}