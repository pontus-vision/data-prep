package org.talend.dataprep.transformation.pipeline.node;

import static java.util.Collections.emptyMap;
import static junit.framework.TestCase.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;

public class LimitNodeTest {

    @Rule
    public Runtimes runtimes = new Runtimes();

    @Test
    public void shouldLimit() throws Exception {
        // given
        final DataSetRow row0 = new DataSetRow(emptyMap());
        final DataSetRow row1 = new DataSetRow(emptyMap());

        final LimitNode node = new LimitNode(1);
        final Node pipeline = NodeBuilder.source(Stream.of(row0, row1)).to(node).to(new CollectorNode()).build();

        // when
        List<DataSetRow> collected = Runtimes.execute(pipeline);

        // then
        assertEquals(1, collected.size());
    }

}