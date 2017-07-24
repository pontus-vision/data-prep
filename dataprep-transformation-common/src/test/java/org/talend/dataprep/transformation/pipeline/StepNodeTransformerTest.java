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

package org.talend.dataprep.transformation.pipeline;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.talend.dataprep.api.preparation.Step;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;
import org.talend.dataprep.transformation.pipeline.node.*;
import org.talend.dataprep.transformation.pipeline.util.NodeClassVisitor;
import org.talend.dataprep.transformation.pipeline.util.StepNodeTransformer;

public class StepNodeTransformerTest {

    private static final Step STEP = new Step(null, null, "");

    private static final Step INVALID_STEP = new Step(null, null, "");

    private static final Step ROOT = new Step(null, null, "");

    @Before
    public void setUp() throws Exception {
        STEP.setRowMetadata("rowMetadata");
        INVALID_STEP.setRowMetadata(null);
    }

    @Test
    public void shouldNotCreateStepNode() {
        // given
        Node node = NodeBuilder.from(new LocalSourceNode(Stream.empty())).build();

        // when
        final Node processed = StepNodeTransformer.transform(node, emptyList(), s -> null);

        // then
        assertEquals(LocalSourceNode.class, processed.getClass());
        assertNull(processed.getLink());
    }

    @Test
    public void shouldCreateStepNode() {
        // given
        Node node = NodeBuilder //
                .from(new LocalSourceNode(Stream.empty())) //
                .to(new CompileNode(null, Collections.emptyMap())) //
                .to(new ActionNode(null, Collections.emptyMap())) //
                .build();

        // when
        final Node processed = StepNodeTransformer.transform(node, asList(ROOT, STEP), s -> null);

        // then
        final Class[] expectedClasses = { LocalSourceNode.class, StepNode.class };
        final NodeClassVisitor visitor = new NodeClassVisitor();
        processed.accept(visitor);
        assertThat(visitor.getTraversedClasses(), hasItems(expectedClasses));

    }

    @Test
    public void shouldCreateStepNodeWhenSurrounded() {
        // given
        Node node = NodeBuilder //
                .from(new LocalSourceNode(Stream.empty())) //
                .to(new CompileNode(null, Collections.emptyMap())) //
                .to(new ActionNode(null, Collections.emptyMap())) //
                .to(new BasicNode()) //
                .build();

        // when
        final Node processed = StepNodeTransformer.transform(node, asList(ROOT, STEP), s -> null);

        // then
        final Class[] expectedClasses = { LocalSourceNode.class, StepNode.class, BasicNode.class };
        final NodeClassVisitor visitor = new NodeClassVisitor();
        processed.accept(visitor);
        assertThat(visitor.getTraversedClasses(), hasItems(expectedClasses));
    }

    @Test
    public void shouldCreateStepNodesWhenSurrounded() {
        // given
        Node node = NodeBuilder //
                .from(new LocalSourceNode(Stream.empty())) //
                .to(new CompileNode(null, Collections.emptyMap())) //
                .to(new ActionNode(null, Collections.emptyMap())) //
                .to(new BasicNode()) //
                .to(new CompileNode(null, Collections.emptyMap())) //
                .to(new ActionNode(null, Collections.emptyMap())) //
                .build();

        // when
        final Node processed = StepNodeTransformer.transform(node, asList(ROOT, STEP, STEP), s -> null);

        // then
        final AtomicInteger stepNodeCount = new AtomicInteger();
        processed.accept(new Visitor<Void>() {

            @Override
            public Void visitStep(StepNode stepNode) {
                stepNodeCount.incrementAndGet();
                super.visitStep(stepNode);
                return null;
            }
        });
        assertEquals(2, stepNodeCount.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailCreateStepNode() {
        // given
        Node node = NodeBuilder //
                .from(new CompileNode(null, Collections.emptyMap())) //
                .to(new ActionNode(null, Collections.emptyMap())) //
                .build();

        // then
        StepNodeTransformer.transform(node, emptyList(), s -> null);
    }

    @Test
    public void shouldCreateStepNodeWithTooManySteps() {
        // given
        Node node = NodeBuilder //
                .from(new LocalSourceNode(Stream.empty())) //
                .to(new CompileNode(null, Collections.emptyMap())) //
                .to(new ActionNode(null, Collections.emptyMap())) //
                .build();

        // when
        final Node processed = StepNodeTransformer.transform(node, asList(ROOT, STEP, STEP), s -> null);

        // then
        final Class[] expectedClasses = { LocalSourceNode.class, StepNode.class };
        final NodeClassVisitor visitor = new NodeClassVisitor();
        processed.accept(visitor);
        assertThat(visitor.getTraversedClasses(), hasItems(expectedClasses));
    }

}
