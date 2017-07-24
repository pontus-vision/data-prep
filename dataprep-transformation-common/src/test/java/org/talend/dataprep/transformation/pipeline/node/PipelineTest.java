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

import static java.util.Collections.emptyMap;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.actions.DataSetRowAction;
import org.talend.dataprep.transformation.actions.common.RunnableAction;
import org.talend.dataprep.transformation.actions.context.ActionContext;
import org.talend.dataprep.transformation.actions.context.TransformationContext;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;
import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.util.NodeClassVisitor;

public class PipelineTest implements Serializable {

    @Rule
    public Runtimes runtimes = new Runtimes();

    @Test
    public void testActionNode() throws Exception {
        final RunnableAction mockAction = new RunnableAction();
        ActionNode compileNode = new ActionNode(mockAction, Collections.emptyMap());

        assertEquals(mockAction, compileNode.getAction());
    }

    @Test
    public void testCompileNode() throws Exception {
        final RunnableAction mockAction = new RunnableAction();
        CompileNode compileNode = new CompileNode(mockAction, Collections.emptyMap());

        assertEquals(mockAction, compileNode.getAction());
    }

    @Test
    public void testRecompileAction() throws Exception {
        // Given
        final RunnableAction mockAction = new RunnableAction() {

            @Override
            public DataSetRowAction getRowAction() {
                return new DataSetRowAction() {

                    @Override
                    public void compile(ActionContext actionContext) {
                        actionContext.getRowMetadata().getById("0000").setType(Type.DATE.getName());
                        actionContext.setActionStatus(ActionContext.ActionStatus.OK);
                    }

                    @Override
                    public Collection<DataSetRow> apply(DataSetRow dataSetRow, ActionContext context) {
                        return Collections.singletonList(dataSetRow);
                    }
                };
            }
        };

        final RowMetadata rowMetadata1 = new RowMetadata();
        rowMetadata1.addColumn(new ColumnMetadata());
        final DataSetRow row1 = new DataSetRow(rowMetadata1).setTdpId(0L);

        final RowMetadata rowMetadata2 = rowMetadata1.clone();
        final DataSetRow row2 = new DataSetRow(rowMetadata2).setTdpId(1L);
        final Node node = NodeBuilder.source(Stream.of(row1, row2)) //
                .to(new CompileNode(mockAction, Collections.emptyMap())) //
                .to(new CollectorNode()) //
                .build();

        // when
        final List<DataSetRow> collected = Runtimes.execute(node);

        // then
        for (DataSetRow dataSetRow : collected) {
            assertEquals(Type.DATE.getName(), dataSetRow.getRowMetadata().getById("0000").getType());
        }
    }

    @Test
    public void testCanceledAction() throws Exception {
        // Given
        final RunnableAction mockAction = new RunnableAction() {

            @Override
            public DataSetRowAction getRowAction() {
                return (r, context) -> {
                    context.get("Executed", p -> true);
                    return Collections.singletonList(r);
                };
            }
        };
        final DataSetRow row = new DataSetRow(emptyMap());
        final ActionContext actionContext = new ActionContext(new TransformationContext());
        actionContext.setActionStatus(ActionContext.ActionStatus.CANCELED);
        final Node node = NodeBuilder.source(Stream.of(row)).to(new ActionNode(mockAction, Collections.emptyMap())).to(new CollectorNode()).build();

        // when
        Runtimes.execute(node);

        // then
        assertFalse(actionContext.has("Executed"));
    }

    @Test
    public void testCloneLink() throws Exception {
        // Given
        final DataSetRow row1 = new DataSetRow(emptyMap()).setTdpId(1L);
        final DataSetRow row2 = row1.setTdpId(2L);
        CollectorNode output = new CollectorNode();
        CollectorNode output2 = new CollectorNode();
        final Node node = NodeBuilder.source(of(row1, row2)) //
                .dispatch(output, output2) //
                .zip(rows -> rows[0]) //
                .build();

        // when
        final List<DataSetRow> collected = Runtimes.execute(node);

        // then
        assertEquals(4, collected.size());
    }

    @Test
    public void testSourceNode() throws Exception {
        // Given
        final DataSetRow row1 = new DataSetRow(emptyMap()).setTdpId(1L);
        final DataSetRow row2 = row1.setTdpId(2L);
        final Node node = NodeBuilder.source(of(row1, row2)) //
                .to(new CollectorNode()) //
                .build();

        // when
        List<DataSetRow> collected = Runtimes.execute(node);

        // then
        assertEquals(2, collected.size());
        assertThat(collected.get(0).getTdpId(), anyOf(equalTo(1L), equalTo(2L)));
        assertThat(collected.get(1).getTdpId(), anyOf(equalTo(1L), equalTo(2L)));
    }

    @Test
    public void testFilteredSourceNode() throws Exception {
        // Given
        final DataSetRow row1 = new DataSetRow(emptyMap()).setTdpId(1L);
        final DataSetRow row2 = row1.setTdpId(2L);
        final Node node = NodeBuilder.filteredSource(new SourceFilterPredicate(), of(row1, row2)) //
                .to(new CollectorNode()) //
                .build();

        // when
        List<DataSetRow> collected = Runtimes.execute(node);

        // then
        assertEquals(1, collected.size());
        assertEquals(row2, collected.get(0));
    }

    @Test
    public void testVisitorAndToString() throws Exception {
        final Node node = NodeBuilder
                .source(empty()) //
                .to(new BasicNode()) //
                .dispatch(new BasicNode()) //
                .zip(rows -> rows[0]) //
                .to(new ActionNode(new RunnableAction(), Collections.emptyMap())) //
                .build();
        final Pipeline pipeline = new Pipeline(node);
        final NodeClassVisitor visitor = new NodeClassVisitor();

        // when
        pipeline.accept(visitor);

        // then
        final Class[] expectedClasses =
                { Pipeline.class, BasicLink.class, BasicNode.class, CloneLink.class, ActionNode.class };
        assertThat(visitor.getTraversedClasses(), CoreMatchers.hasItems(expectedClasses));
        assertNotNull(pipeline.toString());
    }

    private static class SourceFilterPredicate implements Predicate<DataSetRow>, Serializable {

        @Override
        public boolean test(DataSetRow row) {
            return row.getTdpId() == 2;
        }
    }
}
