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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;

public class ConsumerNodeTest {

    private static final boolean[] modifications = new boolean[3];

    @Rule
    public Runtimes runtimes = new Runtimes();

    @After
    public void tearDown() throws Exception {
        System.setProperty("row.modified", "false");
        System.setProperty("signal.modified", "false");
    }

    @Test
    public void receive_should_perform_consumers_and_emit_row() {
        // given
        final DataSetRow row = new DataSetRow(emptyMap());
        final ConsumerNode node = new ConsumerNode( //
                new SerializableConsumer<>("row.modified"), //
                new SerializableConsumer<>("no checked in common tests")
        );
        final Node pipeline = NodeBuilder.source(Stream.of(row)).to(node).to(new CollectorNode()).build();


        // when
        List<DataSetRow> collected = runtimes.execute(pipeline);

        // then
        assertEquals(1, collected.size());
        assertTrue(Boolean.getBoolean("row.modified"));
    }

    private static class SerializableConsumer<T> implements Consumer<T>, Serializable {

        private String property;

        private SerializableConsumer(String property) {
            this.property = property;
        }

        @Override
        public void accept(Object o) {
            System.setProperty(property, "true");
        }
    }
}
