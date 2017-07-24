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

package org.talend.dataprep.transformation.api.transformer.json;

import static org.mockito.Mockito.verify;
import static org.talend.dataprep.api.preparation.Step.ROOT_STEP;
import static org.talend.dataprep.transformation.pipeline.Runtimes.AvailableRuntimes.JAVA;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.preparation.PreparationActions;
import org.talend.dataprep.api.preparation.Step;
import org.talend.dataprep.transformation.actions.common.RunnableAction;
import org.talend.dataprep.transformation.actions.context.ActionContext;
import org.talend.dataprep.transformation.actions.context.TransformationContext;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;
import org.talend.dataprep.transformation.pipeline.node.ActionNode;
import org.talend.dataprep.transformation.pipeline.node.BasicNode;
import org.talend.dataprep.transformation.pipeline.node.StepNode;
import org.talend.dataprep.transformation.service.StepMetadataRepository;

@RunWith(MockitoJUnitRunner.class)
public class UpdatedStepVisitorTest {

    private ActionNode entryNode;

    private ActionContext actionContext;

    @Rule
    public Runtimes context = new Runtimes();

    @Mock
    private StepMetadataRepository stepMetadataRepository;

    @Before
    public void setUp() throws Exception {
        actionContext = new ActionContext(new TransformationContext());
        entryNode = new ActionNode(new RunnableAction((row, context) -> Collections.singletonList(row)), Collections.emptyMap());
        context.getExecutor().setStepMetadataRepository(stepMetadataRepository);
    }

    @Test
    public void testUpdatedStepsWithOk() throws Exception {
        // Given
        final Step step = new Step(ROOT_STEP.id(), new PreparationActions().id(), "0.0");
        final RowMetadata stepRowMetadata = new RowMetadata();
        final Node stepNode = NodeBuilder.source(Stream.of(new DataSetRow(stepRowMetadata))) //
                .to(new StepNode(step, stepRowMetadata, entryNode, new BasicNode())) //
                .to(new BasicNode()) //
                .build();
        actionContext.setActionStatus(ActionContext.ActionStatus.OK); // OK action!

        // When
        Runtimes.execute(stepNode, JAVA);

        // Then
        if (Runtimes.matches(JAVA)) {
            verify(stepMetadataRepository).update(step.id(), stepRowMetadata);
        }
    }

    @Test
    public void testUpdatedStepsWithKO() throws Exception {
        // Given
        final Step step = new Step(ROOT_STEP.id(), new PreparationActions().id(), "0.0");
        final RowMetadata stepRowMetadata = new RowMetadata();
        final Node stepNode = NodeBuilder.source(Stream.of(new DataSetRow(stepRowMetadata))) //
                .to(new StepNode(step, stepRowMetadata, entryNode, new BasicNode())) //
                .to(new BasicNode()) //
                .build();
        actionContext.setActionStatus(ActionContext.ActionStatus.CANCELED); // Canceled action!

        // When
        Runtimes.execute(stepNode, JAVA);

        // Then
        if (Runtimes.matches(JAVA)) {
            verify(stepMetadataRepository).update(step.id(), stepRowMetadata);
        }
    }
}
