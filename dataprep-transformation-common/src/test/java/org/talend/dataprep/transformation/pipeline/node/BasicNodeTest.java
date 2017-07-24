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

import java.util.List;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;

public class BasicNodeTest {

    @Rule
    public Runtimes runtimes = new Runtimes();

    @Test
    public void should_emit_single_input_row_to_its_link() {
        // given
        final DataSetRow row = new DataSetRow(emptyMap());
        final Node pipeline = NodeBuilder.source(Stream.of(row)).to(new CollectorNode()).build();

        // when
        List<DataSetRow> collected = Runtimes.execute(pipeline);

        // then
        assertEquals(1, collected.size());
    }

    @Test
    public void should_emit_multi_input_row_to_its_link() {
        // given
        final DataSetRow row1 = new DataSetRow(emptyMap());
        final DataSetRow row2 = new DataSetRow(emptyMap());
        final Node pipeline = NodeBuilder.source(Stream.of(row1, row2)).to(new CollectorNode()).build();

        // when
        List<DataSetRow> collected = Runtimes.execute(pipeline);

        // then
        assertEquals(2, collected.size());
    }
}
