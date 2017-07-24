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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Visitor;
import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.node.*;

public class NodeClassVisitor extends Visitor<Void> {

    private final List<Class> traversedClasses = new ArrayList<>();

    @Override
    public Void visitAction(ActionNode actionNode) {
        traversedClasses.add(actionNode.getClass());
        return super.visitAction(actionNode);
    }

    @Override
    public Void visitCompile(CompileNode compileNode) {
        traversedClasses.add(compileNode.getClass());
        return super.visitCompile(compileNode);
    }

    @Override
    public Void visitSource(SourceNode sourceNode) {
        traversedClasses.add(sourceNode.getClass());
        return super.visitSource(sourceNode);
    }

    @Override
    public Void visitLocalSource(LocalSourceNode localSourceNode) {
        traversedClasses.add(localSourceNode.getClass());
        return super.visitLocalSource(localSourceNode);
    }

    @Override
    public Void visitBasicLink(BasicLink basicLink) {
        traversedClasses.add(basicLink.getClass());
        return super.visitBasicLink(basicLink);
    }

    @Override
    public Void visitPipeline(Pipeline pipeline) {
        traversedClasses.add(pipeline.getClass());
        return super.visitPipeline(pipeline);
    }

    @Override
    public Void visitNode(Node node) {
        traversedClasses.add(node.getClass());
        return super.visitNode(node);
    }

    @Override
    public Void visitCloneLink(CloneLink cloneLink) {
        traversedClasses.add(cloneLink.getClass());
        return super.visitCloneLink(cloneLink);
    }

    @Override
    public Void visitStep(StepNode stepNode) {
        traversedClasses.add(stepNode.getClass());
        return super.visitStep(stepNode);
    }

    public List<Class> getTraversedClasses() {
        return Collections.unmodifiableList(traversedClasses);
    }
}
