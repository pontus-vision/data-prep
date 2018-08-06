package org.talend.dataprep.transformation.pipeline;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.action.ActionDefinition;
import org.talend.dataprep.transformation.ActionAdapter;
import org.talend.dataprep.transformation.WantedActionInterface;

import static java.util.stream.Collectors.toList;

@Component
public class SpringActionRegistry implements ActionRegistry { // NOSONAR

    private List<ActionDefinition> actions;

    @Autowired(required = false)
    public SpringActionRegistry(List<ActionDefinition> actions, List<WantedActionInterface> actionsV2) {
        this.actions = actions;
        actions.addAll(actionsV2.stream().map(ActionAdapter::new).collect(toList()));
    }

    @Override
    public ActionDefinition get(String name) {
        for (ActionDefinition action : actions) {
            if (action.getName().equals(name)) {
                return action;
            }
        }
        return null;
    }

    @Override
    public Stream<ActionDefinition> findAll() {
        return actions.stream();
    }
}
