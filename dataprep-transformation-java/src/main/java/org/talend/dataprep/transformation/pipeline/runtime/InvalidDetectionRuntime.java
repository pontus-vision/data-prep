package org.talend.dataprep.transformation.pipeline.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.dataset.row.InvalidMarker;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.quality.AnalyzerService;
import org.talend.dataprep.transformation.actions.Providers;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.InvalidDetectionNode;
import org.talend.dataquality.common.inference.Analyzer;
import org.talend.dataquality.common.inference.Analyzers;

class InvalidDetectionRuntime implements RuntimeNode {

    private final InvalidDetectionNode invalidDetectionNode;

    private final RuntimeNode nextNode;

    private AnalyzerService analyzerService;

    private List<ColumnMetadata> filteredColumns;

    private RowMetadata rowMetadata;

    private Analyzer<Analyzers.Result> configuredAnalyzer;

    private InvalidMarker invalidMarker;

    InvalidDetectionRuntime(InvalidDetectionNode invalidDetectionNode, RuntimeNode nextNode) {
        this.invalidDetectionNode = invalidDetectionNode;
        this.nextNode = nextNode;
    }

    private void performColumnFilter(DataSetRow row, RowMetadata metadata) {
        final boolean needRefresh = !metadata.equals(rowMetadata);
        List<ColumnMetadata> columns = metadata.getColumns();
        if (!columns.isEmpty()) {
            if (filteredColumns == null || needRefresh) {
                filteredColumns =
                        columns.stream().filter(invalidDetectionNode.getFilter()).collect(Collectors.toList());
            }
        } else {
            // No column in row metadata, guess all type, starting from string columns.
            ColumnMetadata.Builder builder = ColumnMetadata.Builder.column().type(Type.STRING);
            final int rowSize = row.toArray(DataSetRow.SKIP_TDP_ID).length;
            columns = new ArrayList<>(rowSize + 1);
            for (int i = 0; i < rowSize; i++) {
                final ColumnMetadata newColumn = builder.build();
                metadata.addColumn(newColumn);
                columns.add(newColumn);
            }
            filteredColumns = columns;
        }
        rowMetadata = metadata;
    }

    private AnalyzerService getAnalyzerService() {
        if (analyzerService == null) {
            this.analyzerService = Providers.get(AnalyzerService.class);
        }
        return analyzerService;
    }

    @Override
    public void receive(DataSetRow row) {
        performColumnFilter(row, row.getRowMetadata());
        if (configuredAnalyzer == null) {
            this.configuredAnalyzer = getAnalyzerService().build(filteredColumns, AnalyzerService.Analysis.QUALITY);
            this.invalidMarker = new InvalidMarker(filteredColumns, configuredAnalyzer);
        }
        nextNode.receive(invalidMarker.apply(row));
    }

    @Override
    public void signal(Signal signal) {
        if (nextNode == null) {
            return;
        }
        nextNode.signal(signal);
    }

    @Override
    public RuntimeNode getNext() {
        return nextNode;
    }
}
