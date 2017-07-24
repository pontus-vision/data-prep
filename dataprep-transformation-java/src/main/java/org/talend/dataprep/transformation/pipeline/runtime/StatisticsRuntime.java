package org.talend.dataprep.transformation.pipeline.runtime;

import java.util.List;
import java.util.stream.Collectors;

import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.dataset.StatisticsAdapter;
import org.talend.dataprep.transformation.pipeline.Signal;
import org.talend.dataprep.transformation.pipeline.node.StatisticsNode;
import org.talend.dataquality.common.inference.Analyzer;
import org.talend.dataquality.common.inference.Analyzers;

class StatisticsRuntime implements RuntimeNode {

    private final StatisticsNode statisticsNode;

    private final RuntimeNode nextNode;

    private Analyzer<Analyzers.Result> configuredAnalyzer;

    StatisticsRuntime(StatisticsNode statisticsNode, RuntimeNode nextNode) {
        this.statisticsNode = statisticsNode;
        this.nextNode = nextNode;
    }

    @Override
    public void receive(DataSetRow row) {
        if (!row.isDeleted()) {
            final RowMetadata rowMetadata = row.getRowMetadata();
            final List<ColumnMetadata> columns = rowMetadata.getColumns();
            final List<ColumnMetadata> filteredColumns = columns //
                    .stream() //
                    .filter(statisticsNode.getFilter()) //
                    .collect(Collectors.toList());
            if (configuredAnalyzer == null) {
                this.configuredAnalyzer = statisticsNode.getAnalyzer().apply(filteredColumns);
            }
            configuredAnalyzer.analyze(row //
                    .filter(filteredColumns) //
                    .order(filteredColumns) //
                    .toArray(DataSetRow.SKIP_TDP_ID) //
            );
            final StatisticsAdapter adapter = statisticsNode.getAdapter();
            adapter.adapt(filteredColumns, configuredAnalyzer.getResult());
            nextNode.receive(row.setRowMetadata(rowMetadata));
        } else {
            nextNode.receive(row);
        }
    }

    @Override
    public void signal(Signal signal) {
        if (nextNode == null) {
            return;
        }
        nextNode.signal(signal);
    }
}
