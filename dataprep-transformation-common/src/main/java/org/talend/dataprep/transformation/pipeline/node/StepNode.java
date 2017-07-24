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

import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.preparation.Step;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;
import org.talend.dataprep.transformation.pipeline.util.StepNodeTransformer;

/**
 * <p>
 * This node is dedicated to execution when a preparation is available. This node is used to group together nodes that
 * correspond to a step.
 * </p>
 * <p>
 * This allows code to reuse row metadata contained in step instead of provided one.
 * </p>
 *
 * @see StepNodeTransformer
 */
public class StepNode extends BasicNode {

    private final Step step;

    private final Node entryNode;

    private final Node lastNode;

    private RowMetadata stepRowMetadata;

    public StepNode(Step step, RowMetadata stepRowMetadata, Node entryNode, Node lastNode) {
        this.step = step;
        this.stepRowMetadata = stepRowMetadata;
        this.entryNode = entryNode;
        this.lastNode = lastNode;
    }


    public RowMetadata getStepRowMetadata() {
        return stepRowMetadata;
    }

    public Node getEntryNode() {
        return entryNode;
    }

    public Step getStep() {
        return step;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitStep(this);
    }

    @Override
    public Node copyShallow() {
        return new StepNode(step, stepRowMetadata, entryNode, lastNode);
    }

}
