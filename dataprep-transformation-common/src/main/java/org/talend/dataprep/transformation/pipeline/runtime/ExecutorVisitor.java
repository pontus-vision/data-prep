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

package org.talend.dataprep.transformation.pipeline.runtime;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.io.output.NullOutputStream;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.actions.DataSetRowAction;
import org.talend.dataprep.transformation.actions.common.RunnableAction;
import org.talend.dataprep.transformation.actions.context.ActionContext;
import org.talend.dataprep.transformation.actions.context.TransformationContext;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;
import org.talend.dataprep.transformation.pipeline.node.ActionNode;
import org.talend.dataprep.transformation.pipeline.node.CompileNode;
import org.talend.dataprep.transformation.pipeline.node.FilterNode;
import org.talend.dataprep.transformation.pipeline.node.StepNode;
import org.talend.dataprep.transformation.service.StepMetadataRepository;

public abstract class ExecutorVisitor<R> extends Visitor<Node> {

    protected TransformationContext context = new TransformationContext();

    protected StepMetadataRepository stepMetadataRepository;

    protected OutputStream outputStream = new NullOutputStream();

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setStepMetadataRepository(StepMetadataRepository stepMetadataRepository) {
        this.stepMetadataRepository = stepMetadataRepository;
    }

    @Override
    protected Node doNodeVisit(Node node) {
        super.doNodeVisit(node);
        return node;
    }

    @Nonnull
    Predicate<DataSetRow> handleFilterNode(FilterNode filterNode) {
        return row -> Stream.of(filterNode.getFilters()).allMatch(f -> f.test(row));
    }

    @Nonnull
    Function<DataSetRow, DataSetRow> toConsumer(CompileNode compileNode) {
        return row -> {
            final RunnableAction action = compileNode.getAction();
            final DataSetRowAction rowAction = action.getRowAction();
            final RowMetadata rowMetadata = row.getRowMetadata();
            final ActionContext actionContext = context.create(action, rowMetadata, compileNode.getParameters());
            rowAction.compile(actionContext);

            return row.setRowMetadata(actionContext.getRowMetadata());
        };
    }

    @Nonnull
    Function<DataSetRow, Collection<DataSetRow>> toFunction(ActionNode actionNode) {
        return row -> {
            final RunnableAction action = actionNode.getAction();
            final ActionContext actionContext = context.create(action, row.getRowMetadata());
            switch (actionContext.getActionStatus()) {
            case NOT_EXECUTED:
            case OK:
                final DataSetRowAction rowAction = action.getRowAction();
                return rowAction.apply(row, actionContext);
            case DONE:
            case CANCELED:
            default:
                return Collections.singletonList(row);
            }
        };
    }

    @Nonnull
    Function<DataSetRow, DataSetRow> handleStepNode(StepNode stepNode) {
        return row -> {
            final RowMetadata stepRowMetadata = stepNode.getStepRowMetadata();
            if (stepRowMetadata != null) {
                row.setRowMetadata(stepRowMetadata);
            }
            return row;
        };
    }

    @Override
    public Node visitStep(StepNode stepNode) {
        super.visitStep(stepNode);
        return stepNode.getEntryNode().accept(this).copyShallow();
    }

    public abstract R getResult();

    public abstract ExecutorRunnable toRunnable();

    public TransformationContext getContext() {
        return context;
    }
}
