package org.talend.dataprep.transformation.pipeline.node;

import static junit.framework.TestCase.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;

public class InvalidDetectionNodeTest {

    @Rule
    public Runtimes runtimes = new Runtimes();

    @Test
    public void shouldMarkInvalidValues() throws Exception {
        // given
        final RowMetadata metadata0 =
                new RowMetadata(Collections.singletonList(ColumnMetadata.Builder.column().type(Type.INTEGER).build()));
        final DataSetRow row0 = new DataSetRow(metadata0).set("0000", "aaaa"); // Not an integer
        assertFalse(row0.isInvalid("0000"));

        final InvalidDetectionNode invalidDetection = new InvalidDetectionNode(c -> true);
        Node pipeline = NodeBuilder.source(Stream.of(row0)).to(invalidDetection).to(new CollectorNode()).build();

        // when
        List<DataSetRow> collected = Runtimes.execute(pipeline);

        // then
        assertEquals(1, collected.size());
        assertTrue(collected.get(0).isInvalid("0000"));
    }

}