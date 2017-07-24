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

package org.talend.dataprep.transformation.pipeline.link;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;
import org.talend.dataprep.transformation.pipeline.node.CollectorNode;
import org.talend.dataprep.transformation.pipeline.node.NoOpNode;

public class CloneLinkTest {

    @Rule
    public Runtimes runtimes = new Runtimes();

    @Test
    public void should_emit_single_input_row_to_all_targets() {
        // given
        final CollectorNode target1 = new CollectorNode();
        final CollectorNode target2 = new CollectorNode();

        final DataSetRow row = new DataSetRow(emptyMap()).setTdpId(0L);
        final Node pipeline = NodeBuilder.source(Stream.of(row)).dispatch(target1, target2).zip(rows -> rows[0]).to(new NoOpNode()).build();

        // when
        final List<DataSetRow> collected = Runtimes.execute(pipeline);

        // then
        assertEquals(2, collected.size());
        for (DataSetRow dataSetRow : collected) {
            assertTrue(dataSetRow != row);
            assertEquals(0L, dataSetRow.getTdpId().longValue());
        }
    }

    @Test
    public void should_emit_multi_input_row_to_all_targets() {
        // given
        final CollectorNode target1 = new CollectorNode();
        final CollectorNode target2 = new CollectorNode();
        final DataSetRow row1 = new DataSetRow(emptyMap()).setTdpId(0L);
        final DataSetRow row2 = new DataSetRow(emptyMap()).setTdpId(1L);

        final Node pipeline = NodeBuilder.source(Stream.of(row1, row2)).dispatch(target1, target2).zip(rows -> rows[0]).to(new NoOpNode()).build();

        // when
        List<DataSetRow> collected = Runtimes.execute(pipeline);

        // then
        assertEquals(4, collected.size());
        for (DataSetRow dataSetRow : collected) {
            assertTrue(dataSetRow != row1 && dataSetRow != row2);
            assertTrue(dataSetRow.getTdpId() == 0 || dataSetRow.getTdpId() == 1);
        }
    }

}
