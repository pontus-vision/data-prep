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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.io.Serializable;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;

public class FilterNodeTest implements Serializable {

    @Rule
    public Runtimes runtimes = new Runtimes();

    @Test
    public void receive_should_filter_with_simple_predicate() throws Exception {
        // given
        final DataSetRow row0 = new DataSetRow(emptyMap()).setTdpId(0L); // does not pass the predicate
        final DataSetRow row1 = new DataSetRow(emptyMap()).setTdpId(1L); // pass the predicate
        final FilterNode node = new FilterNode(new FilterTestPredicate());
        final Node pipeline = NodeBuilder.source(Stream.of(row0, row1)).to(node).to(new CollectorNode()).build();

        // when
        List<DataSetRow> rows = runtimes.execute(pipeline);

        // then
        assertThat(rows, hasSize(1));
    }

    @Test
    public void receive_multi_should_filter_with_multi_predicate() throws Exception {
        // given
        final DataSetRow row0 = new DataSetRow(emptyMap()).setTdpId(0L); // does not pass the predicate
        final DataSetRow row1 = new DataSetRow(emptyMap()).setTdpId(1L); // pass the predicate
        final DataSetRow row2 = new DataSetRow(emptyMap()).setTdpId(2L); // does not pass the predicate
        final DataSetRow row3 = new DataSetRow(emptyMap()).setTdpId(3L); // pass the predicate

        final FilterNode node = new FilterNode(new AcceptAllTestPredicate(), new FilterTestPredicate());
        final Node pipeline = NodeBuilder.source(Stream.of(row0, row1, row2, row3)).to(node).to(new CollectorNode()).build();

        // when
        List<DataSetRow> collected = Runtimes.execute(pipeline);

        // then
        assertThat(collected, hasSize(1));
    }

    private static class FilterTestPredicate implements Predicate<DataSetRow>, Serializable {

        @Override
        public boolean test(DataSetRow row) {
            return row.getTdpId() == 1;
        }
    }

    private static class AcceptAllTestPredicate implements Predicate<DataSetRow>, Serializable {

        @Override
        public boolean test(DataSetRow row) {
            return true;
        }
    }
}
