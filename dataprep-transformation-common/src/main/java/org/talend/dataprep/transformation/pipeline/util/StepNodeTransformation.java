// ============================================================================
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.pipeline.util;

import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.preparation.Step;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;
import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.node.*;

/**
 * An {@link Visitor} for node that groups all step related nodes into a {@link StepNode}.
 */
class StepNodeTransformation extends Visitor<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StepNodeTransformation.class);

    private final Iterator<Step> steps;

    private final Function<Step, RowMetadata> rowMetadataSupplier;

    private State DISPATCH = new Dispatch();

    private State DEFAULT = new DefaultState();

    private State state = DISPATCH;

    private NodeBuilder builder;

    /**
     * Build a new visitor to transform nodes into new node pipeline which will eventually use {@link StepNode} if
     * applicable. For each new {@link StepNode}, one of the <code>steps</code> is consumed.
     *
     * @param steps The {@link Step steps} to be used when creating new {@link StepNode}.
     * @param rowMetadataSupplier An function that allows this code to fetch {@link RowMetadata} to associate with step.
     */
    StepNodeTransformation(List<Step> steps, Function<Step, RowMetadata> rowMetadataSupplier) {
        if (!steps.isEmpty() && !Step.ROOT_STEP.getId().equals(steps.get(0).getId())) {
            // Code expects root step to be located at the beginning of iterator.
            Collections.reverse(steps);
        }
        this.steps = steps.iterator();
        this.rowMetadataSupplier = rowMetadataSupplier;
    }

    Node getTransformedNode() {
        if (steps.hasNext()) {
            AtomicInteger remainingCount = new AtomicInteger(0);
            steps.forEachRemaining(s -> {
                if (!Step.ROOT_STEP.getId().equals(s.getId())) {
                    LOGGER.warn("Remaining step #{}: {}", remainingCount.get(), s);
                    remainingCount.incrementAndGet();
                }
            });
            if (remainingCount.get() > 0) {
                LOGGER.warn("Too many steps remaining ({} remaining).", remainingCount.get());
            }
        }
        return builder.build();
    }

    private void processNode(Node node) {
        state = state.process(node);
    }

    @Override
    public Void visitSource(SourceNode sourceNode) {
        builder = NodeBuilder.source(sourceNode.getSourceUrl());
        return super.visitSource(sourceNode);
    }

    @Override
    public Void visitLocalSource(LocalSourceNode localSourceNode) {
        builder = NodeBuilder.source(localSourceNode.getSource());
        return super.visitLocalSource(localSourceNode);
    }

    @Override
    public Void visitAction(ActionNode actionNode) {
        processNode(actionNode);
        return super.visitAction(actionNode);
    }

    @Override
    public Void visitCompile(CompileNode compileNode) {
        processNode(compileNode);
        return super.visitCompile(compileNode);
    }

    @Override
    public Void visitNode(Node node) {
        processNode(node);
        return super.visitNode(node);
    }

    /**
     * Internal state for the visitor.
     */
    interface State {

        /**
         * Process the given node and return the next state.
         *
         * @param node the node to process.
         * @return the next state that'll handle the node.
         */
        State process(Node node);
    }

    /**
     * Choose between 'default' mode (no action to take) and 'step' mode (create StepNode).
     */
    private class Dispatch implements State {

        private Node previous = null;

        @Override
        public State process(Node node) {
            final State newState;
            if (node instanceof CompileNode) {
                // Sanity check: there should be enough Step for all Compile/Action couple in pipeline.
                if (!steps.hasNext()) {
                    throw new IllegalArgumentException("Not enough steps to transform pipeline.");
                }

                // insert a StepNode within the pipeline builder
                Step nextStep = steps.next();
                if (Step.ROOT_STEP.getId().equals(nextStep.getId())) {
                    LOGGER.debug("Unable to use step '{}' (root step).", nextStep.getId());
                    if (steps.hasNext()) {
                        nextStep = steps.next();
                    } else {
                        LOGGER.error("Unable to use root step as first step and no remaining steps.");
                        nextStep = null;
                    }
                }

                if (nextStep != null) {
                    ofNullable(previous).ifPresent(n -> n.setLink(null));
                    newState = new StepState(previous, nextStep);
                } else {
                    newState = DEFAULT;
                }

            } else {
                newState = DEFAULT;
            }
            previous = node;
            return newState.process(node);
        }
    }

    /**
     * State when creating a StepNode
     */
    private class StepState implements State {

        private final Node previous;

        private final Step step;

        private StepState(Node previous, Step step) {
            this.previous = previous;
            this.step = step;
        }

        @Override
        public State process(Node node) {
            if (node instanceof CompileNode) {
                // Continue (create StepNode)
                final NodeCopy copy = new NodeCopy();
                node.accept(copy);

                final StepNode stepNode =
                        new StepNode(step, rowMetadataSupplier.apply(step), copy.getCopy(), copy.getLastNode());
                // and plug the previous link to the new StepNode
                ofNullable(previous).ifPresent(n -> n.setLink(new BasicLink(stepNode)));
                builder.to(stepNode);

                return this;
            } else if (node instanceof ActionNode) {
                return DISPATCH;
            } else {
                return this;
            }
        }

    }

    /**
     * No specific action to take, continue building node as they previously were.
     */
    private class DefaultState implements State {

        @Override
        public State process(Node node) {
            builder.to(node.copyShallow());
            return DISPATCH;
        }
    }

}
