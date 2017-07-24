package org.talend.dataprep.transformation.pipeline.util;

import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;
import org.talend.dataprep.transformation.pipeline.node.*;

/**
 * <p>
 * A {@link Visitor} implementation to copy nodes (and reachable nodes from visited node) using
 * {@link Node#copyShallow()}.
 * </p>
 * <p>
 * This is used to copy both the {@link CompileNode} and the {@link ActionNode}
 * </p>
 */
class NodeCopy extends Visitor<Void> {

    /** The builder used to copy. */
    private final NodeBuilder builder = NodeBuilder.source();

    /** Flag set to true when the copy is finished. */
    private boolean hasEnded = false;

    /** The last node to copy. */
    private Node lastNode;

    @Override
    public Void visitAction(ActionNode actionNode) {
        if (!hasEnded) {
            // stop the copy with the first ActionNode met
            lastNode = actionNode.copyShallow();
            builder.to(lastNode);
            hasEnded = true;
        }
        // No call to super -> interrupt copy.
        return null;
    }

    @Override
    public Void visitCompile(CompileNode compileNode) {
        visitNode(compileNode);
        return super.visitCompile(compileNode);
    }

    @Override
    public Void visitSource(SourceNode sourceNode) {
        visitNode(sourceNode);
        return super.visitSource(sourceNode);
    }

    @Override
    public Void visitLocalSource(LocalSourceNode localSourceNode) {
        visitNode(localSourceNode);
        return super.visitLocalSource(localSourceNode);
    }

    @Override
    public Void visitStep(StepNode stepNode) {
        visitNode(stepNode);
        return super.visitStep(stepNode);
    }

    @Override
    public Void visitPipeline(Pipeline pipeline) {
        visitNode(pipeline);
        return super.visitPipeline(pipeline);
    }

    @Override
    public Void visitCleanUp(CleanUpNode cleanUpNode) {
        visitNode(cleanUpNode);
        return super.visitCleanUp(cleanUpNode);
    }

    @Override
    public Void visitFilterNode(FilterNode filterNode) {
        visitNode(filterNode);
        return super.visitFilterNode(filterNode);
    }

    @Override
    public Void visitStatistics(StatisticsNode statisticsNode) {
        visitNode(statisticsNode);
        return super.visitStatistics(statisticsNode);
    }

    @Override
    public Void visitInvalidDetection(InvalidDetectionNode invalidDetectionNode) {
        visitNode(invalidDetectionNode);
        return super.visitInvalidDetection(invalidDetectionNode);
    }

    @Override
    public Void visitTypeDetection(TypeDetectionNode typeDetectionNode) {
        visitNode(typeDetectionNode);
        return super.visitTypeDetection(typeDetectionNode);
    }

    @Override
    public Void visitNToOne(NToOneNode nToOneNode) {
        visitNode(nToOneNode);
        return super.visitNToOne(nToOneNode);
    }

    @Override
    public Void visitCollector(CollectorNode collectorNode) {
        visitNode(collectorNode);
        return super.visitCollector(collectorNode);
    }

    @Override
    public Void visitConsumerNode(ConsumerNode consumerNode) {
        visitNode(consumerNode);
        return super.visitConsumerNode(consumerNode);
    }

    @Override
    public Void visitNode(Node node) {
        builder.to(node.copyShallow());
        return super.visitNode(node);
    }

    /**
     * @return The copy of the visited node(s).
     */
    public Node getCopy() {
        return builder.build();
    }

    /**
     * @return The last node of the pipeline copy.
     */
    public Node getLastNode() {
        return lastNode;
    }
}
