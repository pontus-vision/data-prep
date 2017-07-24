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
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;

public class NToOneNodeTest {

    @Rule
    public Runtimes runtimes = new Runtimes();

    @Test
    public void receive_should_emit_function_result() {
        // given
        final DataSetRow row1 = new DataSetRow(emptyMap()).setTdpId(0L);
        final DataSetRow row2 = new DataSetRow(emptyMap()).setTdpId(1L);

        Node pipeline = NodeBuilder
                .source(Stream.of(row1, row2)) //
                .dispatch( //
                        NodeBuilder.source().to(new BasicNode()).build(), //
                        NodeBuilder.source().to(new BasicNode()).build() //
                ) //
                .zip(new TestZipper()) //
                .to(new CollectorNode()) //
                .build();

        // when
        List<DataSetRow> collected = Runtimes.execute(pipeline);

        // then
        assertEquals(2, collected.size());
        assertEquals(row1.getTdpId(), collected.get(0).getTdpId());
        assertTrue(collected.get(0).isDeleted());
        assertEquals(row2.getTdpId(), collected.get(1).getTdpId());
    }

    private static class TestZipper implements Function<DataSetRow[], DataSetRow>, Serializable {

        @Override
        public DataSetRow apply(DataSetRow[] rows) {
            if (rows.length == 2) {
                if (rows[0].getTdpId() == 1) {
                    return rows[0];
                } else {
                    return rows[0].setDeleted(true);
                }
            }
            throw new IllegalStateException(); // Unexpected (there should be row1 or row2 somewhere).
        }
    }
}
