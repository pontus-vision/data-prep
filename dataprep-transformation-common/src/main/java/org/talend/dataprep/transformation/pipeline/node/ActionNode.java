// ============================================================================
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.pipeline.node;

import java.util.Collections;
import java.util.Map;

import org.talend.dataprep.transformation.actions.common.RunnableAction;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;

/**
 * A node that executes an action on a row
 */
public class ActionNode extends BasicNode {

    private final RunnableAction action;

    private final Map<String, String> parameters;

    public ActionNode(RunnableAction action) {
        this(action, Collections.emptyMap());
    }

    public ActionNode(RunnableAction action, Map<String, String> parameters) {
        this.action = action;
        this.parameters = parameters;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitAction(this);
    }

    @Override
    public Node copyShallow() {
        return new ActionNode(action, parameters);
    }

    public RunnableAction getAction() {
        return action;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
}
