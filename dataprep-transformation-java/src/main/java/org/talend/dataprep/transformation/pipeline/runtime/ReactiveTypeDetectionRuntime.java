// ============================================================================
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.TypeDetectionNode;
import org.talend.dataquality.common.inference.Analyzer;
import org.talend.dataquality.common.inference.Analyzers;

import reactor.core.publisher.BlockingSink;
import reactor.core.publisher.ReplayProcessor;

public class ReactiveTypeDetectionRuntime implements RuntimeNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveTypeDetectionRuntime.class);

    private final RuntimeNode nextNode;

    private final ReplayProcessor<DataSetRow> processor;

    private final BlockingSink<DataSetRow> sink;

    private final TypeDetectionNode typeDetectionNode;

    private RowMetadata metadata;

    private List<ColumnMetadata> filteredColumns;

    private Analyzer<Analyzers.Result> resultAnalyzer;

    ReactiveTypeDetectionRuntime(TypeDetectionNode typeDetectionNode, RuntimeNode nextNode) {
        this.typeDetectionNode = typeDetectionNode;

        this.processor = ReplayProcessor.create(10000, true);
        this.sink = processor.connectSink();
        processor.subscribe(row -> {
            LOGGER.trace("analyze row: " + row);
            performColumnFilter(row);
            analyze(row);
        });

        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        sink.submit(row);
    }

    private void analyze(DataSetRow row) {
        if (!row.isDeleted()) {
            // Lazy initialization of the result analyzer
            if (resultAnalyzer == null) {
                resultAnalyzer = typeDetectionNode.getAnalyzer().apply(filteredColumns);
            }
            final String[] values = row
                    .filter(filteredColumns) //
                    .order(metadata.getColumns()) //
                    .toArray(DataSetRow.SKIP_TDP_ID);
            try {
                resultAnalyzer.analyze(values);
            } catch (Exception e) {
                LOGGER.debug("Unable to analyze row '{}'.", Arrays.toString(values), e);
            }
        }
    }

    private void performColumnFilter(DataSetRow row) {
        final RowMetadata rowMetadata = row.getRowMetadata();
        if (metadata == null || !Objects.equals(metadata, rowMetadata)) {
            List<ColumnMetadata> columns = rowMetadata.getColumns();
            if (filteredColumns == null) {
                filteredColumns = columns.stream().filter(typeDetectionNode.getFilter()).collect(Collectors.toList());
            }
            metadata = rowMetadata;
        }
    }

    @Override
    public void signal(Signal signal) {
        sink.finish();
        if (nextNode != null) {
            processor.subscribe(row -> {
                LOGGER.trace("forward row = " + row);
                nextNode.receive(row.setRowMetadata(metadata));
            });
            nextNode.signal(signal);
        }
    }
}
