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

package org.talend.dataprep.transformation.pipeline.util;

import org.talend.dataprep.transformation.pipeline.Monitored;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;
import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.node.*;

public class PipelineConsoleDump extends Visitor<Void> {

    private final StringBuilder builder;

    public PipelineConsoleDump(StringBuilder builder) {
        this.builder = builder;
    }

    private void buildMonitorInformation(Monitored monitored) {
        final long totalTime = monitored.getTotalTime();
        final long count = monitored.getCount();
        double speed = totalTime > 0 ? Math.round(((double) count * 1000) / totalTime) : Double.POSITIVE_INFINITY;

        builder
                .append("(")
                .append(monitored.getTotalTime())
                .append(" ms - ")
                .append(monitored.getCount())
                .append(" rows - ")
                .append(speed)
                .append(" rows/s) ");
    }

    @Override
    public Void visitAction(ActionNode actionNode) {
        builder.append("ACTION").append(" [").append(actionNode.getAction().getName()).append("] ").append(")").append(
                '\n');
        return super.visitAction(actionNode);
    }

    @Override
    public Void visitCompile(CompileNode compileNode) {
        builder
                .append("COMPILE")
                .append(" [")
                .append(compileNode.getAction().getName())
                .append("] ")
                .append(")")
                .append('\n');
        return super.visitCompile(compileNode);
    }

    @Override
    public Void visitSource(SourceNode sourceNode) {
        builder.append("-> SOURCE").append('\n');
        return super.visitSource(sourceNode);
    }

    @Override
    public Void visitBasicLink(BasicLink basicLink) {
        builder.append("-> ");
        return super.visitBasicLink(basicLink);
    }

    @Override
    public Void visitPipeline(Pipeline pipeline) {
        builder.append("PIPELINE {").append('\n');
        super.visitPipeline(pipeline);
        builder.append('\n').append('}').append('\n');
        return null;
    }

    @Override
    public Void visitNode(Node node) {
        if (node instanceof Monitored) {
            buildMonitorInformation((Monitored) node);
        }
        builder.append("UNKNOWN NODE (").append(node.getClass().getName()).append(")").append('\n');
        return super.visitNode(node);
    }

    @Override
    public Void visitCloneLink(CloneLink cloneLink) {
        builder.append("->").append('\n');
        return super.visitCloneLink(cloneLink);
    }

    @Override
    public Void visitStep(StepNode stepNode) {
        builder.append("STEP NODE (").append(stepNode.getStep().toString()).append(")\n");
        return super.visitStep(stepNode);
    }
}
