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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.*;
import static org.talend.dataprep.transformation.pipeline.builder.NodeBuilder.source;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.actions.DataSetRowAction;
import org.talend.dataprep.transformation.actions.common.RunnableAction;
import org.talend.dataprep.transformation.actions.context.ActionContext;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.node.ActionNode;
import org.talend.dataprep.transformation.pipeline.node.CollectorNode;
import org.talend.dataprep.transformation.pipeline.node.CompileNode;

public class ExecutorTest implements Serializable {

    @Rule
    public Runtimes runtimes = new Runtimes();

    @Test
    public void shouldExecuteAction() throws Exception {
        // Given
        final DataSetRow dataSetRow = new DataSetRow(emptyMap());
        final ActionNode actionNode = new ActionNode(new RunnableAction((r, c) -> {
            // Beam tests can't use atomic boolean (apply makes a copy of function)
            System.setProperty("action.execute", "true");
            return Collections.singletonList(r);
        }), Collections.emptyMap());

        final Node model = source(Stream.of(dataSetRow)).to(actionNode).to(new CollectorNode()).build();

        // When
        Runtimes.execute(model);

        // Then
        assertFalse(Boolean.getBoolean("action.compile"));
        assertTrue(Boolean.getBoolean("action.execute"));
    }

    @Test
    public void shouldUpperCase() throws Exception {
        // Given
        final DataSetRow dataSetRow = new DataSetRow(singletonMap("0000", "Value"));
        final ActionNode actionNode =
                new ActionNode(new RunnableAction((r, c) -> Collections.singletonList(r.set("0000", r.get("0000").toUpperCase()))), Collections.emptyMap());

        final Node model = source(Stream.of(dataSetRow)) //
                .to(actionNode) //
                .to(new CollectorNode()) //
                .build();

        // When
        List<DataSetRow> collected = Runtimes.execute(model);

        // Then
        assertEquals("Value", dataSetRow.get("0000"));
        assertEquals("VALUE", collected.get(0).get("0000"));
    }

    @Test
    public void shouldExecuteCompile() throws Exception {
        // Given
        final DataSetRow dataSetRow = new DataSetRow(emptyMap());
        final RunnableAction action = new RunnableAction(new DataSetRowAction() {

            @Override
            public Collection<DataSetRow> apply(DataSetRow dataSetRow, ActionContext actionContext) {
                return Collections.singletonList(dataSetRow);
            }

            @Override
            public void compile(ActionContext actionContext) {
                // Beam tests can't use atomic boolean (apply makes a copy of function)
                System.setProperty("action.compile", "true");
            }
        });
        final CompileNode compileNode = new CompileNode(action, Collections.emptyMap());

        final Node model = source(Stream.of(dataSetRow)).to(compileNode).to(new CollectorNode()).build();

        // When
        Runtimes.execute(model);

        // Then
        assertTrue(Boolean.getBoolean("action.compile"));
        assertFalse(Boolean.getBoolean("action.execute"));
    }

    @After
    public void tearDown() throws Exception {
        System.setProperty("action.compile", "false");
        System.setProperty("action.execute", "false");
    }

}
