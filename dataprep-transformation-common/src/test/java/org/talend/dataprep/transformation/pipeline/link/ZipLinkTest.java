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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;
import org.talend.dataprep.transformation.pipeline.node.BasicNode;
import org.talend.dataprep.transformation.pipeline.node.CollectorNode;

public class ZipLinkTest {

    @Rule
    public Runtimes runtimes = new Runtimes();

    @Test
    public void should_emit_single_input_row_when_all_source_has_emitted_one() {
        // given
        final BasicNode source1 = new BasicNode();
        final BasicNode source2 = new BasicNode();

        final DataSetRow row = new DataSetRow(Collections.emptyMap()).setTdpId(0L);
        Node node = NodeBuilder.source(Stream.of(row)).dispatch(source1, source2).zip(rows -> rows[0]).to(new CollectorNode()).build();

        // when
        List<DataSetRow> collected = Runtimes.execute(node);

        // then
        assertEquals(1, collected.size());
        assertEquals(0L, collected.get(0).getTdpId().longValue());
    }

}
